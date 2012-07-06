/**
 * 
 */
package com.browseengine.bobo.geosearch;

import com.browseengine.bobo.geosearch.bo.CartesianCoordinateUUID;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;

/**
 * @author Ken McCracken
 *
 */
    public interface IGeoConverter {
        IFieldNameFilterConverter makeFieldNameFilterConverter();
        void addFieldBitMask(String fieldName, byte bitMask);
        
        IDGeoRecord toIDGeoRecord(double latitude, double longitude, byte[] uuid);
        IDGeoRecord toIDGeoRecord(CartesianCoordinateUUID coordinate);
        CartesianGeoRecord toCartesianGeoRecord(CartesianCoordinateDocId cartesianCoordinateDocID, byte filterByte);
        CartesianCoordinateUUID toCartesianCoordinate(IDGeoRecord geoRecord);
        CartesianCoordinateUUID toCartesianCoordinate(double latitude, double longitude, byte[] uuid);
        
        CartesianCoordinateDocId toCartesianCoordinateDocId(CartesianGeoRecord geoRecord);
        CartesianGeoRecord toCartesianGeoRecord(LatitudeLongitudeDocId latLongDocID, byte filterByte);
        CartesianGeoRecord toCartesianGeoRecord(CartesianCoordinateDocId coord);
        
    }
