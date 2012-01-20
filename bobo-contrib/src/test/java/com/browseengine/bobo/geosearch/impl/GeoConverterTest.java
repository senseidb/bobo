package com.browseengine.bobo.geosearch.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.junit.Test;

import com.browseengine.bobo.geosearch.bo.CartesianCoordinateUUID;
import com.browseengine.bobo.geosearch.score.impl.Conversions;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;
import com.browseengine.bobo.geosearch.solo.impl.IDGeoRecordComparator;

/**
 * 
 * 
 * @author gcooney
 *
 */
public class GeoConverterTest {

    GeoConverter geoConverter = new GeoConverter();
    
    /**
     * The tests in this method use the cartesian coordinates calculated by 
     * the conversion spreadsheet provided by the uk
     * ordnance survey as the expected values to compare our conversion logic against.  
     * 
     * http://www.ordnancesurvey.co.uk/oswebsite/gps/information/coordinatesystemsinfo/gpsspreadsheet.html
     * 
     */
    @Test
    public void testToCartesianCoordinates_xyzMethods() {
        double latitude;
        double longitude;
        
        latitude = Conversions.d2r(0);
        longitude = Conversions.d2r(0);
        int x = geoConverter.getXFromRadians(latitude, longitude);
        int y = geoConverter.getYFromRadians(latitude, longitude);
        int z = geoConverter.getZFromRadians(latitude);
        assertCoordinateMatches(x, y, z, Conversions.EARTH_RADIUS_INTEGER_UNITS, 0, 0);
        
        latitude = Conversions.d2r(0);
        longitude = Conversions.d2r(180);
        x = geoConverter.getXFromRadians(latitude, longitude);
        y = geoConverter.getYFromRadians(latitude, longitude);
        z = geoConverter.getZFromRadians(latitude);
        assertCoordinateMatches(x, y, z, -Conversions.EARTH_RADIUS_INTEGER_UNITS, 0, 0);
        
        latitude = Conversions.d2r(90);
        longitude = Conversions.d2r(0);
        x = geoConverter.getXFromRadians(latitude, longitude);
        y = geoConverter.getYFromRadians(latitude, longitude);
        z = geoConverter.getZFromRadians(latitude);
        assertCoordinateMatches(x, y, z, 0, 0, 2125674026);
        
        latitude = Conversions.d2r(-90);
        longitude = Conversions.d2r(0);
        x = geoConverter.getXFromRadians(latitude, longitude);
        y = geoConverter.getYFromRadians(latitude, longitude);
        z = geoConverter.getZFromRadians(latitude);
        assertCoordinateMatches(x, y, z, 0, 0, -2125674026);
        
        latitude = Conversions.d2r(0);
        longitude = Conversions.d2r(90);
        x = geoConverter.getXFromRadians(latitude, longitude);
        y = geoConverter.getYFromRadians(latitude, longitude);
        z = geoConverter.getZFromRadians(latitude);
        assertCoordinateMatches(x, y, z, 0, Conversions.EARTH_RADIUS_INTEGER_UNITS, 0);
        
        latitude = Conversions.d2r(0);
        longitude = Conversions.d2r(-90);
        x = geoConverter.getXFromRadians(latitude, longitude);
        y = geoConverter.getYFromRadians(latitude, longitude);
        z = geoConverter.getZFromRadians(latitude);
        assertCoordinateMatches(x, y, z, 0, -Conversions.EARTH_RADIUS_INTEGER_UNITS, 0);
        
        latitude = Conversions.d2r(86);
        longitude = Conversions.d2r(165);
        x = geoConverter.getXFromRadians(latitude, longitude);
        y = geoConverter.getYFromRadians(latitude, longitude);
        z = geoConverter.getZFromRadians(latitude);
        assertCoordinateMatches(x, y, z, -144192300, 38636210, 2120495991);
        
        latitude = Conversions.d2r(52.65757030139);
        longitude = Conversions.d2r(1.7179215810);
        x = geoConverter.getXFromRadians(latitude, longitude);
        y = geoConverter.getYFromRadians(latitude, longitude);
        z = geoConverter.getZFromRadians(latitude);
        assertCoordinateMatches(x, y , z, 1297492004, 38914868, 1689962941);
        
        latitude = Conversions.d2r(-2.5);
        longitude = Conversions.d2r(96.25);
        x = geoConverter.getXFromRadians(latitude, longitude);
        y = geoConverter.getYFromRadians(latitude, longitude);
        z = geoConverter.getZFromRadians(latitude);
        assertCoordinateMatches(x, y, z, -232753371, 2125255864, -92720598);
    }
    
    @Test
    public void testToCartesianCoordinates_latLong() {
        double latitude = -2.5;
        double longitude = 96.25;
        byte[] uuid  = {(byte)0, (byte)1, (byte)2, (byte)3};
        CartesianCoordinateUUID coordinate = geoConverter.toCartesianCoordinate(latitude, longitude, uuid);
        assertEquals(uuid, coordinate.uuid);
        assertCoordinateMatches(coordinate.x, coordinate.y, coordinate.z, -232753371, 2125255864, -92720598);
    }
    
    private void assertCoordinateMatches(int actualX, int actualY, int actualZ,
            int expectedX, int expectedY, int expectedZ) {
        assertEquals("Unequal X coordinate", expectedX, actualX);
        assertEquals("Unequal Y coordinate", expectedY, actualY);
        assertEquals("Unequal Z coordinate", expectedZ, actualZ);
    }
    
    @Test
    public void testToIDGeoRecord_XCoordOrder_LargeDelta() {
        int totalCoordinates = 10000;
        int startX = Integer.MIN_VALUE;
        int deltaX = 2 * (Integer.MAX_VALUE / totalCoordinates);
        int startY = 0;
        int deltaY = 0;
        int startZ = 0;
        int deltaZ = 0;
        
        createRecordsAndValidateSortOrder(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    @Test
    public void testToIDGeoRecord_XCoordOrder_SmallDelta() {
        int totalCoordinates = 10000;
        int startX = -5000;
        int deltaX = 1;
        int startY = 0;
        int deltaY = 0;
        int startZ = 0;
        int deltaZ = 0;
        
        createRecordsAndValidateSortOrder(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    @Test
    public void testToIDGeoRecord_XCoordOrder_TinyDelta() {
        int totalCoordinates = 10;
        int startX = -5;
        int deltaX = 1;
        int startY = 0;
        int deltaY = 0;
        int startZ = 0;
        int deltaZ = 0;
        
        createRecordsAndValidateSortOrder(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    @Test
    public void testToIDGeoRecord_YCoordOrder_LargeDelta() {
        int startX = 0;
        int deltaX = 0;
        int totalCoordinates = 10000;
        int startY = Integer.MIN_VALUE;
        int deltaY = 2 * (Integer.MAX_VALUE / totalCoordinates);
        int startZ = 0;
        int deltaZ = 0;
        
        createRecordsAndValidateSortOrder(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    @Test
    public void testToIDGeoRecord_YCoordOrder_SmallDelta() {
        int totalCoordinates = 10000;
        int startX = 0;
        int deltaX = 0;
        int startY = -5000;
        int deltaY = 2;
        int startZ = 0;
        int deltaZ = 0;
        
        createRecordsAndValidateSortOrder(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    @Test
    public void testToIDGeoRecord_ZCoordOrder_LargeDelta() {
        int startX = 0;
        int deltaX = 0;
        int startY = 0;
        int deltaY = 0;
        int totalCoordinates = 10000;
        int startZ = Integer.MIN_VALUE;
        int deltaZ = 2 * (Integer.MAX_VALUE / totalCoordinates);
        
        createRecordsAndValidateSortOrder(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    @Test
    public void testToIDGeoRecord_ZCoordOrder_SmallDelta() {
        int totalCoordinates = 10000;
        int startX = 0;
        int deltaX = 0;
        int startY = 0;
        int deltaY = 0;
        int startZ = -5000;
        int deltaZ = 2;
        
        createRecordsAndValidateSortOrder(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    @Test
    public void testToIDGeoRecord_ZCoordOrder_TinyDelta() {
        int totalCoordinates = 10;
        int startX = 0;
        int deltaX = 0;
        int startY = 0;
        int deltaY = 0;
        int startZ = -5;
        int deltaZ = 2;
        
        createRecordsAndValidateSortOrder(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    @Test
    public void testToIDGeoRecord_AllCoordOrder_LargeDelta() {
        int totalCoordinates = 10000;
        int startX = Integer.MIN_VALUE;
        int deltaX = 2 * (Integer.MAX_VALUE / totalCoordinates);
        int startY = Integer.MIN_VALUE;
        int deltaY = 2 * (Integer.MAX_VALUE / totalCoordinates);
        int startZ = Integer.MIN_VALUE;
        int deltaZ = 2 * (Integer.MAX_VALUE / totalCoordinates);
        
        createRecordsAndValidateSortOrder(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    @Test
    public void testToIDGeoRecord_AllCoordOrder_SmallDelta() {
        int totalCoordinates = 10000;
        int startX = -5000;
        int deltaX = 1;
        int startY = -5000;
        int deltaY = 2;
        int startZ = -5000;
        int deltaZ = 2;
        
        createRecordsAndValidateSortOrder(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    private void createRecordsAndValidateSortOrder(int startX, int deltaX, int startY, int deltaY,
            int startZ, int deltaZ, int totalCoordinates) {
        ArrayList<IDGeoRecord> list = new ArrayList<IDGeoRecord>(totalCoordinates);
        TreeSet<IDGeoRecord> tree = new TreeSet<IDGeoRecord>(new IDGeoRecordComparator());
        
        for (int i = 0; i < totalCoordinates; i++) {
            int x = startX + i * deltaX;
            int y = startY + i * deltaY;
            int z = startZ + i * deltaZ;
            byte[] id = Integer.toString(i).getBytes();
            
            IDGeoRecord geoRecord = geoConverter.toIDGeoRecord(x, y, z, id);
            
            assertEquals("Iteration " + i + ": Unexpected id", id, geoRecord.id);
            
            list.add(geoRecord);
            tree.add(geoRecord);
        }
        
        int i = 0;
        for (Iterator<IDGeoRecord> geoIter = tree.iterator(); geoIter.hasNext();) {
            IDGeoRecord treeNext = geoIter.next();
            IDGeoRecord arrayNext = list.get(i);
            
            assertEquals("unexepcted record at index " + i, arrayNext, treeNext);
            
            i++;
        }
    }
    
    @Test
    public void testInterlaceThenUninterlace_LargeRange() {
        int totalCoordinates = 10000;
        int startX = Integer.MIN_VALUE;
        int deltaX = 2 * (Integer.MAX_VALUE / totalCoordinates);
        int startY = Integer.MIN_VALUE;
        int deltaY = 2 * (Integer.MAX_VALUE / totalCoordinates);
        int startZ = Integer.MIN_VALUE;
        int deltaZ = 2 * (Integer.MAX_VALUE / totalCoordinates);
        
        interlaceAndUninterlace(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    @Test
    public void testInterlaceThenUninterlace_SmallRange() {
        int totalCoordinates = 100;
        int startX = -50;
        int deltaX = 1;
        int startY = -50;
        int deltaY = 1;
        int startZ = -50;
        int deltaZ = 1;
        
        interlaceAndUninterlace(startX, deltaX, startY, deltaY, 
                startZ, deltaZ, totalCoordinates);
    }
    
    @Test
    public void testInterlaceThenUninterlace_EveryPowerOfTwo_XOnly() {
        int totalCoordinates = 1;
        int startX = Integer.MIN_VALUE;
        int deltaX = 0;
        int startY = Integer.MIN_VALUE;
        int deltaY = 0;
        int startZ = Integer.MIN_VALUE;
        int deltaZ = 0;
        
        for (int i=0; i < 31; i++) {
            startX = 2^i;
            interlaceAndUninterlace(startX, deltaX, startY, deltaY, 
                    startZ, deltaZ, totalCoordinates);
            
            startX = 2^i + Integer.MIN_VALUE;
            
            interlaceAndUninterlace(startX, deltaX, startY, deltaY, 
                    startZ, deltaZ, totalCoordinates);
        }
    }
    
    @Test
    public void testInterlaceThenUninterlace_EveryPowerOfTwo_YOnly() {
        int totalCoordinates = 1;
        int startX = Integer.MIN_VALUE;
        int deltaX = 0;
        int startY = Integer.MIN_VALUE;
        int deltaY = 0;
        int startZ = Integer.MIN_VALUE;
        int deltaZ = 0;
        
        for (int i=0; i < 31; i++) {
            startY = 2^i;
            interlaceAndUninterlace(startX, deltaX, startY, deltaY, 
                    startZ, deltaZ, totalCoordinates);
            
            startY = 2^i + Integer.MIN_VALUE;
            
            interlaceAndUninterlace(startX, deltaX, startY, deltaY, 
                    startZ, deltaZ, totalCoordinates);
        }
    }
    
    @Test
    public void testInterlaceThenUninterlace_EveryPowerOfTwo_ZOnly() {
        int totalCoordinates = 1;
        int startX = Integer.MIN_VALUE;
        int deltaX = 0;
        int startY = Integer.MIN_VALUE;
        int deltaY = 0;
        int startZ = Integer.MIN_VALUE;
        int deltaZ = 0;
        
        for (int i=0; i < 31; i++) {
            startZ = 2^i;
            interlaceAndUninterlace(startX, deltaX, startY, deltaY, 
                    startZ, deltaZ, totalCoordinates);
            
            startZ = 2^i + Integer.MIN_VALUE;
            
            interlaceAndUninterlace(startX, deltaX, startY, deltaY, 
                    startZ, deltaZ, totalCoordinates);
        }
    }
    
    private void interlaceAndUninterlace(int startX, int deltaX, int startY, int deltaY, int startZ, int deltaZ,
            int totalCoordinates) {
        for (int i = 0; i < totalCoordinates; i++) {
            int x = startX + i * deltaX;
            int y = startY + i * deltaY;
            int z = startZ + i * deltaZ;
            byte[] id = Integer.toString(i).getBytes();
            
            CartesianCoordinateUUID expectedCoordinate = new CartesianCoordinateUUID(x, y, z, id);
            IDGeoRecord geoRecord = geoConverter.toIDGeoRecord(x, y, z, id);
            
            CartesianCoordinateUUID actualCoordinate = geoConverter.toCartesianCoordinate(geoRecord);

            assertEquals("UUID should not change", expectedCoordinate.uuid, actualCoordinate.uuid);
            assertTrue("x should not change: expected=" + expectedCoordinate.x + "; actual=" + 
                    actualCoordinate.x, expectedCoordinate.x - actualCoordinate.x == 0);
            assertTrue("y should not change by more than 1: expected=" + expectedCoordinate.y + "; actual=" + 
                    actualCoordinate.y, Math.abs(expectedCoordinate.y - actualCoordinate.y) <= 1);
            assertTrue("z should not change by more than 1: expected=" + expectedCoordinate.z + "; actual=" + 
                    actualCoordinate.z, Math.abs(expectedCoordinate.z - actualCoordinate.z) <= 1);
        }
    }

    public enum Dimension {
        X, Y, Z;
    }
}
