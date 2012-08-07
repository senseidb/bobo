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
 * send mail to owner@browseengine.com.
 */
//package com.browseengine.local.service.geosearch;

package com.browseengine.bobo.geosearch.score.impl;

/**
 * @author spackle
 * 
 */
public class HaversineFormula {

    /** mean radius of the earth, in miles */
    private static final double R = 3956;

    /**
     * inputs are in radians. computes the Haversine distance between 2 points,
     * in miles.
     * 
     * <p>
     * source: http://www.movable-type.co.uk/scripts/GIS-FAQ-5.1.html
     * 
     * <div> Haversine Formula (from R.W. Sinnott, "Virtues of the Haversine",
     * Sky and Telescope, vol. 68, no. 2, 1984, p. 159):
     * 
     * <pre>
     * 	dlon = lon2 - lon1
     * 	dlat = lat2 - lat1
     * 	a = sin^2(dlat/2) + cos(lat1) * cos(lat2) * sin^2(dlon/2)
     * 	c = 2 * arcsin(min(1,sqrt(a)))
     * 	d = R * c
     * </pre>
     * 
     * </div>
     */
    public static float computeHaversineDistanceMiles(double lon1, double lat1, double lon2, double lat2) {
        double dLon = lon2 - lon1;
        double dLat = lat2 - lat1;

        double sinLat = Math.sin(dLat / 2);
        double sinLon = Math.sin(dLon / 2);
        double a = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLon * sinLon;
        double c = 2 * Math.asin(Math.min(1, Math.sqrt(a)));
        double d = R * c;

        return (float) d;
    }

    /* Taylor series approximation. 
     * static double sin(double x) { double x2 = x*x; return x*(1 - (x2)*(1 -
     * (x2)*(1 - (x2)/(6*7) ) / (4*5) ) /(2*3) ); } static double cos(double x)
     * { double x2 = x*x; return 1 - (x2/2)*(1-x2/6)*(1/2 - x2/90); } static
     * double asin(double x){ double x2 = x*x; return x*(1 + (x2/2)*(1/3 +
     * (x2/4)*(3/5 + (5*x2/2)*(1/7 + 7*x2/(36))))); } static double min(double
     * a, double b) { if (a < b) return a; return b; }
     */

    /**
     * all inputs and outputs are in radians. might be screwed up near the
     * poles.
     * 
     * @param radius_d
     *            to the caller, this is the radius of the search, in miles.
     *            however, we treat this as our value "d" in the Haversine
     *            formula. hence, the name reflects both.
     * @return the latitudinal displacement, in radians
     */
    public static double computeLatBoundary(float radius_d) {
        // d =
        // R*(2*sin^-1(min(1,sqrt(sin^2(dlat/2)+cos(lat1)*cos(lat2)*sin^2(dlon/2)))))
        // dlon = 0, so sin^2(dlon/2) = 0
        // so lat boundary is independent of latitude, as expected
        // d = R*(2*sin^-1(min(1,sqrt(sin^2(dlat/2)))))
        // as long as we are not antipodal, we know the sqrt(sin^2(...)) < 1,
        // and distances more than half way around the earth don't make
        // sense as inputs,
        // and we are just looking for the positive value,
        // so sqrt(sin^2(...)) == sin(...)
        // dlat = d/R
        return radius_d / R;
    }

    /**
     * all inputs and outputs are in radians. might be screwed up near the
     * poles. lat1 can't be PI/2, for example.
     */
    public static double computeLonBoundary(double lat1, float radius_d) {
        // d =
        // R*(2*sin^-1(min(1,sqrt(sin^2(dlat/2)+cos(lat1)*cos(lat2)*sin^2(dlon/2)))))
        // dlat = 0, so sin^2(dlat/2) = 0
        // lat1 === lat2
        // d = R*(2*sin^-1(min(1,sqrt(cos^2(lat1)*sin^2(dlon/2)))))
        // again, we assume we don't need to worry about the min thing, since
        // sqrt will be smaller
        // sqrt(cos^2(lat1)*sin^2(dlon/2)) = sin(d/(2R))
        // dlon = 2*sin^-1(abs(sin(d/(2R))/cos(lat1)))
        double dlon = 2 * Math.asin(Math.abs(Math.sin(radius_d / (2 * R)) / Math.cos(lat1)));
        // dlon is now on -PI/2,PI/2 (not sure of fenceposts, documentation
        // isn't specific)
        if (dlon < 0) {
            dlon += Math.PI;
        }
        return dlon;
    }

}
