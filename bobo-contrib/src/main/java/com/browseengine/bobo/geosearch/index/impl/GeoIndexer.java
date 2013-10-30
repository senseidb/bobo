package com.browseengine.bobo.geosearch.index.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.util.IOUtils;

import com.browseengine.bobo.geosearch.GeoVersion;
import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.IGeoUtil;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordSerializer;
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
    IGeoRecordSerializer<CartesianGeoRecord> geoRecordSerializer;

    Set<CartesianGeoRecord> fieldTree;
    Set<String> fieldNames = new HashSet<String>();
    
    private final Object treeLock = new Object();
    
    public GeoIndexer(GeoSearchConfig config) {
        geoUtil = config.getGeoUtil();
        geoConverter = config.getGeoConverter();
        fieldTree = geoUtil.getBinaryTreeOrderedByBitMag();
        geoRecordSerializer = new CartesianGeoRecordSerializer();
        
        this.config = config;
    }
    
    @Override
    public void index(int docID, GeoCoordinateField field) {
        String fieldName = field.name();
        GeoCoordinate coordinate = field.getGeoCoordinate();
        
        LatitudeLongitudeDocId longLatDocId = new LatitudeLongitudeDocId(coordinate.getLatitude(), 
                coordinate.getLongitude(), docID);
        
        IFieldNameFilterConverter fieldNameFilterConverter = geoConverter.makeFieldNameFilterConverter();
        CartesianGeoRecord geoRecord = geoConverter.toCartesianGeoRecord(fieldNameFilterConverter, fieldName, longLatDocId);
        
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
    public void flush(SegmentWriteState state) throws IOException {
        Set<CartesianGeoRecord> treeToFlush;
        Set<String> fieldNamesToFlush;
        synchronized (treeLock) {
            fieldNamesToFlush = fieldNames;
            fieldNames = new HashSet<String>();
            
            treeToFlush = fieldTree;
            fieldTree = geoUtil.getBinaryTreeOrderedByBitMag();
        }

        String segmentName = state.segmentInfo.name;
        GeoSegmentWriter<CartesianGeoRecord> geoRecordBTree = null;
        
        GeoSegmentInfo geoSegmentInfo = buildGeoSegmentInfo(fieldNamesToFlush, segmentName);
        
        boolean success = false;
        try {
            String fileName = config.getGeoFileName(segmentName);
            try {
                geoRecordBTree = new GeoSegmentWriter<CartesianGeoRecord>(treeToFlush, state.directory, 
                        state.context, fileName, geoSegmentInfo, geoRecordSerializer);
            } catch (InvalidTreeSizeException e) {
                throw new IOException(e);
            }
            
            success = true;
        } finally {
            // see https://issues.apache.org/jira/browse/LUCENE-3405
            if (success) {
                IOUtils.close(geoRecordBTree);
            } else {
                IOUtils.closeWhileHandlingException(geoRecordBTree);
            }
        }
    }

    private GeoSegmentInfo buildGeoSegmentInfo(Set<String> fieldNames, String segmentName) throws IOException {
        //write version
        GeoSegmentInfo info = new GeoSegmentInfo();
        info.setGeoVersion(GeoVersion.CURRENT_VERSION);

        info.setSegmentName(segmentName);
        
        //now write field -> filterByte mapping info
        IFieldNameFilterConverter fieldNameFilterConverter = geoConverter.makeFieldNameFilterConverter();
        if (fieldNameFilterConverter != null) {
            info.setFieldNameFilterConverter(fieldNameFilterConverter);
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
