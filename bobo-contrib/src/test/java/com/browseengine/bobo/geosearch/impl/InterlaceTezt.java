package com.browseengine.bobo.geosearch.impl;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.browseengine.bobo.geosearch.bo.IGeoRecord;

/**
 * Provides an abstract tests class for validating GeoRecord bit-interlace functions
 * @author gcooney
 *
 * @param <T>
 */
@RunWith(Parameterized.class)
public abstract class InterlaceTezt<T extends IGeoRecord> {

    private final DimensionSpec[] dimensionSpecs;
    private final boolean[] holdConstant;
    GeoConverter geoConverter = new GeoConverter();

    public InterlaceTezt(DimensionSpec[] dimensionSpecs, boolean[] holdConstant) {
        this.dimensionSpecs = dimensionSpecs;
        this.holdConstant = holdConstant;
    }
    
    @Test
    public void testToIDGeoRecordOrder_LargeDelta() {
        int totalCoordinates = 10000;
        int start = Integer.MIN_VALUE;
        int delta = getDelta(2 * (Integer.MAX_VALUE / totalCoordinates));
        
        createRecordsAndValidateSortOrder(dimensionSpecs, start, delta, totalCoordinates);
    }
    
    @Test
    public void testToIDGeoRecordOrder_SmallDelta() {
        int totalCoordinates = 10000;
        int start = -5000;
        int delta = getDelta(1);
       
        createRecordsAndValidateSortOrder(dimensionSpecs, start, delta, totalCoordinates);
    }
    
    @Test
    public void testToIDGeoRecordOrder_TinyDelta() {
        int totalCoordinates = 10;
        int start = -5;
        int delta = getDelta(1);
        
        createRecordsAndValidateSortOrder(dimensionSpecs, start, delta, totalCoordinates);
    }

    @Test
    public void testInterlaceThenUninterlace_LargeRange() {
        int totalCoordinates = 10000;
        int start = Integer.MIN_VALUE;
        int delta = getDelta(2 * (Integer.MAX_VALUE / totalCoordinates));
        
        interlaceAndUninterlace(dimensionSpecs, start, delta, totalCoordinates);
    }
    
    @Test
    public void testInterlaceThenUninterlace_SmallRange() {
        int totalCoordinates = 100;
        int start = -50;
        int delta = getDelta(1);
        
        interlaceAndUninterlace(dimensionSpecs, start, delta, totalCoordinates);
    }
    
    @Test
    public void testInterlaceThenUninterlace_EveryPowerOfTwo_XOnly() {
        int totalCoordinates = 1;
        int start = Integer.MIN_VALUE;
        int delta = 0;
        
        for (int i=0; i < 31; i++) {
            start = 2^i;
            interlaceAndUninterlace(dimensionSpecs, start, delta, totalCoordinates);
            
            start = 2^i + Integer.MIN_VALUE;
            
            interlaceAndUninterlace(dimensionSpecs, start, delta, totalCoordinates);
        }
    }
    
    private int getDelta(int requestedDelta) {
        int delta = requestedDelta;
        
        for (int i = 0; i < dimensionSpecs.length; i++) {
            if (!holdConstant[i]) {
                delta = Math.max(delta, dimensionSpecs[i].minDelta);
            }
        }
        
        return delta;
    }
    
    private void interlaceAndUninterlace(DimensionSpec[] specs, int start, int delta,
            int totalCoordinates) {
        for (int i = 0; i < totalCoordinates; i++) {
            int[] values = buildValues(specs, start, delta, totalCoordinates, i);
            
            T geoRecord = buildGeoRecord(values, i);
            
            validateGeoRecord(geoRecord, values, i);
        }
    }

    private int[] buildValues(DimensionSpec[] specs, int start, int delta, int totalCoordinates, int currentCoordinate) {
        int[] values = new int[specs.length];
        for (int j = 0; j < specs.length; j++) {
            DimensionSpec spec = specs[j];
            if (holdConstant[j]) {
                values[j] = start + currentCoordinate * delta;
            } else {
                int specDelta = delta;
                int specStart = start;
                if (start < spec.minValue) {
                    specDelta = (int)(((double)delta * totalCoordinates + start - spec.minValue) /  totalCoordinates);
                    specStart = spec.minValue;
                }
                
                values[j] = specStart + currentCoordinate * specDelta;
            }
        }
        
        return values;
    }
    
    private void createRecordsAndValidateSortOrder(DimensionSpec[] specs, int start, int delta, int totalCoordinates) {
        ArrayList<T> list = new ArrayList<T>(totalCoordinates);
        TreeSet<T> tree = new TreeSet<T>(getGeoRecordComparator());
        
        for (int i = 0; i < totalCoordinates; i++) {
            int[] values = buildValues(specs, start, delta, totalCoordinates, i);
            
            T geoRecord =  buildGeoRecord(values, i);
            
            list.add(geoRecord);
            tree.add(geoRecord);
            
        }
        
        int i = 0;
        for (Iterator<T> geoIter = tree.iterator(); geoIter.hasNext();) {
            T treeNext = geoIter.next();
            T arrayNext = list.get(i);
            
            assertEquals("unexpected record at index " + i, arrayNext, treeNext);
            
            i++;
        }
    }
    
    protected abstract T buildGeoRecord(int[] values, int identifier);
    
    protected abstract void validateGeoRecord(T actualResult, int[] values, int identifier);
    
    protected abstract Comparator<T> getGeoRecordComparator();
    
    public static Collection<Object[]> buildDataFromDimensionSpecs(DimensionSpec[] dimensionSpecs) {
        Collection<Object[]> data = new ArrayList<Object[]>();
        
        //first build data with one dimension on at a time
        for (int i = 0; i < dimensionSpecs.length; i++) {
            boolean[] holdConstant = new boolean[dimensionSpecs.length];
            for (int j = 0; j < dimensionSpecs.length; j++) {
                holdConstant[j] =  (j != i);
            }
            
            data.add(new Object[] {dimensionSpecs, holdConstant});
        }
        
        
        //then build data with all dimensions on
        boolean[] holdConstant = new boolean[dimensionSpecs.length];
        data.add(new Object[] {dimensionSpecs, holdConstant});
        
        return data;
    }
    
    public static final class DimensionSpec {
        final String dimensionName;
        final int minValue;
        final int maxValue;
        final int minDelta;

        public DimensionSpec(String dimensionName, int minValue, int maxValue, int minDelta) {
            this.dimensionName = dimensionName;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.minDelta = minDelta;
        }
    }
}
