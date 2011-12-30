package com.browseengine.bobo.geosearch.solo.index.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockObtainFailedException;

import com.browseengine.bobo.geosearch.GeoVersion;
import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianCoordinateUUID;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinate;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinateField;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentWriter;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;
import com.browseengine.bobo.geosearch.solo.impl.IDGeoRecordComparator;
import com.browseengine.bobo.geosearch.solo.impl.IDGeoRecordSerializer;

/**
 *
 * This class is NOT currently thread-safe.  
 * 
 * @author gcooney
 */
public class GeoOnlyIndexer {
    //TODO:  This should be moved to be part of the GeoSearchConfig
    public static final int ID_BYTE_COUNT = 16;
    
    GeoSearchConfig config;
    Directory directory;
    String indexName;
    Lock lock;
    
    IDGeoRecordComparator geoComparator = new IDGeoRecordComparator();
    IDGeoRecordSerializer geoRecordSerializer = new IDGeoRecordSerializer(); 
    TreeSet<IDGeoRecord> inMemoryIndex =  new TreeSet<IDGeoRecord>(new IDGeoRecordComparator());
    List<IDGeoRecord> newRecords = new LinkedList<IDGeoRecord>();

    public GeoOnlyIndexer(GeoSearchConfig config, Directory directory, String indexName) throws IOException {
        this.config = config;
        this.directory = directory;
        this.indexName = indexName;
        
        lock = directory.makeLock(indexName);
        if (!lock.obtain()) {
            throw new LockObtainFailedException("Index locked for write: " + indexName);
        }
    }
    
    public void index(byte[] uuid, GeoCoordinateField field) {
        IGeoConverter converter = config.getGeoConverter();
        
        GeoCoordinate geoCoordinate = field.getGeoCoordinate();
        CartesianCoordinateUUID cartesianCoordinate = converter.toCartesianCoordinate(
                geoCoordinate.getLatitude(), geoCoordinate.getLongitude(), uuid);
        IDGeoRecord geoRecord = converter.toIDGeoRecord(cartesianCoordinate);
        newRecords.add(geoRecord);
    }

    public void flush() throws IOException {
        try {
            loadCurrentIndex();
            
            for (IDGeoRecord newRecord: newRecords) {
                inMemoryIndex.add(newRecord);
            }
            
            flushInMemoryIndex();
        } finally {
            lock.release();
        }
    }

    private void flushInMemoryIndex() throws IOException {
        GeoSegmentWriter<IDGeoRecord> segmentWriter = getGeoSegmentWriter(inMemoryIndex);
        
        segmentWriter.close();
    }
    
    GeoSegmentWriter<IDGeoRecord> getGeoSegmentWriter(Set<IDGeoRecord> dataToFlush) throws IOException {
        String fileName = indexName + "." + config.getGeoFileExtension();
        
        return new GeoSegmentWriter<IDGeoRecord>(
                dataToFlush, directory, fileName, 
                buildGeoSegmentInfo(indexName), geoRecordSerializer);
    }

    private void loadCurrentIndex() throws IOException {
        inMemoryIndex.clear();
        
        GeoSegmentReader<IDGeoRecord> currentIndex = getGeoSegmentReader();
        try {
            Iterator<IDGeoRecord> currentIndexIterator = 
                currentIndex.getIterator(IDGeoRecord.MIN_VALID_GEORECORD, IDGeoRecord.MAX_VALID_GEORECORD);
            
            while (currentIndexIterator.hasNext()) {
                IDGeoRecord geoRecord = currentIndexIterator.next();
                inMemoryIndex.add(geoRecord);
            }
        } finally {
            currentIndex.close();
        }
    }
    
    GeoSegmentReader<IDGeoRecord> getGeoSegmentReader() throws IOException {
        String fileName = indexName + "." + config.getGeoFileExtension();
        
        return new GeoSegmentReader<IDGeoRecord>(
                directory, fileName, -1, 
                config.getBufferSizePerGeoSegmentReader(), geoRecordSerializer, 
                geoComparator); 
    }
    
    private GeoSegmentInfo buildGeoSegmentInfo(String segmentName) throws IOException {
        IGeoConverter converter = config.getGeoConverter();
        
        //write version
        GeoSegmentInfo info = new GeoSegmentInfo();
        info.setGeoVersion(GeoVersion.CURRENT_GEOONLY_VERSION);

        info.setSegmentName(segmentName);
        
        info.setBytesPerRecord(IDGeoRecordSerializer.INTERLACE_BYTES + ID_BYTE_COUNT);
        
        //now write field -> filterByte mapping info
        IFieldNameFilterConverter fieldNameFilterConverter = converter.makeFieldNameFilterConverter();
        if (fieldNameFilterConverter != null) {
            info.setFieldNameFilterConverter(fieldNameFilterConverter);
        }
        
        return info;
    }
}
