package com.browseengine.bobo.geosearch.impl;
import java.util.Comparator;

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
public class CartesianGeoRecordCompareByDocId implements Comparator<Object>
{

    @Override
    public int compare(Object o1, Object o2) {
        GeoConverter gc = new GeoConverter();
        CartesianCoordinateDocId ccdid1 = gc.toCartesianCoordinateDocId((CartesianGeoRecord)o1);
        CartesianCoordinateDocId ccdid2 = gc.toCartesianCoordinateDocId((CartesianGeoRecord)o2);
        int diff = ccdid1.docid - ccdid2.docid;
        if(diff < 0) {
            return 1;
        } else if (diff > 0) {
            return -1;
        }
        return 0;
    }

}
