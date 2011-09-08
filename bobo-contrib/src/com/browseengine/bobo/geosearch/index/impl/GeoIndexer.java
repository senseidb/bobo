    package com.browseengine.bobo.geosearch.index.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.store.Directory;
import org.apache.lucene.util.IOUtils;

import com.browseengine.bobo.geosearch.GeoVersion;
import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.IGeoUtil;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.index.IGeoIndexer;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinate;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinateField;

/**
 * 
 * @author Geoff Cooney
 *
 */
public class GeoIndexer implements IGeoIndexer {

    IGeoUtil geoUtil;
    IGeoConverter geoConverter;
    GeoSearchConfig config;

    Set<GeoRecord> fieldTree;
    Set<String> fieldNames = new HashSet<String>();
    
    private final Object treeLock = new Object();
    
    public GeoIndexer(GeoSearchConfig config) {
        geoUtil = config.getGeoUtil();
        geoConverter = config.getGeoConverter();
        fieldTree = geoUtil.getBinaryTreeOrderedByBitMag();
        
        this.config = config;
    }
    
    @Override
    public void index(int docID, GeoCoordinateField field) {
        String fieldName = field.name();
        GeoCoordinate coordinate = field.getGeoCoordinate();
        
        LatitudeLongitudeDocId longLatDocId = new LatitudeLongitudeDocId(coordinate.getLatitude(), 
                coordinate.getLongitude(), docID);
        
        GeoRecord geoRecord = geoConverter.toGeoRecord(fieldName, longLatDocId);
        
        //For now, we need to synchronize this since we can only safely have one thread at a
        //time adding an item to a treeset.  One alternative strategy is to add geoRecords to
        //an object with better concurrency while indexing and then sort using the TreeSet on 
        //flush
        synchronized (treeLock) {
            fieldTree.add(geoRecord);
            fieldNames.add(fieldName);
        }
        
        return;
    }

    @Override
    public void flush(Directory directory, String segmentName) throws IOException {
        Set<GeoRecord> treeToFlush;
        Set<String> fieldNamesToFlush;
        synchronized (treeLock) {
            fieldNamesToFlush = fieldNames;
            fieldNames = new HashSet<String>();
            
            treeToFlush = fieldTree;
            fieldTree = geoUtil.getBinaryTreeOrderedByBitMag();
        }
        
        GeoSegmentWriter geoRecordBTree = null;
        
        GeoSegmentInfo geoSegmentInfo = buildGeoSegmentInfo(fieldNamesToFlush, segmentName);
        
        boolean success = false;
        try {
            String fileName = config.getGeoFileName(segmentName);
            geoRecordBTree = new GeoSegmentWriter(treeToFlush, directory, fileName, geoSegmentInfo);
            
            success = true;
        } finally {
            IOUtils.closeSafely(!success, geoRecordBTree);
        }
    }

    private GeoSegmentInfo buildGeoSegmentInfo(Set<String> fieldNames, String segmentName) throws IOException {
        //write version
        GeoSegmentInfo info = new GeoSegmentInfo();
        info.setGeoVersion(GeoVersion.CURRENT_VERSION);

        info.setSegmentName(segmentName);
        
        //now write field -> filterByte mapping info
        IFieldNameFilterConverter fieldNameConverter = geoConverter.getFieldNameFilterConverter();
        if (fieldNameConverter != null) {
            info.setFieldNameFilterConverter(fieldNameConverter);
        }
        
        return info;
    }
    
    @Override
    public void abort() {
        synchronized (fieldTree) {
            fieldNames.clear();
            fieldTree.clear();
        }
    }
}
