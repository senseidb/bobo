package com.browseengine.bobo.geosearch.bo;

import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.impl.MappedFieldNameFilterConverter;

public class GeoSegmentInfo {
    private String segmentName;
    private IFieldNameFilterConverter fieldNameFilterConverter = new MappedFieldNameFilterConverter();
    private int geoVersion;
    
    public String getSegmentName() {
        return segmentName;
    }
    
    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }
    
    public IFieldNameFilterConverter getFieldNameFilterConverter() {
        return fieldNameFilterConverter;
    }
    
    public void setFieldNameFilterConverter(IFieldNameFilterConverter fieldNameFilterConverter) {
        this.fieldNameFilterConverter = fieldNameFilterConverter;
    }
    
    public int getGeoVersion() {
        return geoVersion;
    }

    public void setGeoVersion(int geoVersion) {
        this.geoVersion = geoVersion;
    }
}
