/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.util.Bits;

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.IGeoBlockOfHitsProvider;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.DocsSortedByDocId;
import com.browseengine.bobo.geosearch.bo.GeRecordAndCartesianDocId;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;

/**
 * @author Ken McCracken
 *
 */
public class GeoBlockOfHitsProvider implements IGeoBlockOfHitsProvider {
    
    private final IGeoConverter geoConverter;
    
    public GeoBlockOfHitsProvider(IGeoConverter geoConverter) {
        this.geoConverter = geoConverter;
    }

    /**
     * {@inheritDoc}
     * @throws IOException 
     *
     */
    @Override
    public DocsSortedByDocId getBlock(GeoSegmentReader<CartesianGeoRecord> geoSegmentReader, 
            int segmentStartDocId, Bits acceptDocs,
            int minX, int maxX, int minY, int maxY, int minZ, int maxZ, int mindocid, int maxdocid)
            throws IOException {

        CartesianCoordinateDocId minccd = new CartesianCoordinateDocId(minX, minY, minZ, mindocid);
        CartesianGeoRecord minValue = geoConverter.toCartesianGeoRecord(minccd, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
        CartesianCoordinateDocId maxccd = new CartesianCoordinateDocId(maxX, maxY, maxZ, maxdocid);
        CartesianGeoRecord maxValue = geoConverter.toCartesianGeoRecord(maxccd, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
        Iterator<CartesianGeoRecord> iterator = geoSegmentReader.getIterator(minValue, maxValue);
        DocsSortedByDocId docs = new DocsSortedByDocId();

        while (iterator.hasNext()) {
            CartesianGeoRecord geoRecord = iterator.next();
            CartesianCoordinateDocId ccd  = geoConverter.toCartesianCoordinateDocId(geoRecord);
            if(minX <= ccd.x  && ccd.x <= maxX && minY <= ccd.y  && ccd.y <= maxY && minZ <= ccd.z  
                    && ccd.z <= maxZ && mindocid <= ccd.docid  && ccd.docid <= maxdocid
                    && acceptDocs.get(segmentStartDocId + ccd.docid)) {
                GeRecordAndCartesianDocId both = new GeRecordAndCartesianDocId(geoRecord, ccd);
                docs.add(ccd.docid, both);
            }
        }
        
        return docs;
    }

    
}
