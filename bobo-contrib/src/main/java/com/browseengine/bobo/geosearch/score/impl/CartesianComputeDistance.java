package com.browseengine.bobo.geosearch.score.impl;

/**
 * 
 * @author gcooney
 * @author shandets
 *
 */
public class CartesianComputeDistance {
    
    public static double computeDistanceSquared(int x1, int y1, int z1, 
            int x2, int y2, int z2) {
        double xDiff = ((double)x1 - (double)x2);
        double yDiff = ((double)y1 - (double)y2);
        double zDiff = ((double)z1 - (double)z2);
        
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
    }
    
    public static double computeDistance(int x1, int y1, int z1, 
            int x2, int y2, int z2) {
        return Math.sqrt(computeDistanceSquared(x1, y1, z1, x2, y2, z2));
    }
    
}
