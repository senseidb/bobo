package com.browseengine.bobo.geosearch.solo.search.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.IfProfileValue;

import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianCoordinateUUID;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;
import com.browseengine.bobo.geosearch.query.GeoQuery;
import com.browseengine.bobo.geosearch.score.impl.Conversions;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;

/**
 * 
 * @author gcooney
 *
 */
@IfProfileValue(name = "test-suite", values = { "unit", "all" })
public class GeoOnlySearcherTest {
    Mockery context = new Mockery() {{ 
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
    GeoOnlySearcher searcher;
    private GeoSearchConfig config;
    private Directory directory;
    private String indexName;
    
    private GeoSegmentReader<IDGeoRecord> mockGeoSegmentReader;
    private IGeoConverter geoConverter;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws IOException {
        indexName = UUID.randomUUID().toString();
        config = new GeoSearchConfig();
        directory = new RAMDirectory();
        
        mockGeoSegmentReader = context.mock(GeoSegmentReader.class);

        geoConverter = new GeoConverter();
        
        searcher = new GeoOnlySearcher(config, directory, indexName) {
            @Override
            GeoSegmentReader<IDGeoRecord> getGeoSegmentReader() {
                return mockGeoSegmentReader;
            }
        };
    }
    
    @Test
    public void testSearch_top1() throws IOException {
        final int start = 0; 
        final int count = 1;
        final int hitCount = 1;
        final double centroidLat = 0;
        final double centroidLong = 0;
        final float rangeInMiles = 10f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top10() throws IOException {
        final int start = 0; 
        final int count = 10;
        final int hitCount = 10;
        final double centroidLat = 0;
        final double centroidLong = 0;
        final float rangeInMiles = 10f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top10_20inRange() throws IOException {
        final int start = 0; 
        final int count = 10;
        final int hitCount = 20;
        final double centroidLat = 0;
        final double centroidLong = 0;
        final float rangeInMiles = 10f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top10_start10_20inRange() throws IOException {
        final int start = 10; 
        final int count = 10;
        final int hitCount = 20;
        final double centroidLat = 0;
        final double centroidLong = 0;
        final float rangeInMiles = 10f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top10_start30_20inRange() throws IOException {
        final int start = 30; 
        final int count = 10;
        final int hitCount = 20;
        final double centroidLat = 0;
        final double centroidLong = 0;
        final float rangeInMiles = 10f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top25_1000inRange() throws IOException {
        final int start = 0; 
        final int count = 25;
        final int hitCount = 1000;
        final double centroidLat = 0;
        final double centroidLong = 0;
        final float rangeInMiles = 20f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top10_start15_20inRange() throws IOException {
        final int start = 15; 
        final int count = 10;
        final int hitCount = 20;
        final double centroidLat = 0;
        final double centroidLong = 0;
        final float rangeInMiles = 10f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top10_90long() throws IOException {
        final int start = 0; 
        final int count = 10;
        final int hitCount = 10;
        final double centroidLat = 0;
        final double centroidLong = 90;
        final float rangeInMiles = 10f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top10_90long45lat() throws IOException {
        final int start = 0; 
        final int count = 10;
        final int hitCount = 10;
        final double centroidLat = 45;
        final double centroidLong = 90;
        final float rangeInMiles = 10f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top10_180long_range100() throws IOException {
        final int start = 0; 
        final int count = 10;
        final int hitCount = 10;
        final double centroidLat = 0;
        final double centroidLong = 180;
        final float rangeInMiles = 100f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top10_90lat_range100() throws IOException {
        final int start = 0; 
        final int count = 10;
        final int hitCount = 10;
        final double centroidLat = 90;
        final double centroidLong = 0;
        final float rangeInMiles = 100f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top10_neg90lat_range100() throws IOException {
        final int start = 0; 
        final int count = 10;
        final int hitCount = 10;
        final double centroidLat = -90;
        final double centroidLong = 0;
        final float rangeInMiles = 10f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, start, count);
    }
    
    @Test
    public void testSearch_top10_allmisses() throws IOException {
        final int start = 0; 
        final int count = 10;
        final int hitCount = 0;
        final int missCount = 1000;
        final double centroidLat = 0;
        final double centroidLong = 0;
        final float rangeInMiles = 10f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, missCount, 
                start, count);
    }
    
    @Test
    public void testSearch_top10_10justmisses() throws IOException {
        final int start = 0; 
        final int count = 20;
        final int hitCount = 10;
        final int missCount = 10;
        final double centroidLat = 0;
        final double centroidLong = 0;
        final float rangeInMiles = 10f;
     
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, missCount, 
                start, count);
    }
    
    private void searchAndVerify_allHitsInRange(double centroidLat, double centroidLong, 
            Float rangeInMiles, int hitCount, int start, int count) throws IOException {
        int missCount = 0;
        searchAndVerify_allHitsInRange(centroidLat, centroidLong, rangeInMiles, hitCount, missCount, start, count);
    }
    
    private void searchAndVerify_allHitsInRange(double centroidLat, double centroidLong, 
            Float rangeInMiles, int hitCount, int missCount, int start, int count) throws IOException {
        final GeoQuery query = new GeoQuery(centroidLong, centroidLat, rangeInMiles, null);
        
        final IDGeoRecord maxRecord = buildMaxRecord(centroidLong, centroidLat, rangeInMiles, GeoOnlySearcher.EMPTY_UUID); 
        final IDGeoRecord minRecord = buildMinRecord(centroidLong, centroidLat, rangeInMiles, GeoOnlySearcher.EMPTY_UUID);
        final List<IDGeoRecord> expectedHits = buildHitSet(centroidLong, centroidLat, rangeInMiles, hitCount);
        final List<IDGeoRecord> expectedMisses = buildJustMissSet(centroidLong, centroidLat, rangeInMiles, missCount);
        final List<IDGeoRecord> treeHits = new ArrayList<IDGeoRecord>(expectedHits);
        treeHits.addAll(expectedMisses);
        context.checking(new Expectations() {
            {
                one(mockGeoSegmentReader).getIterator(minRecord, maxRecord);
                will(returnValue(treeHits.iterator()));
            }
        });
        
        Iterator<IDGeoRecord> hitIter = expectedHits.iterator();
        for (int i = 0; i < start && hitIter.hasNext() ; i++){
            hitIter.next();
        }
        
        GeoOnlyHits hits = searcher.search(query, start, count);
        assertEquals(hitCount, hits.totalHits());
        int expectedResultsCount = Math.min(Math.max(0, hitCount - start), count);
        assertEquals(expectedResultsCount, hits.getHits().size());
        for (int index = 0; index < count && hitIter.hasNext(); index++) {
            IDGeoRecord expectedHit = hitIter.next();
            GeoOnlyHit actualHit = hits.getHits().get(index);
            assertNotNull("Expected hit at index " + index + " is null", expectedHit);
            assertNotNull("Hit at index " + index + " is null", actualHit);
            assertEquals("Hit at index " + index + " has wrong id", new String(expectedHit.id), new String(actualHit.uuid));
        }
    }
    
    private IDGeoRecord buildMinRecord(double centroidLong, double centroidLat, Float rangeInMiles, byte[] uuid) {
        CartesianCoordinateUUID coordinate = buildMinCoordinate(centroidLong, centroidLat, rangeInMiles, uuid);
        
        return geoConverter.toIDGeoRecord(coordinate);
    }

    private IDGeoRecord buildMaxRecord(double centroidLong, double centroidLat, Float rangeInMiles, byte[] uuid) {
        CartesianCoordinateUUID coordinate = buildMaxCoordinate(centroidLong, centroidLat, rangeInMiles, uuid);
        
        return geoConverter.toIDGeoRecord(coordinate);
    }

    private List<IDGeoRecord> buildHitSet(double centroidLong, double centroidLat, float rangeInMiles, 
            int hitCount) {
        List<IDGeoRecord> hits = new ArrayList<IDGeoRecord>();
        
        CartesianCoordinateUUID centroidCoordinate = buildMaxCoordinate(centroidLong, centroidLat, 0, new byte[0]);
        int deltax = calculateMaxDelta(centroidCoordinate.x, rangeInMiles); 
        int deltay = calculateMaxDelta(centroidCoordinate.y, rangeInMiles);
        int deltaz = calculateMaxDelta(centroidCoordinate.z, rangeInMiles);
        
        for (int i = 0; i < hitCount; i++) {
            IDGeoRecord geoRecord;
            byte[] uuid = new byte[] {(byte)(i + "a".getBytes()[0])};
            if (i % 2 == 0) {
                CartesianCoordinateUUID coordinate = new CartesianCoordinateUUID(
                        centroidCoordinate.x + ((deltax * i) / hitCount), 
                        centroidCoordinate.y + ((deltay * i) / hitCount), 
                        centroidCoordinate.z + ((deltaz * i) / hitCount),
                        uuid); 
                
                geoRecord = geoConverter.toIDGeoRecord(coordinate); 
            } else {
                CartesianCoordinateUUID coordinate = new CartesianCoordinateUUID(
                        centroidCoordinate.x - ((deltax * i) / hitCount), 
                        centroidCoordinate.y - ((deltay * i) / hitCount), 
                        centroidCoordinate.z - ((deltaz * i) / hitCount),
                        uuid);
                
                geoRecord = geoConverter.toIDGeoRecord(coordinate);
            }
            hits.add(geoRecord);
        }
        
        return hits;
    }
    
    private List<IDGeoRecord> buildJustMissSet(double centroidLong, double centroidLat, float rangeInMiles, 
            int missCount) {
        List<IDGeoRecord> hits = new ArrayList<IDGeoRecord>();
        
        CartesianCoordinateUUID centroidCoordinate = buildMaxCoordinate(centroidLong, centroidLat, 0, new byte[0]);
        int deltax = calculateMaxDelta(centroidCoordinate.x, rangeInMiles); 
        int deltay = calculateMaxDelta(centroidCoordinate.y, rangeInMiles);
        int deltaz = calculateMaxDelta(centroidCoordinate.z, rangeInMiles);
        
        for (int i = 0; i < missCount; i++) {
            IDGeoRecord geoRecord;
            byte[] uuid = new byte[] {(byte)(i + "a".getBytes()[0])};
            if (i % 2 == 0) {
                CartesianCoordinateUUID coordinate = new CartesianCoordinateUUID(
                        centroidCoordinate.x + deltax + (i + 1) * 2, 
                        centroidCoordinate.y + deltay + (i + 1) * 2, 
                        centroidCoordinate.z + deltaz + (i + 1) * 2,
                        uuid);
                
                geoRecord = geoConverter.toIDGeoRecord(coordinate);
            } else {
                CartesianCoordinateUUID coordinate = new CartesianCoordinateUUID(
                        centroidCoordinate.x - deltax - (i + 1) * 2, 
                        centroidCoordinate.y - deltay - (i + 1) * 2, 
                        centroidCoordinate.z - deltaz - (i + 1) * 2,
                        uuid);
                
                geoRecord = geoConverter.toIDGeoRecord(coordinate);
            }
            hits.add(geoRecord);
        }
        
        return hits;
    }

    private int calculateMaxDelta(int originalPoint, float rangeInMiles) {
        float rangeInKm = Conversions.mi2km(rangeInMiles);
        int rangeInUnits = Conversions.radiusMetersToIntegerUnits(rangeInKm * 1000);
        int max = calculateMaximumCoordinate(originalPoint, rangeInUnits);
        int min = calculateMinimumCoordinate(originalPoint, rangeInUnits);
        return Math.min(max - originalPoint, originalPoint - min);
    }
    
    private CartesianCoordinateUUID buildMinCoordinate(double centroidLong, double centroidLat, float rangeInMiles, 
            byte[] uuid) {
        double rangeInkm = Conversions.mi2km(rangeInMiles);
        int rangeInUnits = Conversions.radiusMetersToIntegerUnits(rangeInkm * 1000);
        CartesianCoordinateUUID centroidCoordinate = geoConverter.toCartesianCoordinate(
                centroidLat, centroidLong, uuid);
        
        int minX = calculateMinimumCoordinate(centroidCoordinate.x, rangeInUnits);
        int minY = calculateMinimumCoordinate(centroidCoordinate.y, rangeInUnits);
        int minZ = calculateMinimumCoordinate(centroidCoordinate.z, rangeInUnits);
        CartesianCoordinateUUID minCoordinate = 
            new CartesianCoordinateUUID(minX, minY, minZ, uuid);
        
        return minCoordinate;
    }
    
    private CartesianCoordinateUUID buildMaxCoordinate(double centroidLong, double centroidLat, float rangeInMiles, 
            byte[] uuid) {
        double rangeInkm = Conversions.mi2km(rangeInMiles);
        int rangeInUnits = Conversions.radiusMetersToIntegerUnits(rangeInkm * 1000);
        CartesianCoordinateUUID centroidCoordinate = geoConverter.toCartesianCoordinate(
                centroidLat, centroidLong, GeoOnlySearcher.EMPTY_UUID);
        
        int maxX = calculateMaximumCoordinate(centroidCoordinate.x, rangeInUnits);
        int maxY = calculateMaximumCoordinate(centroidCoordinate.y, rangeInUnits);
        int maxZ = calculateMaximumCoordinate(centroidCoordinate.z, rangeInUnits);
        CartesianCoordinateUUID maxCoordinate = 
            new CartesianCoordinateUUID(maxX, maxY, maxZ, uuid);
        
        return maxCoordinate;
    }
    
    private int calculateMinimumCoordinate(int originalPoint, int delta) {
        if (originalPoint > 0 || 
                originalPoint > Integer.MIN_VALUE + delta) {
            return originalPoint - delta;
        } else {
            return Integer.MIN_VALUE;
        }
    }
    
    private int calculateMaximumCoordinate(int originalPoint, int delta) {
        if (originalPoint < 0 || 
                originalPoint < Integer.MAX_VALUE - delta) {
            return originalPoint + delta;
        } else {
            return Integer.MAX_VALUE;
        }
    }
    
}
