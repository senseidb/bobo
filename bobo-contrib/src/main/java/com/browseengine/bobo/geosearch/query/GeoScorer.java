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

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.IDeletedDocs;
import com.browseengine.bobo.geosearch.IGeoBlockOfHitsProvider;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.DocsSortedByDocId;
import com.browseengine.bobo.geosearch.bo.GeRecordAndCartesianDocId;
import com.browseengine.bobo.geosearch.impl.DeletedDocs;
import com.browseengine.bobo.geosearch.impl.GeoBlockOfHitsProvider;
import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;
import com.browseengine.bobo.geosearch.score.IComputeDistance;
import com.browseengine.bobo.geosearch.score.impl.HaversineComputeDistance;
import com.browseengine.bobo.geosearch.solo.search.impl.GeoOnlySearcher;

/**
 * @author Ken McCracken
 * @author shandets
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
    private final List<GeoSegmentReader<CartesianGeoRecord>> segmentsInOrder;
    private final int centroidX;
    private final int centroidY;
    private final int centroidZ;
    private final float rangeInKm;
    private int[] cartesianBoundingBox;
    
    // current pointers
    private int docid = DOCID_CURSOR_NONE_YET; 
    private int indexOfCurrentPartition = DOCID_CURSOR_NONE_YET;
    private int startDocidOfCurrentPartition;
    private GeoSegmentReader<CartesianGeoRecord> currentSegment = null;
    private DocsSortedByDocId currentBlockScoredDocs;
    
    private Entry<Integer, Collection<GeRecordAndCartesianDocId>> currentDoc;
    
    private IDeletedDocs wholeIndexDeletedDocs;
    
    
    public static void main(String args[]) {
        System.out.println("NO_MORE_DOCS equals to = " + NO_MORE_DOCS);
    }
    
    public GeoScorer(Weight                      weight,
                     List<GeoSegmentReader<CartesianGeoRecord>>      segmentsInOrder, 
                     IDeletedDocs                wholeIndexDeletedDocs, 
                     int                         centroidX,
                     int                         centroidY,
                     int                         centroidZ,
                     float                       rangeInKm) {
                     super                       (weight);
        
        this.geoConverter = new GeoConverter();
        this.geoBlockOfHitsProvider = new GeoBlockOfHitsProvider(geoConverter);
        
        this.segmentsInOrder = segmentsInOrder;
        
        this.centroidX = centroidX;
        this.centroidY = centroidY;
        this.centroidZ = centroidZ;
        this.rangeInKm = rangeInKm;
        
        startDocidOfCurrentPartition = -1;
        
        this.computeDistance = new HaversineComputeDistance();
        CartesianCoordinateDocId minccd = GeoOnlySearcher.buildMinCoordinate(rangeInKm, centroidX, centroidY, centroidZ, 0);
        CartesianCoordinateDocId maxccd = GeoOnlySearcher.buildMaxCoordinate(rangeInKm, centroidX, centroidY, centroidZ, 0);
        this.cartesianBoundingBox = new int [] {minccd.x, maxccd.x, minccd.y, maxccd.y, minccd.z, maxccd.z};
        
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
     * MINIMUM_DISTANCE_WE_CARE_ABOUT = (x-x')*(x-x') + (y-y')*(y-y') + (z-z')*(z-z')
     *  Where x, y, z and x', y', z' are 2 meter apart.
     * 
     */
    private static final long MINIMUM_DISTANCE_WE_CARE_ABOUT = 273798;
    
    private float score(Collection<CartesianCoordinateDocId> values) {
        long squaredDistance = Long.MAX_VALUE;
        for (CartesianCoordinateDocId value : values) {
             long squaredDistance2 = computeDistance.getSquaredDistance(centroidX, centroidY, centroidZ, value.x, value.y, value.z);
             if(squaredDistance2 < squaredDistance) {
                 squaredDistance = squaredDistance2;
             }
        }
        return score(squaredDistance);
    }
    
    /**
     * Score is 1/distance normalized to 1 at MINIMUM_DISTANCE_WE_CARE_ABOUT.
     * 
     * @param minimumDistanceMiles
     * @return
     */
    private float score(long squaredDistance) {
        if (squaredDistance < MINIMUM_DISTANCE_WE_CARE_ABOUT) {
            return 1f;
        }
        return (float)((double)MINIMUM_DISTANCE_WE_CARE_ABOUT/((double)squaredDistance));
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
        Entry<Integer, Collection<GeRecordAndCartesianDocId>> doc = currentBlockScoredDocs.pollFirst();
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
                cartesianBoundingBox[0], cartesianBoundingBox[1], cartesianBoundingBox[2],
                cartesianBoundingBox[3], cartesianBoundingBox[4], cartesianBoundingBox[5],
                minimumDocidInPartition, maximumDocidInPartition);
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
