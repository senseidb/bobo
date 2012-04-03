

package com.browseengine.bobo.geosearch.impl;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.junit.Test;

import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;

public class GeoRecordBTreeTest {
    
    GeoRecordComparator geoComparator = new GeoRecordComparator();
    
    @Test
    public void test_Test() throws IOException {
        for(int i = 0; i < 100; i++) {
            // Create a Random Binary Tree
            TreeSet<GeoRecord> tree = getRandomBTreeOrderedByBitMag(10 + (int) (100 * Math.random()));
            // Construct a GeoRecordBTreeAsArray
            GeoRecordBTree grbt = new GeoRecordBTree(tree);
            // Test if the GeoRecordBTreeAsArray was constructed correctly
            test_CompleteTreeIsOrderedCorrectly(grbt);
            // Test itterator functionality
            test_IteratorFunctionality(grbt);
        }
    }
    
    public void test_IteratorFunctionality(GeoRecordBTree grbt) throws IOException {
        GeoConverter gc = new GeoConverter();
        ArrayList<LatitudeLongitudeDocId> lldida = getArrayListLLDID(2);
        GeoRecord minRecord, maxRecord;
        minRecord = gc.toGeoRecord((byte)0, lldida.get(0));
        maxRecord = gc.toGeoRecord((byte)0, lldida.get(1));
        if(geoComparator.compare(minRecord, maxRecord) == 1) {
            minRecord = gc.toGeoRecord((byte)0, lldida.get(1));
            maxRecord = gc.toGeoRecord((byte)0, lldida.get(0));
        }
        Iterator<GeoRecord> gIt = grbt.getIterator(minRecord, maxRecord);
        GeoRecord current=null, next=null;
        while(gIt.hasNext()) {
            if(next != null) {
                current = next;
            }
            next = gIt.next();
            if(current != null) {
                assertTrue("The indexer is out of order.",geoComparator.compare(current, next) != 1);
            }
            assertTrue("Iterator is out of range ",geoComparator.compare(next, minRecord) != -1 ||
                    geoComparator.compare(next, maxRecord) != 1);
        }
    }
    
    public void test_CompleteTreeIsOrderedCorrectly(GeoRecordBTree grbt) throws IOException {
        int len = grbt.arrayLength;
        for(int index = 0; index < len; index++) {
            if(index > 0) {
                if(grbt.isALeftChild(index)) {
                     assertTrue("Left child incorecctly greater than parent: "
                             , grbt.compareValuesAt(index, grbt.getParentIndex(index)) != 1);       
                } else {
                    assertTrue("Right child incorecctly less than parent: "
                            , grbt.compareValuesAt(index, grbt.getParentIndex(index)) != -1);   
                }
            }
            if(grbt.hasLeftChild(index)) {
                assertTrue("Left child incorecctly greater than parent: "
                        , grbt.compareValuesAt(grbt.getLeftChildIndex(index), index) != 1);  
            } else if (grbt.hasRightChild(index)) {
                assertTrue("Right child incorecctly less than parent: "
                        , grbt.compareValuesAt(grbt.getRightChildIndex(index), index) != 1);    
            }
        }
    }
    
    public TreeSet<GeoRecord>     getRandomBTreeOrderedByBitMag(int len) {
        Iterator <LatitudeLongitudeDocId> lldidIter = getArrayListLLDID(len).iterator();
        TreeSet<GeoRecord> tree = new TreeSet<GeoRecord>(new GeoRecordComparator());
        GeoConverter gc = new GeoConverter();
        while(lldidIter.hasNext()) {
            byte filterByte = GeoRecord.DEFAULT_FILTER_BYTE;
            tree.add(gc.toGeoRecord(filterByte, lldidIter.next()));
        }
        return tree;
    }
    
    public ArrayList<LatitudeLongitudeDocId> getArrayListLLDID(int len) {
        
        ArrayList<LatitudeLongitudeDocId> lldid = new ArrayList<LatitudeLongitudeDocId>();
        int i, docid;
        double lat,lng;
        for(i=0;i<len;i++) {
            lng = Math.random() * 360.0 - 180.0;
            lat = Math.random() * 180.0 - 90.0; 
            docid = (int)(1 + Math.random() * Integer.MAX_VALUE);
            lldid.add(new LatitudeLongitudeDocId(lat, lng, docid));
        }
        return lldid;
    }
}
