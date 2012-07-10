package com.browseengine.bobo.geosearch.impl;

import static junit.framework.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import org.junit.runners.Parameterized.Parameters;

import com.browseengine.bobo.geosearch.bo.CartesianCoordinateUUID;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;
import com.browseengine.bobo.geosearch.solo.impl.IDGeoRecordComparator;

public class IDGeoRecordInterlaceTest extends InterlaceTezt<IDGeoRecord> {

    public IDGeoRecordInterlaceTest(DimensionSpec[] dimensionSpecs, boolean[] holdConstant) {
        super(dimensionSpecs, holdConstant);
    }

    @Parameters
    public static Collection<Object[]> data() {
        DimensionSpec[] dimensionSpecs = new DimensionSpec[3];
        
        dimensionSpecs[0] =  new DimensionSpec("x", Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
        dimensionSpecs[1] =  new DimensionSpec("y", Integer.MIN_VALUE, Integer.MAX_VALUE, 2);
        dimensionSpecs[2] =  new DimensionSpec("z", Integer.MIN_VALUE, Integer.MAX_VALUE, 2);

        return buildDataFromDimensionSpecs(dimensionSpecs);
    }
    
    @Override
    protected IDGeoRecord buildGeoRecord(int[] values, int identifier) {
        byte[] id = Integer.toString(identifier).getBytes();
        
        return geoConverter.toIDGeoRecord(values[0], values[1], values[2], id);
    }

    @Override
    protected Comparator<IDGeoRecord> getGeoRecordComparator() {
        return new IDGeoRecordComparator();
    }

    @Override
    protected void validateGeoRecord(IDGeoRecord actualResult, int[] values, int identifier) {
        byte[] id = Integer.toString(identifier).getBytes();
        
        CartesianCoordinateUUID actualCoordinate = geoConverter.toCartesianCoordinate(actualResult);

        assertTrue("UUID should not change", Arrays.equals(id, actualCoordinate.uuid));
        assertTrue("x should not change: expected=" + values[0] + "; actual=" + 
                actualCoordinate.x, values[0] - actualCoordinate.x == 0);
        assertTrue("y should not change by more than 1: expected=" + values[1] + "; actual=" + 
                actualCoordinate.y, Math.abs(values[1] - actualCoordinate.y) <= 1);
        assertTrue("z should not change by more than 1: expected=" + values[2] + "; actual=" + 
                actualCoordinate.z, Math.abs(values[2] - actualCoordinate.z) <= 1);
    }

}
