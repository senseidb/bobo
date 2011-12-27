package com.browseengine.bobo.geosearch.impl;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.junit.Test;

import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianCoordinate;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;
import com.browseengine.bobo.geosearch.solo.impl.IDGeoRecordComparator;

/**
 * 
 * 
 * @author gcooney
 *
 */
public class GeoConverterTest {

    IGeoConverter geoConverter = new GeoConverter();
    
    /**
     * The tests in this method use the cartesian coordinates calculated by 
     * the conversion spreadsheet provided by the uk
     * ordnance survey as the expected values to compare our conversion logic against.  
     * 
     * http://www.ordnancesurvey.co.uk/oswebsite/gps/information/coordinatesystemsinfo/gpsspreadsheet.html
     * 
     */
    @Test
    public void testToCartesianCoordinates() {
        double latitude;
        double longitude;
        CartesianCoordinate coord;
        
        latitude = 0;
        longitude = 0;
        coord = geoConverter.toCartesianCoordinate(latitude, longitude);
        assertCoordinateMatches(coord, GeoConverter.EARTH_RADIUS_INTEGER_UNITS, 0, 0);
        
        latitude = 0;
        longitude = 180;
        coord = geoConverter.toCartesianCoordinate(latitude, longitude);
        assertCoordinateMatches(coord, -GeoConverter.EARTH_RADIUS_INTEGER_UNITS, 0, 0);
        
        latitude = 90;
        longitude = 0;
        coord = geoConverter.toCartesianCoordinate(latitude, longitude);
        assertCoordinateMatches(coord, 0, 0, 2125674026);
        
        latitude = -90;
        longitude = 0;
        coord = geoConverter.toCartesianCoordinate(latitude, longitude);
        assertCoordinateMatches(coord, 0, 0, -2125674026);
        
        latitude = 0;
        longitude = 90;
        coord = geoConverter.toCartesianCoordinate(latitude, longitude);
        assertCoordinateMatches(coord, 0, GeoConverter.EARTH_RADIUS_INTEGER_UNITS, 0);
        
        latitude = 0;
        longitude = -90;
        coord = geoConverter.toCartesianCoordinate(latitude, longitude);
        assertCoordinateMatches(coord, 0, -GeoConverter.EARTH_RADIUS_INTEGER_UNITS, 0);
        
        latitude = 86;
        longitude = 165;
        coord = geoConverter.toCartesianCoordinate(latitude, longitude);
        assertCoordinateMatches(coord, -144192300, 38636210, 2120495991);
        
        latitude = 52.65757030139;
        longitude = 1.7179215810;
        coord = geoConverter.toCartesianCoordinate(latitude, longitude);
        assertCoordinateMatches(coord, 1297492004, 38914868, 1689962941);
        
        latitude = -2.5;
        longitude = 96.25;
        coord = geoConverter.toCartesianCoordinate(latitude, longitude);
        assertCoordinateMatches(coord, -232753371, 2125255864, -92720598);
    }
    
    private void assertCoordinateMatches(CartesianCoordinate coord, int expectedX, int expectedY, 
            int expectedZ) {
        assertEquals("Unequal X coordinate", expectedX, coord.x);
        assertEquals("Unequal Y coordinate", expectedY, coord.y);
        assertEquals("Unequal Z coordinate", expectedZ, coord.z);
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
            CartesianCoordinate coordinate = new CartesianCoordinate(x, y, z);
            
            byte[] id = Integer.toString(i).getBytes();
            IDGeoRecord geoRecord = geoConverter.toIDGeoRecord(coordinate, id);
            
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
    
    public enum Dimension {
        X, Y, Z;
    }
}
