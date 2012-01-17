/**
 * 
 */
package com.browseengine.bobo.geosearch.score;

/**
 * @author Ken McCracken
 *
 */
public interface IComputeDistance {

    /**
     * Given two points A and B on the surface of the Earth, 
     * this function computes the distance between these two 
     * points, where inputs are expressed in decimal degrees longitude and 
     * decimal degrees latitude. 
     * 
     * @param longitudeInDegreesA
     * @param latitudeInDegreesA
     * @param longitudeInDegreesB
     * @param latitudeInDegreesB
     * @return the distance between A and B, in miles
     */
    float getDistanceInMiles(double longitudeInDegreesA, double latitudeInDegreesA,
            double longitudeInDegreesB, double latitudeInDegreesB);

    /**
     * The delta in the latitudinal dimension in degrees, to 
     * go radiusInMiles miles from the point (longitudeInDegrees, latitudeInDegrees)
     * on the surface of the Earth.
     * 
     * @param longitudeInDegrees
     * @param longitudeInDegrees
     * @param radiusInMiles
     * @return the delta latitude, in decimal degrees
     */
    double computeLatBoundary(double longitudeInDegrees, double latitudeInDegrees, 
            float radiusInMiles);
    
    /**
     * The delta in the longitudinal dimension in degrees, 
     * to go radiusInMiles miles from the point (longitudeInDegrees, latitudeInDegrees) 
     * on the surface of the Earth.
     * 
     * @param longitudeInDegrees
     * @param latitudeInDegrees
     * @param radiusInMiles
     * @return
     */
    double computeLonBoundary(double longitudeInDegrees, double latitudeInDegrees,
            float radiusInMiles);
    
}
