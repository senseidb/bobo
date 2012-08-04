package com.browseengine.bobo.geosearch.score.impl;

/**
 * 
 * @author gcooney
 * @author shandets
 *
 */
public class CartesianComputeDistance {
    
    public static float computeDistanceSquared(int x1, int y1, int z1, 
            int x2, int y2, int z2) {
        float xDiff = ((float)x1 - (float)x2);
        float yDiff = ((float)y1 - (float)y2);
        float zDiff = ((float)z1 - (float)z2);
        
        return xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
    }
    
    public static float computeDistance(int x1, int y1, int z1, 
            int x2, int y2, int z2) {
        return (float) Math.sqrt(computeDistanceSquared(x1, y1, z1, x2, y2, z2));
    }
    
}
