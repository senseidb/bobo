package com.browseengine.bobo.geosearch.impl;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

import com.browseengine.bobo.geosearch.IGeoUtil;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;

@Component
public class GeoUtil implements IGeoUtil {

    @Override
    public Iterator<GeoRecord> getGeoRecordIterator(Iterator<LatitudeLongitudeDocId> lldidIter) {
        GeoConverter gc = new GeoConverter();
        ArrayList<GeoRecord> grl = new ArrayList<GeoRecord>();
        while (lldidIter.hasNext()) {
            grl.add(gc.toGeoRecord(null, null, lldidIter.next()));
        }
        return grl.iterator();
    }

    @Override
    public TreeSet<GeoRecord> getBinaryTreeOrderedByBitMag(Iterator<GeoRecord> grIter) {
        TreeSet<GeoRecord> tree = getBinaryTreeOrderedByBitMag();
        while(grIter.hasNext()){
            tree.add(grIter.next());
        }
        return tree;
    }

    @Override
    public TreeSet<GeoRecord> getBinaryTreeOrderedByBitMag() {
        return new TreeSet<GeoRecord>(new GeoRecordComparator());
    }
    
    @Override
    public TreeSet<GeoRecord> getBinaryTreeOrderedByDocId(Iterator<GeoRecord> grtIter) {
        TreeSet<GeoRecord> tree = new TreeSet<GeoRecord>(new GeoRecordCompareByDocId());
        while(grtIter.hasNext()){
            tree.add(grtIter.next());
        }
        return tree;
    }

    @Override
    public Iterator<GeoRecord> getGeoRecordRangeIterator(TreeSet<GeoRecord> tree, GeoRecord minRange, GeoRecord maxRange) {
        return tree.subSet(minRange, maxRange).iterator();
    }

    private static final double MINIMUM_LONGITUDE_EXCLUSIVE = -180.;
    private static final double MAXIMUM_LONGITUDE_INCLUSIVE = 180.;
    private static final double MINIMUM_LATITUDE_INCLUSIVE = -90.;
    private static final double MAXIMUM_LATITUDE_INCLUSIVE = 90.;
    
    public static boolean isValidLongitude(Double longitude) {
        return null != longitude 
        && MINIMUM_LONGITUDE_EXCLUSIVE < longitude 
        && longitude <= MAXIMUM_LONGITUDE_INCLUSIVE;
    }
    
    public static boolean isValidLatitude(Double latitude) {
        return null != latitude 
        && MINIMUM_LATITUDE_INCLUSIVE <= latitude
        && latitude <= MAXIMUM_LATITUDE_INCLUSIVE;
    }
    

}


