/**
 * 
 */
package com.browseengine.bobo.geosearch;

import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;

/**
 * @author Ken McCracken
 *
 */
    public interface IGeoConverter {
        LatitudeLongitudeDocId toLongitudeLatitudeDocId(GeoRecord geoRecord);
        GeoRecord toGeoRecord(String fieldName, LatitudeLongitudeDocId longitudeLatitudeDocId);
        GeoRecord toGeoRecord(byte filterByte, LatitudeLongitudeDocId longitudeLatitudeDocId);
        void setFieldNameFilterConverter(IFieldNameFilterConverter fieldNameFilterConverter);
        IFieldNameFilterConverter getFieldNameFilterConverter();
    }
