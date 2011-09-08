package com.browseengine.bobo.geosearch.impl;
import java.util.Comparator;

import com.browseengine.bobo.geosearch.bo.GeoRecord;
public class GeoRecordComparator implements Comparator<GeoRecord>
{
    @Override
    public int compare(GeoRecord o1, GeoRecord o2) {
        GeoUtil gu = new GeoUtil();
        return gu.compare(o1, o2);
    }
}
