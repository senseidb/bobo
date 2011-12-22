package com.browseengine.bobo.geosearch.impl;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianCoordinate;

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
}
