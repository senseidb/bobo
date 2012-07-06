/**
 * 
 */
package com.browseengine.bobo.geosearch;

import com.browseengine.bobo.geosearch.bo.CartesianCoordinateUUID;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
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
        CartesianGeoRecord toCartesianGeoRecord(IFieldNameFilterConverter fieldNameFilterConverter, 
                String fieldName, LatitudeLongitudeDocId longitudeLatitudeDocId);
        CartesianGeoRecord toCartesianGeoRecord(LatitudeLongitudeDocId latLongDocID, byte filterByte);
        CartesianCoordinateUUID toCartesianCoordinate(IDGeoRecord geoRecord);
        CartesianCoordinateUUID toCartesianCoordinate(double latitude, double longitude, byte[] uuid);
        CartesianGeoRecord toCartesianGeoRecord(CartesianCoordinateDocId coord);
        CartesianCoordinateDocId toCartesianCoordinateDocId(CartesianGeoRecord geoRecord);
    }
