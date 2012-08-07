/**
 * 
 */
package com.browseengine.bobo.geosearch.score.impl;

import com.browseengine.bobo.geosearch.score.IComputeDistance;

/**
 * @author Ken McCracken
 *
 */
public class HaversineComputeDistance implements IComputeDistance {

    /**
     * {@inheritDoc}
     */
    @Override
    public float getDistanceInMiles(double longitudeInDegreesA, double latitudeInDegreesA, 
            double longitudeInDegreesB,
            double latitudeInDegreesB) {
        double longitudeInRadiansA = Conversions.d2r(longitudeInDegreesA);
        double latitudeInRadiansA = Conversions.d2r(latitudeInDegreesA);
        double longitudeInRadiansB = Conversions.d2r(longitudeInDegreesB);
        double latitudeInRadiansB = Conversions.d2r(latitudeInDegreesB);
        
        return HaversineFormula.computeHaversineDistanceMiles(longitudeInRadiansA, 
                latitudeInRadiansA, longitudeInRadiansB, latitudeInRadiansB);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLatBoundary(float radiusInMiles) {
        double latBoundaryRadians = HaversineFormula.computeLatBoundary(radiusInMiles);
        
        return Conversions.r2d(latBoundaryRadians);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeLonBoundary(double latitudeInDegrees, 
            float radiusInMiles) {
        double latitudeInRadians = Conversions.d2r(latitudeInDegrees);
        double lonBoundaryRadians = HaversineFormula.computeLonBoundary(latitudeInRadians, 
                radiusInMiles);
        
        return Conversions.r2d(lonBoundaryRadians);
    }
}
