package com.browseengine.bobo.geosearch.score.impl;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.score.impl.HaversineComputeDistance;

public class HaversineComputeDistanceTest {
    
    /**
     * The best a spherical approximation can be is within 0.5% of that of an ellipsoid approximation of the Earth.
     * Ellipsoid approximation derived from.
     * http://www.movable-type.co.uk/scripts/latlong-vincenty.html
     * 
     * I am still somewhat concerned that there approximations can be 1000's of meters off for long distances.
     * 
     * 
     */

    private static final double METERS_PER_MILE = 1609.344;
    @Test
    public void test_Test() {
        LatitudeLongitudeDocId one, two;
        HaversineComputeDistance hsc = new HaversineComputeDistance();
        double ellipsoid, sphere;
        for(int i = 0; i < 100; i++) {
            one = getNewRandomLongitudeLatitudeDocId();
            two = getNewRandomLongitudeLatitudeDocId();
            ellipsoid = distVincenty(one.latitude, one.longitude, two.latitude, two.longitude);
            sphere = hsc.getDistanceInMiles(one.longitude, one.latitude, two.longitude
                    , two.latitude) * METERS_PER_MILE;
            assertTrue("HSC does not meet the accuracy required of a good spherical distance approximation. ",
                    (0.995*ellipsoid <= sphere && sphere <= 1.005*ellipsoid));
        }
    }
    
    public void printOut(double ellipsoid, double sphere) {
        sphere *= METERS_PER_MILE;
        if (0.99*ellipsoid <= sphere && sphere <= 1.01*ellipsoid) {
            System.err.println("The sperical rerpesentation is IN range, sphere = "
                    + sphere + ", the ellipsoid = " + ellipsoid + 
                    " and the differences is" + Math.abs(sphere-ellipsoid)); 
        } else {
            System.err.println("The sperical rerpesentation is NOT IN range, sphere = "
                    + sphere + ", the ellipsoid = " + ellipsoid + 
                    " and the differences is" + Math.abs(sphere-ellipsoid)); 
        }
    }
    
    public LatitudeLongitudeDocId getNewRandomLongitudeLatitudeDocId() {
        return new LatitudeLongitudeDocId(
                Math.random() * 140.0 - 70.0, 
                Math.random() * 360.0 - 180.0, 
                0);
    }
    
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */
    /* Vincenty Inverse Solution of Geodesics on the Ellipsoid (c) Chris Veness 2002-2010             */
    /*                                                                                                */
    /* from: Vincenty inverse formula - T Vincenty, "Direct and Inverse Solutions of Geodesics on the */
    /*       Ellipsoid with application of nested equations", Survey Review, vol XXII no 176, 1975    */
    /*       http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf                                             */
    /* - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -  */

    /**
     * Calculates geodetic distance between two points specified by latitude/longitude using 
     * Vincenty inverse formula for ellipsoids
     *
     * @param   {Number} lat1, lon1: first point in decimal degrees
     * @param   {Number} lat2, lon2: second point in decimal degrees
     * @return 
     * @returns (Number} distance in metres between points
     */
    private double distVincenty(double lat1, double lon1, double lat2, double lon2) {
      double a = 6378137, b = 6356752.314245,  f = 1/298.257223563;  // WGS-84 ellipsoid params
      double L = Math.toRadians(lon2-lon1);
      double U1 = Math.atan((1-f) * Math.tan(Math.toRadians(lat1)));
      double U2 = Math.atan((1-f) * Math.tan(Math.toRadians(lat2)));
      double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
      double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);
      
      double lambda = L, lambdaP, iterLimit = 100;
      double cosSqAlpha, sinAlpha, cosSigma, sigma, cos2SigmaM, sinSigma, sinLambda, cosLambda;
      do {
        sinLambda = Math.sin(lambda);
        cosLambda = Math.cos(lambda);
        sinSigma = Math.sqrt((cosU2*sinLambda) * (cosU2*sinLambda) + 
          (cosU1*sinU2-sinU1*cosU2*cosLambda) * (cosU1*sinU2-sinU1*cosU2*cosLambda));
        if (sinSigma==0) return 0;  // co-incident points
        cosSigma = sinU1*sinU2 + cosU1*cosU2*cosLambda;
        sigma = Math.atan2(sinSigma, cosSigma);
        sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
        cosSqAlpha = 1 - sinAlpha*sinAlpha;
        cos2SigmaM = cosSigma - 2*sinU1*sinU2/cosSqAlpha;
        //if (isNaN(cos2SigmaM)) cos2SigmaM = 0;  // equatorial line: cosSqAlpha=0 (§6)
        double C = f/16*cosSqAlpha*(4+f*(4-3*cosSqAlpha));
        lambdaP = lambda;
        lambda = L + (1-C) * f * sinAlpha *
          (sigma + C*sinSigma*(cos2SigmaM+C*cosSigma*(-1+2*cos2SigmaM*cos2SigmaM)));
      } while (Math.abs(lambda-lambdaP) > 1e-12 && --iterLimit>0);

     

      double uSq = cosSqAlpha * (a*a - b*b) / (b*b);
      double A = 1 + uSq/16384*(4096+uSq*(-768+uSq*(320-175*uSq)));
      double B = uSq/1024 * (256+uSq*(-128+uSq*(74-47*uSq)));
      double deltaSigma = B*sinSigma*(cos2SigmaM+B/4*(cosSigma*(-1+2*cos2SigmaM*cos2SigmaM)-
        B/6*cos2SigmaM*(-3+4*sinSigma*sinSigma)*(-3+4*cos2SigmaM*cos2SigmaM)));
      double s = b*A*(sigma-deltaSigma);
      
    
      return s;
      
      
    }

    
}
