package com.browseengine.bobo.geosearch;
import java.util.Iterator;
import java.util.TreeSet;

import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;

public interface IGeoUtil {
    Iterator<CartesianGeoRecord> getGeoRecordIterator(Iterator<LatitudeLongitudeDocId> lldidIter);
    TreeSet<CartesianGeoRecord> getBinaryTreeOrderedByBitMag(Iterator<CartesianGeoRecord> grIter);
    TreeSet<CartesianGeoRecord> getBinaryTreeOrderedByBitMag();
    TreeSet<CartesianGeoRecord> getBinaryTreeOrderedByDocId(Iterator<CartesianGeoRecord> grIter);
    Iterator<CartesianGeoRecord> getGeoRecordRangeIterator(TreeSet<CartesianGeoRecord> tree, CartesianGeoRecord minRange, CartesianGeoRecord maxRange);
}

