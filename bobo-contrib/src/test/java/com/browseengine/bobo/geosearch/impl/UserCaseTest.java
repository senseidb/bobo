package com.browseengine.bobo.geosearch.impl;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.junit.Test;

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;

public class UserCaseTest {
    
    CartesianGeoRecordComparator geoComparator = new CartesianGeoRecordComparator();
    
    @Test
    public void useCase_Test() throws Exception{
        byte filterByte = CartesianGeoRecord.DEFAULT_FILTER_BYTE;
        
        for(int i = 0; i < 100; i++) {
            GeoConverter gc = new GeoConverter();
            GeoUtil gu = new GeoUtil();
            // User Creates a Range
            LatitudeLongitudeDocId minLldidRange, maxLldidRange;
            CartesianGeoRecord minGeoRecordRange, maxGeoRecordRange;
            minLldidRange = getRandomLongitudeLatitudeDocId(true);
            maxLldidRange = getRandomLongitudeLatitudeDocId(false);
            // Make sure minRange is <= maxRange
            while 
                (geoComparator.compare( 
                    gc.toCartesianGeoRecord(minLldidRange, filterByte), 
                    gc.toCartesianGeoRecord(maxLldidRange, filterByte))
                   == 1 
                   ||
                   (minLldidRange.docid - maxLldidRange.docid) < 0
                ) 
            {
                           maxLldidRange = getRandomLongitudeLatitudeDocId(false);
            }
            minGeoRecordRange = gc.toCartesianGeoRecord(minLldidRange, filterByte);
            maxGeoRecordRange = gc.toCartesianGeoRecord(maxLldidRange, filterByte);
            
            // Create a random list of LongitudeLatitude (i.e. lng lat docid triplets).
            ArrayList<LatitudeLongitudeDocId> lldid = 
                getArrayListLLDID(
                                      (int)(10  + 100*Math.random())  
                                 );
            
            // Get an iterator of Georecords
            Iterator<CartesianGeoRecord> grIter = gu.getGeoRecordIterator(lldid.iterator());
           
            // Create a self balancing binary search tree constructed by order of bit interlace magnitude 
            TreeSet<CartesianGeoRecord> tree = gu.getBinaryTreeOrderedByBitMag(grIter);
            
            // Get an iterator of GeoRecord objects in order of increase magnitude of their bit interlaced representation
            // and within and/or including the range
            Iterator <CartesianGeoRecord> inRangeInOrderByBitInterMag = gu.getGeoRecordRangeIterator(tree, minGeoRecordRange, maxGeoRecordRange);
            // Test this.
            test_IfDocumentsAreInRangeAndInOrderOfBitInterlacedMag(inRangeInOrderByBitInterMag);
     
            
            // Create a self balancing binary search tree constructed by order of docid 
            tree = gu.getBinaryTreeOrderedByDocId(grIter);
            // Get an iterator of GeoRecord objects in order of increasing docid 
            // and within and/or including the range
            Iterator <CartesianGeoRecord> inRangeInOrderByDocId = gu.getGeoRecordRangeIterator(tree, minGeoRecordRange, maxGeoRecordRange);
            // Test this
            test_IfDocumentsAreInRangeAndInOrderOfDocid(inRangeInOrderByDocId);
        }
    }
    
    public void test_IfDocumentsAreInRangeAndInOrderOfDocid(Iterator <CartesianGeoRecord> inRangeInOrderByDocId) {
        if(inRangeInOrderByDocId.hasNext()) {
            GeoConverter gc = new GeoConverter();
            CartesianCoordinateDocId ccdidCurrent, ccdidNext;
            ccdidNext = gc.toCartesianCoordinateDocId(inRangeInOrderByDocId.next());
            while(inRangeInOrderByDocId.hasNext()) {
                    ccdidCurrent = ccdidNext;
                    ccdidNext = gc.toCartesianCoordinateDocId(inRangeInOrderByDocId.next());
                    assertTrue("The records are out of order by their document id. ",
                            (ccdidNext.docid - ccdidCurrent.docid) >= 0);
            }
        }
    }
    
    public void test_IfDocumentsAreInRangeAndInOrderOfBitInterlacedMag(Iterator <CartesianGeoRecord> inRangeInOrderByBitInterMag) {
        if(inRangeInOrderByBitInterMag.hasNext()) {    
            CartesianGeoRecord next = inRangeInOrderByBitInterMag.next(), current = null;
            while(inRangeInOrderByBitInterMag.hasNext()) {
                    current = next;
                    next = inRangeInOrderByBitInterMag.next();
                    assertTrue("The records are out of order by magnitude of their bit interlace representation. ",
                            geoComparator.compare(current, next) != 1);
            }
        }
    }
    
    public LatitudeLongitudeDocId getRandomLongitudeLatitudeDocId(boolean isMin) {
        int docid;
        double lat,lng;
            lng = Math.random() * 360.0 - 180.0;
            lat = Math.random() * 180.0 - 90.0; 
            if(isMin) {
                docid = (int)(1 + Math.random() * (Integer.MAX_VALUE/2));
            } else {
                docid = (int)(1 + Math.random() * Integer.MAX_VALUE);
            }
        return new LatitudeLongitudeDocId(lat, lng, docid);
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
