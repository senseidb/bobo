/**
 * 
 */
package com.browseengine.bobo.geosearch;

import java.io.IOException;

import com.browseengine.bobo.geosearch.bo.DocsSortedByDocId;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;

/**
 * @author Ken McCracken
 *
 */
public interface IGeoBlockOfHitsProvider {

    /**
     * ALL VALUES RETURNED HAVE RAW DOCID that is the docid WITHIN the 
     * current segment.
     * 
     * <p>
     * Gets a block of results within the specified boundaries.
     * The returned object should contain access to docids and scores 
     * where docids are relative within a partition (docid if that 
     * partition were the only one), and scores should be something that 
     * smooths out the 1/distance or 1/distance^2 curve.
     * 
     * @param tree
     * @param minimumLongitude
     * @param minimumLatitude
     * @param minimumDocid
     * @param maximumLongitude
     * @param maximumLatitude
     * @param maximumDocid
     * @return
     */
    DocsSortedByDocId getBlock(GeoSegmentReader geoSegmentReader, IDeletedDocs deletedDocsWithinSegment,
            double minimumLongitude, double minimumLatitude, int minimumDocid, 
            double maximumLongitude, double maximumLatitude, int maximumDocid) throws IOException;
}
