package com.browseengine.bobo.geosearch.impl;

import static junit.framework.Assert.assertTrue;

import java.util.Collection;
import java.util.Comparator;

import org.junit.runners.Parameterized.Parameters;

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;

/**
 * CartesianGeoRecord implementation of interlace class
 * 
 * @author gcooney
 *
 */
public class CartesianGeoRecordInterlaceTest extends InterlaceTezt<CartesianGeoRecord> {
    
    public CartesianGeoRecordInterlaceTest(DimensionSpec[] dimensionSpecs, boolean[] holdConstant) {
        super(dimensionSpecs, holdConstant);
    }

    @Parameters
    public static Collection<Object[]> data() {
        DimensionSpec[] dimensionSpecs = new DimensionSpec[4];
        
        dimensionSpecs[0] =  new DimensionSpec("x", Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
        dimensionSpecs[1] =  new DimensionSpec("y", Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
        dimensionSpecs[2] =  new DimensionSpec("z", Integer.MIN_VALUE, Integer.MAX_VALUE, 2);
        dimensionSpecs[3] =  new DimensionSpec("docid", 0, Integer.MAX_VALUE, 1);

        return buildDataFromDimensionSpecs(dimensionSpecs);
    }
    
    @Override
    protected CartesianGeoRecord buildGeoRecord(int[] values, int identifier) {
        return geoConverter.toCartesianGeoRecord(values[0], values[1], values[2], values[3], (byte)0);
    }

    @Override
    protected Comparator<CartesianGeoRecord> getGeoRecordComparator() {
        return new CartesianGeoRecordComparator();
    }

    @Override
    protected void validateGeoRecord(CartesianGeoRecord actualResult, int[] values, int identifier) {
        CartesianCoordinateDocId actualCoordinate = geoConverter.toCartesianCoordinateDocId(actualResult);

        assertTrue("x should not change: expected=" + values[0] + "; actual=" + 
                actualCoordinate.x, values[0] - actualCoordinate.x == 0);
        assertTrue("y should not change by more than 1: expected=" + values[1] + "; actual=" + 
                actualCoordinate.y, Math.abs(values[1] - actualCoordinate.y) <= 1);
        assertTrue("z should not change by more than 1: expected=" + values[2] + "; actual=" + 
                actualCoordinate.z, Math.abs(values[2] - actualCoordinate.z) <= 1);
        assertTrue("doc should not change: expected=" + values[3] + "; actual=" + 
                actualCoordinate.docid, Math.abs(values[2] - actualCoordinate.z) <= 1);
    }
}
