package com.browseengine.bobo.geosearch.solo.search.impl;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.store.Directory;

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
        IDGeoRecord minRecord = buildMinRecord(query);
        IDGeoRecord maxRecord = buildMaxRecord(query);
        
        String fileName = indexName + "." + config.getGeoFileExtension();
        GeoSegmentReader<IDGeoRecord> segmentReader = new GeoSegmentReader<IDGeoRecord>(
                directory, fileName, Integer.MAX_VALUE, config.getBufferSizePerGeoSegmentReader(), 
                geoRecordSerializer, geoComparator);
        
        Iterator<IDGeoRecord> hitIterator = segmentReader.getIterator(minRecord, maxRecord);

        CartesianCoordinateUUID minCoordinate = geoConverter.toCartesianCoordinate(minRecord);
        CartesianCoordinateUUID maxCoordinate = geoConverter.toCartesianCoordinate(maxRecord);
        CartesianCoordinateUUID centroidCoordinate = geoConverter.toCartesianCoordinate(
            query.getCentroidLatitude(), query.getCentroidLongitude(), new byte[0]);
        
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
        
        for (int i = 0; i < start; i++) {
            hitQueue.pop();
        }
        
        GeoOnlyHit[] hits = new GeoOnlyHit[count];
        for (int i = 0; i < count; i++) {
            hits[i] = hitQueue.pop();
        }
        
        return new GeoOnlyHits(totalHits, hits);
    }

    private IDGeoRecord buildMinRecord(GeoQuery query){
        double minLattitude = query.getCentroidLatitude() 
            - Conversions.mi2km(query.getRangeInMiles());
        double minLongitude = query.getCentroidLongitude() 
            - Conversions.mi2km(query.getRangeInMiles());
        return geoConverter.toIDGeoRecord(minLattitude, minLongitude, new byte[0]);
    }
    
    private IDGeoRecord buildMaxRecord(GeoQuery query){
        double maxLattitude = query.getCentroidLatitude() 
            + Conversions.mi2km(query.getRangeInMiles());
        double maxLongitude = query.getCentroidLongitude() 
            + Conversions.mi2km(query.getRangeInMiles());
        return geoConverter.toIDGeoRecord(maxLattitude, maxLongitude, new byte[0]);
    }
}
