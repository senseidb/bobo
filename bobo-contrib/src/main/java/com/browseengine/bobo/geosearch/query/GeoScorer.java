/**
 * 
 */
package com.browseengine.bobo.geosearch.query;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;

import com.browseengine.bobo.geosearch.IDeletedDocs;
import com.browseengine.bobo.geosearch.IGeoBlockOfHitsProvider;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.DocsSortedByDocId;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoRecordAndLongitudeLatitudeDocId;
import com.browseengine.bobo.geosearch.impl.DeletedDocs;
import com.browseengine.bobo.geosearch.impl.GeoBlockOfHitsProvider;
import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;
import com.browseengine.bobo.geosearch.score.IComputeDistance;
import com.browseengine.bobo.geosearch.score.impl.HaversineComputeDistance;

/**
 * @author Ken McCracken
 *
 */
public class GeoScorer extends Scorer {
    /**
     * Number of Documents we look at at a time.
     */
    private static final int BLOCK_SIZE = 16384;
    
    private static final int DOCID_CURSOR_NONE_YET = -1;

    private final IGeoConverter geoConverter;
    private final IGeoBlockOfHitsProvider geoBlockOfHitsProvider;
    private final IComputeDistance computeDistance;
    private final List<GeoSegmentReader<GeoRecord>> segmentsInOrder;
    private final double centroidLongitudeDegrees;
    private final double centroidLatitudeDegrees;
    private final float rangeInMiles;
    
    // TODO: add dynamic refinement as the query goes, 
    // to collapse the range
    private double minimumLongitudeDegrees;
    private double maximumLongitudeDegrees;
    private double minimumLatitudeDegrees;
    private double maximumLatitudeDegrees;
    // current pointers
    private int docid = DOCID_CURSOR_NONE_YET; 
    private int indexOfCurrentPartition = DOCID_CURSOR_NONE_YET;
    private int startDocidOfCurrentPartition;
    private GeoSegmentReader<GeoRecord> currentSegment = null;
    private DocsSortedByDocId currentBlockScoredDocs;
    
    private Entry<Integer, Collection<GeoRecordAndLongitudeLatitudeDocId>> currentDoc;
    
    private IDeletedDocs wholeIndexDeletedDocs;
    
    
    public static void main(String args[]) {
        System.out.println("NO_MORE_DOCS equals to = " + NO_MORE_DOCS);
    }
    
    public GeoScorer(Weight                      weight,
                     List<GeoSegmentReader<GeoRecord>>      segmentsInOrder, 
                     IDeletedDocs                wholeIndexDeletedDocs, 
                     double                      centroidLongitudeDegrees,
                     double                      centroidLatitudeDegrees,
                     float                       rangeInMiles) {
                     super                       (weight);
        
        this.geoConverter = new GeoConverter();
        this.geoBlockOfHitsProvider = new GeoBlockOfHitsProvider(geoConverter);
        
        this.segmentsInOrder = segmentsInOrder;
        
        this.centroidLongitudeDegrees = centroidLongitudeDegrees;
        this.centroidLatitudeDegrees = centroidLatitudeDegrees;
        this.rangeInMiles = rangeInMiles;
        
        startDocidOfCurrentPartition = -1;
        
        this.computeDistance = new HaversineComputeDistance();
        
        init();
    }
    
    private void init() {
        double deltaLongitudeDegrees = computeDistance.computeLonBoundary(centroidLongitudeDegrees, 
                centroidLatitudeDegrees, rangeInMiles);
        
        double deltaLatitudeDegrees = computeDistance.computeLatBoundary(centroidLongitudeDegrees, 
                centroidLatitudeDegrees, rangeInMiles);

        minimumLongitudeDegrees = centroidLongitudeDegrees - deltaLongitudeDegrees;
        maximumLongitudeDegrees = centroidLongitudeDegrees + deltaLongitudeDegrees;
        
        minimumLatitudeDegrees = centroidLatitudeDegrees - deltaLatitudeDegrees;
        maximumLatitudeDegrees = centroidLatitudeDegrees + deltaLatitudeDegrees;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float score() throws IOException {
        assert docid >= 0 && docid != NO_MORE_DOCS;

        return score(currentDoc.getValue());
    }
    
    /**
     * about 5 feet.
     */
    private static final float MINIMUM_DISTANCE_WE_CARE_ABOUT_MILES = 0.0001f;
    
    private float score(Collection<GeoRecordAndLongitudeLatitudeDocId> values) {
        float minimumDistanceMiles = 9999999f;
        for (GeoRecordAndLongitudeLatitudeDocId value : values) {
            float distanceMiles = computeDistance.getDistanceInMiles(centroidLongitudeDegrees, centroidLatitudeDegrees, 
                    value.longitudeLatitudeDocId.longitude, value.longitudeLatitudeDocId.latitude);
            if (distanceMiles < minimumDistanceMiles) {
                minimumDistanceMiles = distanceMiles;
            }
        }
        return score(minimumDistanceMiles);
    }
    
    /**
     * Score is 1/distance normalized to 1 at MINIMUM_DISTANCE_WE_CARE_ABOUT_MILES.
     * 
     * @param minimumDistanceMiles
     * @return
     */
    private float score(float minimumDistanceMiles) {
        if (minimumDistanceMiles < MINIMUM_DISTANCE_WE_CARE_ABOUT_MILES) {
            return 1f;
        }
        return MINIMUM_DISTANCE_WE_CARE_ABOUT_MILES/minimumDistanceMiles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int advance(int target) throws IOException {
        assert (NO_MORE_DOCS == docid || DOCID_CURSOR_NONE_YET == docid || target >= docid);
        
        fillBlockContainingAndSeekTo(target);

        return docid;
    }
    
    private boolean doesCurrentTreeContain(int seekDocid) {
        if (currentSegment == null) {
            return false;
        }
        int maxDocAbsoluteCurrentPartition = startDocidOfCurrentPartition 
            + currentSegment.getMaxDoc();
        return seekDocid < maxDocAbsoluteCurrentPartition;
    }
    
    /**
     * 
     * @param seekDocid
     */
    private void seekToTree(int seekDocid) {
        while (!doesCurrentTreeContain(seekDocid)) {
            if (indexOfCurrentPartition == NO_MORE_DOCS) {
                return;
            } else if (indexOfCurrentPartition == DOCID_CURSOR_NONE_YET && segmentsInOrder.size() > 0) {
                indexOfCurrentPartition++;
                startDocidOfCurrentPartition = 0;
            } else {
                indexOfCurrentPartition++;
                if (indexOfCurrentPartition < segmentsInOrder.size()) {
                    startDocidOfCurrentPartition += segmentsInOrder.get(indexOfCurrentPartition-1).getMaxDoc();
                } else {
                    // we are past the end
                    indexOfCurrentPartition = NO_MORE_DOCS;
                    startDocidOfCurrentPartition = NO_MORE_DOCS;
                    docid = NO_MORE_DOCS;
                    return;
                }
            }
            currentSegment = segmentsInOrder.get(indexOfCurrentPartition);
        }

    }
    
    private boolean isBlockInMemoryAlreadyAndSeekWithinBlock(int seekDocid) {
        if (DOCID_CURSOR_NONE_YET == currentBlockGlobalMaxDoc) {
            return false;
        }
        if (seekDocid < currentBlockGlobalMaxDoc) {
            // it's possible its in the current block
            while (currentBlockScoredDocs.size() > 0 && docid < seekDocid && seekDocid < currentBlockGlobalMaxDoc) {
                nextDocidAndCurrentDocFromBlockInMemory();
            }
            if (seekDocid <= docid) {
                return true;
            }
        }

        return false;
    }
    
    private void nextDocidAndCurrentDocFromBlockInMemory() {
        Entry<Integer, Collection<GeoRecordAndLongitudeLatitudeDocId>> doc = currentBlockScoredDocs.pollFirst();
        docid = doc.getKey() + startDocidOfCurrentPartition;
        // docid is now translated into the whole-index docid value
        currentDoc = doc;
    }
    
    private int currentBlockGlobalMaxDoc = DOCID_CURSOR_NONE_YET;
    
    private void fillBlockContainingAndSeekTo(int seekDocid) throws IOException {
        if (isBlockInMemoryAlreadyAndSeekWithinBlock(seekDocid)) {
            return;
        }
        
        if (DOCID_CURSOR_NONE_YET != currentBlockGlobalMaxDoc 
                && NO_MORE_DOCS != currentBlockGlobalMaxDoc) {
            // it was not found in the current block, 
            // so we should seek past the current block if not already doing so.
            seekDocid = Math.max(currentBlockGlobalMaxDoc, seekDocid);
        }
        seekToTree(seekDocid);
        
        if (NO_MORE_DOCS == docid) {
            return;
        }
        
        pullBlockInMemory(seekDocid);
        
        if (currentBlockScoredDocs.size() == 0) {
            fillBlockContainingAndSeekTo(currentBlockGlobalMaxDoc);
        } else if (NO_MORE_DOCS == docid) {
            return;
        } else {
            nextDocidAndCurrentDocFromBlockInMemory();
        }
    }
    
    private void pullBlockInMemory(int seekDocid) throws IOException {
        int offsetDocidWithinPartition = seekDocid - startDocidOfCurrentPartition;
        
        IDeletedDocs deletedDocsWithinSegment = new DeletedDocs(wholeIndexDeletedDocs, 
                startDocidOfCurrentPartition);
        int blockNumber = offsetDocidWithinPartition / BLOCK_SIZE;
        int minimumDocidInPartition = offsetDocidWithinPartition - blockNumber * BLOCK_SIZE;
        int maxDocInPartition = currentSegment.getMaxDoc();
        int maximumDocidInPartition = Math.min(maxDocInPartition, 
                (blockNumber + 1) * BLOCK_SIZE);
        
        currentBlockScoredDocs = geoBlockOfHitsProvider.getBlock(currentSegment, deletedDocsWithinSegment,
                minimumLongitudeDegrees, minimumLatitudeDegrees, minimumDocidInPartition, 
                maximumLongitudeDegrees, maximumLatitudeDegrees, maximumDocidInPartition);
        currentBlockGlobalMaxDoc = startDocidOfCurrentPartition + maximumDocidInPartition;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int docID() {
        assert docid >= 0;

        return docid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int nextDoc() throws IOException {
        if (docid == NO_MORE_DOCS) {
            return NO_MORE_DOCS;
        }
        
        fillBlockContainingAndSeekTo(docid + 1);
        return docid;
    }
}
