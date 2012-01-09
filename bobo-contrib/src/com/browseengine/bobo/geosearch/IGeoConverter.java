/**
 * 
 */
package com.browseengine.bobo.geosearch;

import com.browseengine.bobo.geosearch.bo.CartesianCoordinateUUID;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;

/**
 * @author Ken McCracken
 *
 */
    public interface IGeoConverter {
        LatitudeLongitudeDocId toLongitudeLatitudeDocId(GeoRecord geoRecord);
        GeoRecord toGeoRecord(IFieldNameFilterConverter fieldNameFilterConverter, 
                String fieldName, LatitudeLongitudeDocId longitudeLatitudeDocId);
        GeoRecord toGeoRecord(byte filterByte, LatitudeLongitudeDocId longitudeLatitudeDocId);
        IFieldNameFilterConverter makeFieldNameFilterConverter();
        void addFieldBitMask(String fieldName, byte bitMask);
        
        IDGeoRecord toIDGeoRecord(double latitude, double longitude, byte[] uuid);
        IDGeoRecord toIDGeoRecord(CartesianCoordinateUUID coordinate);
        CartesianCoordinateUUID toCartesianCoordinate(IDGeoRecord geoRecord);
        CartesianCoordinateUUID toCartesianCoordinate(double latitude, double longitude, byte[] uuid);
        
    }
