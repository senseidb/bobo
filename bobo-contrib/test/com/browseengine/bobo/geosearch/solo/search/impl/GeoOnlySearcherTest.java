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
    
    private void searchAndVerify_allHitsInRange(double centroidLat, double centroidLong, 
            Float rangeInMiles, int hitCount, int start, int count) throws IOException {
        final GeoQuery query = new GeoQuery(centroidLat, centroidLong, rangeInMiles, null);
        
        final IDGeoRecord maxRecord = buildMaxRecord(centroidLong, centroidLat, rangeInMiles, GeoOnlySearcher.EMPTY_UUID); 
        final IDGeoRecord minRecord = buildMinRecord(centroidLong, centroidLat, rangeInMiles, GeoOnlySearcher.EMPTY_UUID);
        final List<IDGeoRecord> expectedHits = buildHitSet(centroidLong, centroidLat, rangeInMiles, hitCount);
        context.checking(new Expectations() {
            {
                one(mockGeoSegmentReader).getIterator(minRecord, maxRecord);
                will(returnValue(expectedHits.iterator()));
            }
        });
        
        GeoOnlyHits hits = searcher.search(query, start, count);
        assertEquals(hitCount, hits.totalHits());
        assertEquals(count, hits.getHits().size());
        int index = 0;
        for (Iterator<IDGeoRecord> hitIter = expectedHits.iterator(); hitIter.hasNext();) {
            IDGeoRecord expectedHit = hitIter.next();
            GeoOnlyHit actualHit = hits.getHits().get(index);
            assertNotNull("Expected hit at index " + index + " is null", expectedHit);
            assertNotNull("Hit at index " + index + " is null", actualHit);
            assertEquals("Hit at index " + index + " has wrong id", new String(expectedHit.id), new String(actualHit.uuid));
            index++;
        }
    }
    
    private List<IDGeoRecord> buildHitSet(double centroidLong, double centroidLat, float rangeInMiles, 
            int hitCount) {
        List<IDGeoRecord> hits = new ArrayList<IDGeoRecord>();
        
        for (int i = 0; i < hitCount; i++) {
            IDGeoRecord geoRecord = buildMaxRecord(centroidLong, centroidLat, 
                    (rangeInMiles * i) / hitCount, new byte[] {(byte)(i + "a".getBytes()[0])});
            hits.add(geoRecord);
        }
        
        return hits;
    }

//    private Iterator<IDGeoRecord> buildHitIterator(IDGeoRecord minRecord, IDGeoRecord maxRecord, 
//            int hitCount) {
//        Set<IDGeoRecord> hits = new HashSet<IDGeoRecord>();
//        
//        for (int i = 0; i < hitCount; i++) {
//            double highOrderTotalDiff = (double)maxRecord.highOrder - (double)minRecord.highOrder;
//            double lowOrderTotalDiff = (double)maxRecord.lowOrder - (double)minRecord.lowOrder;
//            if (highOrderDiff = 0 && lowOrderDiff = )
//            
//            double highOrderDiff = (highOrderTotalDiff * i) / hitCount;
//            double lowOrderDiff =  (lowOrderTotalDiff * i) / hitCount;
//            
//            IDGeoRecord geoRecord = new IDGeoRecord(minRecord.highOrder + highOrderDiff, 
//                    minRecord.lowOrder + lowOrderDiff, minRecord.id);
//        }
//        
//        return hits.iterator();
//    }
    
    private IDGeoRecord buildMinRecord(double centroidLong, double centroidLat, float rangeInMiles, 
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
        
        return geoConverter.toIDGeoRecord(minCoordinate);
    }
    
    private IDGeoRecord buildMaxRecord(double centroidLong, double centroidLat, float rangeInMiles, 
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
        
        return geoConverter.toIDGeoRecord(maxCoordinate);
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
            return Integer.MIN_VALUE;
        }
    }
    
}
