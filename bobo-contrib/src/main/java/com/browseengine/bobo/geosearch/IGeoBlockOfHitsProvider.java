/**
 * 
 */
package com.browseengine.bobo.geosearch;

import java.io.IOException;

import org.apache.lucene.util.Bits;

import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.DocsSortedByDocId;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;

/**
 * @author Ken McCracken
 *
 */
public interface IGeoBlockOfHitsProvider {
    
    /**
     * * ALL VALUES RETURNED HAVE RAW DOCID that is the docid WITHIN the 
     * current segment.
     * 
     * <p>
     * Gets a block of results within the specified boundaries.
     * The returned object should contain access to docids and scores 
     * where docids are relative within a partition (docid if that 
     * partition were the only one), and scores should be something that 
     * smooths out the 1/distance or 1/distance^2 curve.
     * 
     * @param geoSegmentReader
     * @param deletedDocsWithinSegment
     * @param minX
     * @param minY
     * @param minZ
     * @param minimumDocid
     * @param maxX
     * @param maxY
     * @param maxZ
     * @param maximumDocid
     * @return
     * @throws IOException
     */
    DocsSortedByDocId getBlock(GeoSegmentReader<CartesianGeoRecord> geoSegmentReader, 
            int segmentStartDocId, Bits acceptDocs,
            int minX, int minY, int minZ, int minimumDocid, 
            int maxX, int maxY, int maxZ, int maximumDocid) throws IOException;
}
