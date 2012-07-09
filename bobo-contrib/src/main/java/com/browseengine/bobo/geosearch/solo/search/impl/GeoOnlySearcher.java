package com.browseengine.bobo.geosearch.solo.search.impl;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.store.Directory;

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianCoordinateUUID;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;
import com.browseengine.bobo.geosearch.query.GeoQuery;
import com.browseengine.bobo.geosearch.score.impl.CartesianComputeDistance;
import com.browseengine.bobo.geosearch.score.impl.Conversions;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;
import com.browseengine.bobo.geosearch.solo.impl.IDGeoRecordComparator;
import com.browseengine.bobo.geosearch.solo.impl.IDGeoRecordSerializer;

/**
 * 
 * @author gcooney
 *
 */
public class GeoOnlySearcher {
    
    public static final byte[] EMPTY_UUID = new byte[0];
    GeoSearchConfig config;
    Directory directory;
    String indexName;
    
    IGeoConverter geoConverter;
    IDGeoRecordComparator geoComparator = new IDGeoRecordComparator();
    IDGeoRecordSerializer geoRecordSerializer = new IDGeoRecordSerializer();
    
    public GeoOnlySearcher(GeoSearchConfig config, Directory directory, String indexName) {
        this.config = config;
        this.directory = directory;
        this.indexName = indexName;
        
        geoConverter = new GeoConverter();
    }
    
    public GeoOnlyHits search(GeoQuery query, int start, int count) throws IOException {
        CartesianCoordinateUUID centroidCoordinate = geoConverter.toCartesianCoordinate(query.getCentroidLatitude(), query.getCentroidLongitude(), EMPTY_UUID);
        
        CartesianCoordinateUUID minCoordinate = buildMinCoordinate(centroidCoordinate, query.getRangeInKm());
        CartesianCoordinateUUID maxCoordinate = buildMaxCoordinate(centroidCoordinate, query.getRangeInKm());

        GeoSegmentReader<IDGeoRecord> segmentReader = getGeoSegmentReader();

        IDGeoRecord minRecord = geoConverter.toIDGeoRecord(minCoordinate);
        IDGeoRecord maxRecord = geoConverter.toIDGeoRecord(maxCoordinate);
        Iterator<IDGeoRecord> hitIterator = segmentReader.getIterator(minRecord, maxRecord);
        
        return collectHits(centroidCoordinate, hitIterator, minCoordinate, maxCoordinate, start, count);
    }

    GeoSegmentReader<IDGeoRecord> getGeoSegmentReader() throws IOException {
        String fileName = indexName + "." + config.getGeoFileExtension();
        GeoSegmentReader<IDGeoRecord> segmentReader = new GeoSegmentReader<IDGeoRecord>(
                directory, fileName, Integer.MAX_VALUE, config.getBufferSizePerGeoSegmentReader(), 
                geoRecordSerializer, geoComparator);
        return segmentReader;
    }
    
    private GeoOnlyHits collectHits(CartesianCoordinateUUID centroidCoordinate, Iterator<IDGeoRecord> hitIterator, 
            CartesianCoordinateUUID minCoordinate, CartesianCoordinateUUID maxCoordinate,
            int start, int count) {
        
        int totalHits = 0;
        GeoOnlyHitQueue hitQueue = new GeoOnlyHitQueue(start+count);
        while (hitIterator.hasNext()) {
            IDGeoRecord record = hitIterator.next();
            CartesianCoordinateUUID hitCoordinate = geoConverter.toCartesianCoordinate(record);
            if (minCoordinate.x <= hitCoordinate.x && hitCoordinate.x <= maxCoordinate.x 
                    && minCoordinate.y <= hitCoordinate.y && hitCoordinate.y <= maxCoordinate.y 
                    && minCoordinate.z <= hitCoordinate.z && hitCoordinate.z <= maxCoordinate.z
                    ) {
                totalHits++;
                
                double score = CartesianComputeDistance.computeDistanceSquared(
                        hitCoordinate.x, hitCoordinate.y, hitCoordinate.z, 
                        centroidCoordinate.x, centroidCoordinate.y, centroidCoordinate.z);
                GeoOnlyHit geoHit = new GeoOnlyHit(score, hitCoordinate.uuid);
                hitQueue.insertWithOverflow(geoHit);
            }
        }
        
        int inRangeHits =  count;
        if (inRangeHits > hitQueue.size() - start) {
            inRangeHits = Math.max(0, hitQueue.size() - start);
        }
        GeoOnlyHit[] hits = new GeoOnlyHit[inRangeHits];
        for (int i = inRangeHits - 1; i >= 0; i--) {
            hits[i] = hitQueue.pop();
        }
        
        return new GeoOnlyHits(totalHits, hits);
    }
    
    private CartesianCoordinateUUID buildMinCoordinate(CartesianCoordinateUUID centroidCoordinate, float rangeInKm) {
        int rangeInUnits = Conversions.radiusMetersToIntegerUnits(rangeInKm * 1000);
        int minX = calculateMinimumCoordinate(centroidCoordinate.x, rangeInUnits);
        int minY = calculateMinimumCoordinate(centroidCoordinate.y, rangeInUnits);
        int minZ = calculateMinimumCoordinate(centroidCoordinate.z, rangeInUnits);
        return new CartesianCoordinateUUID(minX, minY, minZ, EMPTY_UUID);
    }
    public static CartesianCoordinateDocId buildMinCoordinate(float rankeInKm, int x, int y, int z, int docid) {
        int rangeInUnits = Conversions.radiusMetersToIntegerUnits(rankeInKm * 1000.0);
        int minX = calculateMinimumCoordinate(x, rangeInUnits);
        int minY = calculateMinimumCoordinate(y, rangeInUnits);
        int minZ = calculateMinimumCoordinate(z, rangeInUnits);
        return new CartesianCoordinateDocId(minX, minY, minZ, docid);
    }
    public static CartesianCoordinateDocId buildMaxCoordinate(float rankeInKm, int x, int y, int z, int docid) {
        int rangeInUnits = Conversions.radiusMetersToIntegerUnits(rankeInKm * 1000.0);
        int maxX = calculateMaximumCoordinate(x, rangeInUnits);
        int maxY = calculateMaximumCoordinate(y, rangeInUnits);
        int maxZ = calculateMaximumCoordinate(z, rangeInUnits);
        return new CartesianCoordinateDocId(maxX, maxY, maxZ, docid);
    }
    
    private CartesianCoordinateUUID buildMaxCoordinate(CartesianCoordinateUUID centroidCoordinate, float rangeInKm){
        int rangeInUnits = Conversions.radiusMetersToIntegerUnits(rangeInKm * 1000);
        int maxX = calculateMaximumCoordinate(centroidCoordinate.x, rangeInUnits);
        int maxY = calculateMaximumCoordinate(centroidCoordinate.y, rangeInUnits);
        int maxZ = calculateMaximumCoordinate(centroidCoordinate.z, rangeInUnits);
        return new CartesianCoordinateUUID(maxX, maxY, maxZ, EMPTY_UUID);
    }
    
    private static int calculateMinimumCoordinate(int originalPoint, int delta) {
        if (originalPoint > 0 || 
                originalPoint > Integer.MIN_VALUE + delta) {
            return originalPoint - delta;
        } else {
            return Integer.MIN_VALUE;
        }
    }
    
    private static int calculateMaximumCoordinate(int originalPoint, int delta) {
        if (originalPoint < 0 || 
                originalPoint < Integer.MAX_VALUE - delta) {
            return originalPoint + delta;
        } else {
            return Integer.MAX_VALUE;
        }
    }
}
