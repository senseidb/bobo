package com.browseengine.bobo.geosearch.solo.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.test.annotation.IfProfileValue;

import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;

/**
 * 
 * @author gcooney
 *
 */
@IfProfileValue(name = "test-suite", values = { "unit", "all" })
public class IDGeoRecordComparatorTest {
    final IDGeoRecordComparator comparator = new IDGeoRecordComparator();
    
    @Test
    public void testEqual() {
        long highOrder = 10l;
        int lowOrder = 20;
        byte[] id = new byte[] {(byte) 5};
        
        IDGeoRecord idGeoRecord1 = new IDGeoRecord(highOrder, lowOrder, id); 
        IDGeoRecord idGeoRecord2 = new IDGeoRecord(highOrder, lowOrder, id);

        assertEquals(0, comparator.compare(idGeoRecord1, idGeoRecord2));
    }
    
    @Test
    public void testHighOrderGreater() {
        long highOrder1 = 15l;
        long highOrder2 = 10l;
        int lowOrder1 = 10;
        int lowOrder2 = 20;
        byte[] id = new byte[] {(byte) 5};
        
        IDGeoRecord idGeoRecord1 = new IDGeoRecord(highOrder1, lowOrder1, id); 
        IDGeoRecord idGeoRecord2 = new IDGeoRecord(highOrder2, lowOrder2, id);

        assertTrue("Should be greater than 0", comparator.compare(idGeoRecord1, idGeoRecord2) > 0);
    }
    
    @Test
    public void testHighOrderLesser() {
        long highOrder1 = 10l;
        long highOrder2 = 15l;
        int lowOrder1 = 20;
        int lowOrder2 = 10;
        byte[] id = new byte[] {(byte) 5};
        
        IDGeoRecord idGeoRecord1 = new IDGeoRecord(highOrder1, lowOrder1, id); 
        IDGeoRecord idGeoRecord2 = new IDGeoRecord(highOrder2, lowOrder2, id);

        assertTrue("Should be less than 0", comparator.compare(idGeoRecord1, idGeoRecord2) < 0);
    }
    
    @Test
    public void testLowOrderGreater() {
        long highOrder1 = 15l;
        long highOrder2 = 15l;
        int lowOrder1 = 20;
        int lowOrder2 = 10;
        byte[] id = new byte[] {(byte) 5};
        
        IDGeoRecord idGeoRecord1 = new IDGeoRecord(highOrder1, lowOrder1, id); 
        IDGeoRecord idGeoRecord2 = new IDGeoRecord(highOrder2, lowOrder2, id);

        assertTrue("Should be greater than 0", comparator.compare(idGeoRecord1, idGeoRecord2) > 0);
    }
    
    @Test
    public void testLowOrderLesser() {
        long highOrder1 = 15l;
        long highOrder2 = 15l;
        int lowOrder1 = 10;
        int lowOrder2 = 20;
        byte[] id = new byte[] {(byte) 5};
        
        IDGeoRecord idGeoRecord1 = new IDGeoRecord(highOrder1, lowOrder1, id); 
        IDGeoRecord idGeoRecord2 = new IDGeoRecord(highOrder2, lowOrder2, id);

        assertTrue("Should be less than 0", comparator.compare(idGeoRecord1, idGeoRecord2) < 0);
    }
    
    @Test
    public void testIDGreater() {
        long highOrder1 = 15l;
        long highOrder2 = 15l;
        int lowOrder1 = 10;
        int lowOrder2 = 10;
        byte[] id1 = new byte[] {(byte) 12};
        byte[] id2 = new byte[] {(byte) 10};
        
        IDGeoRecord idGeoRecord1 = new IDGeoRecord(highOrder1, lowOrder1, id1); 
        IDGeoRecord idGeoRecord2 = new IDGeoRecord(highOrder2, lowOrder2, id2);

        assertTrue("Should be greater than 0", comparator.compare(idGeoRecord1, idGeoRecord2) > 0);
    }
    
    @Test
    public void testIDLesser() {
        long highOrder1 = 15l;
        long highOrder2 = 15l;
        int lowOrder1 = 10;
        int lowOrder2 = 10;
        byte[] id1 = new byte[] {(byte) 8};
        byte[] id2 = new byte[] {(byte) 10};
        
        IDGeoRecord idGeoRecord1 = new IDGeoRecord(highOrder1, lowOrder1, id1); 
        IDGeoRecord idGeoRecord2 = new IDGeoRecord(highOrder2, lowOrder2, id2);

        assertTrue("Should be less than 0", comparator.compare(idGeoRecord1, idGeoRecord2) < 0);
    }
    
    @Test
    public void testIDLonger() {
        long highOrder1 = 15l;
        long highOrder2 = 15l;
        int lowOrder1 = 10;
        int lowOrder2 = 10;
        byte[] id1 = new byte[] {(byte) 12, (byte) -16};
        byte[] id2 = new byte[] {(byte) 10};
        
        IDGeoRecord idGeoRecord1 = new IDGeoRecord(highOrder1, lowOrder1, id1); 
        IDGeoRecord idGeoRecord2 = new IDGeoRecord(highOrder2, lowOrder2, id2);

        assertTrue("Should be greater than 0", comparator.compare(idGeoRecord1, idGeoRecord2) > 0);
    }
    
    @Test
    public void testIDShorter() {
        long highOrder1 = 15l;
        long highOrder2 = 15l;
        int lowOrder1 = 10;
        int lowOrder2 = 10;
        byte[] id1 = new byte[] {(byte) 8};
        byte[] id2 = new byte[] {(byte) 10, (byte) 12};
        
        IDGeoRecord idGeoRecord1 = new IDGeoRecord(highOrder1, lowOrder1, id1); 
        IDGeoRecord idGeoRecord2 = new IDGeoRecord(highOrder2, lowOrder2, id2);

        assertTrue("Should be less than 0", comparator.compare(idGeoRecord1, idGeoRecord2) < 0);
    }
}
