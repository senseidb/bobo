package com.browseengine.bobo.geosearch.bo;

import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.impl.MappedFieldNameFilterConverter;

public class GeoSegmentInfo {
    public static final int BYTES_PER_RECORD_V1 = 13;
    
    private String segmentName;
    private IFieldNameFilterConverter fieldNameFilterConverter = new MappedFieldNameFilterConverter();
    private int geoVersion;
    private int bytesPerRecord = BYTES_PER_RECORD_V1;
    
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
    
    public int getBytesPerRecord() {
        return bytesPerRecord;
    }
    
    public void setBytesPerRecord(int bytesPerRecord) {
        this.bytesPerRecord = bytesPerRecord;
    }
}
