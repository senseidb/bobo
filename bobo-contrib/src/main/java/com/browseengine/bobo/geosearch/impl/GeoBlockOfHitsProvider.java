/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import java.io.IOException;
import java.util.Iterator;

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.IDeletedDocs;
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
    @Override
    public DocsSortedByDocId getBlock(GeoSegmentReader geoSegmentReader, IDeletedDocs deletedDocsWithinSegment,
            final double minimumLongitude, final double minimumLatitude, final int minimumDocid, 
            final double maximumLongitude, final double maximumLatitude, final int maximumDocid) throws IOException {
        final byte filterByte = GeoRecord.DEFAULT_FILTER_BYTE;
        
        LatitudeLongitudeDocId minRaw = new LatitudeLongitudeDocId(minimumLatitude, minimumLongitude, minimumDocid);
        GeoRecord minValue = geoConverter.toGeoRecord(filterByte, minRaw);
        LatitudeLongitudeDocId maxRaw = new LatitudeLongitudeDocId(maximumLatitude, maximumLongitude, maximumDocid);
        GeoRecord maxValue = geoConverter.toGeoRecord(filterByte, maxRaw);
        Iterator<GeoRecord> iterator = geoSegmentReader.getIterator(minValue, maxValue);
        DocsSortedByDocId docs = new DocsSortedByDocId();
        while (iterator.hasNext()) {
            GeoRecord geoRecord = iterator.next();
            LatitudeLongitudeDocId longitudeLatitudeDocId = geoConverter.toLongitudeLatitudeDocId(geoRecord);
            if (minimumLongitude <= longitudeLatitudeDocId.longitude && longitudeLatitudeDocId.longitude <= maximumLongitude 
                    && minimumLatitude <= longitudeLatitudeDocId.latitude && longitudeLatitudeDocId.latitude <= maximumLatitude 
                    && minimumDocid <= longitudeLatitudeDocId.docid && longitudeLatitudeDocId.docid <= maximumDocid
                    ) {
                // (AT LEAST ALMOST A) HIT!
                GeoRecordAndLongitudeLatitudeDocId both = new GeoRecordAndLongitudeLatitudeDocId(geoRecord, longitudeLatitudeDocId);
                docs.add(longitudeLatitudeDocId.docid, both);
            }
        }
        return docs;
    }
*/
    @Override
    public DocsSortedByDocId getBlock(GeoSegmentReader geoSegmentReader, IDeletedDocs deletedDocsWithinSegment,
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
            if(minX <= ccd.x  && ccd.x <= maxX && minY <= ccd.y  && ccd.y <= maxY && minZ <= ccd.z  && ccd.z <= maxZ && mindocid <= ccd.docid  && ccd.docid <= maxdocid) {
                GeRecordAndCartesianDocId both = new GeRecordAndCartesianDocId(geoRecord, ccd);
                docs.add(ccd.docid, both);
            }
        }
        
        return docs;
    }

    
}
