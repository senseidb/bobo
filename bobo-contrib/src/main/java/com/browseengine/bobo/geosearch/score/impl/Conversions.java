// ADAPTED FROM FORK OF BOBO BROWSE ENGINE


/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  spackle
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * contact owner@browseengine.com.
 */

//package com.browseengine.local.service;
package com.browseengine.bobo.geosearch.score.impl;

/**
 * Converts from degrees to radians, and radians to degrees.
 * 
 * @author spackle
 *
 */
public class Conversions {
    private static final double DEG_TO_RAD = Math.PI/180.;
    static final double EARTH_RADIUS_METERS = 6378137.0;
    public static final int EARTH_RADIUS_INTEGER_UNITS = 2140000000;
    static final double EARTH_RADIUS_METERS_TO_INTEGER_UNITS =  EARTH_RADIUS_INTEGER_UNITS / EARTH_RADIUS_METERS;
    
    public static double d2r(double deg) {
        return deg*DEG_TO_RAD;
    }
    public static double r2d(double rad) {
        return rad/DEG_TO_RAD;
    }

    /**
     * a 5-k run is approx. 3.1 mi.
     */
    private static final float MILES_TO_KM = 5f/3.1f;
    
    public static float mi2km(float mi) {
        return mi*MILES_TO_KM;
    }
    
    public static float km2mi(float km) {
        return km/MILES_TO_KM;
    }
    
    public static int radiusMetersToIntegerUnits(double meters) {
        return (int)(meters * EARTH_RADIUS_METERS_TO_INTEGER_UNITS); 
    }
    
    public static double radiusIntegerUnitsToMeters(int integerUnits) {
        return integerUnits / EARTH_RADIUS_METERS_TO_INTEGER_UNITS;
    }
    
    public static int calculateMinimumCoordinate(int originalPoint, int delta) {
        if (originalPoint > 0 || 
                originalPoint > Integer.MIN_VALUE + delta) {
            return originalPoint - delta;
        } else {
            return Integer.MIN_VALUE;
        }
    }
    
    public static int calculateMaximumCoordinate(int originalPoint, int delta) {
        if (originalPoint < 0 || 
                originalPoint < Integer.MAX_VALUE - delta) {
            return originalPoint + delta;
        } else {
            return Integer.MAX_VALUE;
        }
    }
    public static double unitsToMeters(double distance) {
        return distance / EARTH_RADIUS_METERS_TO_INTEGER_UNITS;
    }
}
