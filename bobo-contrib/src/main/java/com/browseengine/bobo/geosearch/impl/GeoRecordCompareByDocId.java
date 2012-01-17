package com.browseengine.bobo.geosearch.impl;
import java.util.Comparator;

import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
public class GeoRecordCompareByDocId implements Comparator<Object>
{

    @Override
    public int compare(Object o1, Object o2) {
        GeoConverter gc = new GeoConverter();
        LatitudeLongitudeDocId lldid1 = gc.toLongitudeLatitudeDocId((GeoRecord)o1);
        LatitudeLongitudeDocId lldid2 = gc.toLongitudeLatitudeDocId((GeoRecord)o2);
        int diff = lldid1.docid - lldid2.docid;
        if(diff < 0) {
            return 1;
        } else if (diff > 0) {
            return -1;
        }
        return 0;
    }

}
