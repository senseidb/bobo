package com.browseengine.bobo.geosearch.impl;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.bo.CartesianCoordinateUUID;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.score.impl.Conversions;

/**
 * 
 * 
 * @author gcooney
 * @author shandets
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
    public void testCartesianGeoRecord() {
        
        int x,y,z,docid;
        CartesianCoordinateDocId ccd = null;
        CartesianGeoRecord cgr = null;
        CartesianGeoRecord cgrconv = null;
        CartesianCoordinateDocId ccdconv = null;
        Random random = new Random(31);
        CartesianGeoRecordComparator comp = new CartesianGeoRecordComparator();
        for(int i = 0; i < 20; i++) {
            docid = random.nextInt(Integer.MAX_VALUE);
            x = random.nextInt(Integer.MAX_VALUE);
            if(random.nextBoolean()){
                x*=-1;
            }
            y = random.nextInt(Integer.MAX_VALUE);
            if(random.nextBoolean()){
                y*=-1;
            }
            z = random.nextInt(Integer.MAX_VALUE);
            if(random.nextBoolean()){
                z*=-1;
            }
             ccd = new CartesianCoordinateDocId(x, y, z, docid);
             cgr = geoConverter.toCartesianGeoRecord(ccd, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
             ccdconv = geoConverter.toCartesianCoordinateDocId(cgr);
             cgrconv = geoConverter.toCartesianGeoRecord(ccdconv, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
            
            assertTrue(ccd.x == ccdconv.x);
            assertTrue(ccd.y == ccdconv.y);
            assertTrue(Math.abs(ccd.z - ccdconv.z) <= 1);
            assertTrue(ccd.docid == ccdconv.docid);
            assertTrue(comp.compare(cgr, cgrconv) == 0);
        }
        
    }
    @Test
    public void printOutDifferenceInXZYForDifferenceOfDistanceOf1km() {
        // http://andrew.hedges.name/experiments/haversine/ says the distance between 
        // 10.1 and 10.10641 is about 1KM
        double latitudeA = Conversions.d2r(0.0);
        double longitudeA = Conversions.d2r(0.0);
        double latitudeB = Conversions.d2r(0.00641);
        double longitudeB = Conversions.d2r(0.00641);
        int x = geoConverter.getXFromRadians(latitudeA, longitudeA);
        int y = geoConverter.getYFromRadians(latitudeA, longitudeA);
        int z = geoConverter.getZFromRadians(latitudeA);
        int xp = geoConverter.getXFromRadians(latitudeB, longitudeB);
        int yp = geoConverter.getYFromRadians(latitudeB, longitudeB);
        int zp = geoConverter.getZFromRadians(latitudeB);
        
        System.out.println("Distnace btw x, y, z are " + Math.abs(x-xp) + ", " + Math.abs(y-yp) + ", " + Math.abs(z-zp));
    }
    
    @Test 
    public void print2MSquaredDistance() {
        double latitudeA = Conversions.d2r(10.1);
        double longitudeA = Conversions.d2r(10.1);
        double latitudeB = Conversions.d2r(10.10001);
        double longitudeB = Conversions.d2r(10.10001);
        int x = geoConverter.getXFromRadians(latitudeA, longitudeA);
        int y = geoConverter.getYFromRadians(latitudeA, longitudeA);
        int z = geoConverter.getZFromRadians(latitudeA);
        int xp = geoConverter.getXFromRadians(latitudeB, longitudeB);
        int yp = geoConverter.getYFromRadians(latitudeB, longitudeB);
        int zp = geoConverter.getZFromRadians(latitudeB);
        long l = (x-xp)*(x-xp) + (y-yp)*(y-yp) + (z-zp)*(z-zp);
        System.out.println(l);
    }
    
    public enum Dimension {
        X, Y, Z;
    }
}
