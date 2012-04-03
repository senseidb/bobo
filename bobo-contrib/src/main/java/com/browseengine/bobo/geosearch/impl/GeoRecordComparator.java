package com.browseengine.bobo.geosearch.impl;
import java.util.Comparator;

import com.browseengine.bobo.geosearch.bo.GeoRecord;
public class GeoRecordComparator implements Comparator<GeoRecord>
{
    @Override
    public int compare(GeoRecord geoRecordFirst, GeoRecord geoRecordSecond) {
        long diff = geoRecordFirst.highOrder - geoRecordSecond.highOrder;
        if(diff > 0) {
            return 1;
        }
        if (diff < 0) {
            return -1;
        }
        int idiff = geoRecordFirst.lowOrder - geoRecordSecond.lowOrder;
        if(idiff > 0) {
            return 1;
        }
        if (idiff < 0) {
            return -1;
        }
        return 0;
    }
}
