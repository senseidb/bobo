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
    
    private final float ONEKMDIFFX =  82694f;  // 16 powers of 2 is 65,536
    private final float ONEKMDIFFY = 224679f;  // 18 powers of 2 is 262,144
    private final float ONEKMDIFFZ = 234124f;
    
    @Override 
    public int [] cartesianBoundingBox(float rangeInKm, int x, int y, int z) {
        int [] inta = new int[6];
        inta[0] = x - (int)(rangeInKm*ONEKMDIFFX);
        inta[1] = x + (int)(rangeInKm*ONEKMDIFFX);
        inta[2] = y - (int)(rangeInKm*ONEKMDIFFY);
        inta[3] = y + (int)(rangeInKm*ONEKMDIFFY);
        inta[4] = z - (int)(rangeInKm*ONEKMDIFFZ);
        inta[5] = z + (int)(rangeInKm*ONEKMDIFFZ);
        return inta;
    }

    @Override
    public long getSquaredDistance(int x, int y, int z, int xp, int yp, int zp) {
        return (x-xp)*(x-xp)+(y-yp)*(y-yp)*(z-zp)*(z-zp);
    }
}
