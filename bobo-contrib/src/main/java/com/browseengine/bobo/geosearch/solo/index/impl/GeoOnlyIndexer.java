package com.browseengine.bobo.geosearch.solo.index.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinate;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinateField;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentWriter;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;
import com.browseengine.bobo.geosearch.solo.bo.IndexTooLargeException;
import com.browseengine.bobo.geosearch.solo.impl.IDGeoRecordComparator;
import com.browseengine.bobo.geosearch.solo.impl.IDGeoRecordSerializer;

/**
 *
 * This class is NOT currently thread-safe.  
 * 
 * @author gcooney
 */
public class GeoOnlyIndexer {
    GeoSearchConfig config;
    Directory directory;
    String indexName;
    Lock lock;
    
    IDGeoRecordComparator geoComparator = new IDGeoRecordComparator();
    IDGeoRecordSerializer geoRecordSerializer = new IDGeoRecordSerializer(); 
    TreeSet<IDGeoRecord> inMemoryIndex =  new TreeSet<IDGeoRecord>(new IDGeoRecordComparator());
    List<IDGeoRecord> newRecords = new LinkedList<IDGeoRecord>();
    Set<byte[]> removedRecords = new HashSet<byte[]>();

    public GeoOnlyIndexer(GeoSearchConfig config, Directory directory, String indexName) throws IOException {
        this.config = config;
        this.directory = directory;
        this.indexName = indexName;
        
        lock = directory.makeLock(indexName);
        if (!lock.obtain()) {
            throw new LockObtainFailedException("Index locked for write: " + indexName);
        }
    }
    
    /**
     * Adds a record to the geo only index
     * @param uuid
     * @param field
     */
    public void index(byte[] uuid, GeoCoordinateField field) {
        if (uuid.length != config.getBytesForId()) {
            throw new IllegalArgumentException("invalid uuid length: " + uuid.length
                    + ".  Expected uuid to be of length "
                    + config.getBytesForId() + ".");
        }
        
        IGeoConverter converter = config.getGeoConverter();
        
        GeoCoordinate geoCoordinate = field.getGeoCoordinate();
        IDGeoRecord geoRecord = converter.toIDGeoRecord(
                geoCoordinate.getLatitude(), geoCoordinate.getLongitude(), uuid);
        newRecords.add(geoRecord);
    }

    /**
     * Deletes all GeoRecords in the index that have a matching uuid.  Note that this 
     * does not delete any records indexed but not yet flushed.  
     * @param uuid
     */
    public void delete(byte[] uuid) {
        removedRecords.add(uuid);
    }
    
    /**
     * Flushes any requested index updates to directory
     * 
     * @throws IOException
     * @throws IndexTooLargeException If the size of the index is larger than the maximum size
     */
    public void flush() throws IOException, IndexTooLargeException {
        loadCurrentIndex();
        
        for (IDGeoRecord newRecord: newRecords) {
            inMemoryIndex.add(newRecord);
        }
        
        if (inMemoryIndex.size() > config.getMaxIndexSize()) {
            throw new IndexTooLargeException(indexName, inMemoryIndex.size(),
                    config.getMaxIndexSize());
        }
        
        flushInMemoryIndex();
    }

    /**
     * Closes this geoIndexer and releases any locks held
     * @throws IOException
     */
    public void close() throws IOException {
        lock.release();
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
        
        String fileName = indexName + "." + config.getGeoFileExtension();
        if (directory.fileExists(fileName)) {
            GeoSegmentReader<IDGeoRecord> currentIndex = getGeoSegmentReader();
            try {
                Iterator<IDGeoRecord> currentIndexIterator = 
                    currentIndex.getIterator(IDGeoRecord.MIN_VALID_GEORECORD, IDGeoRecord.MAX_VALID_GEORECORD);
                
                while (currentIndexIterator.hasNext()) {
                    IDGeoRecord geoRecord = currentIndexIterator.next();
                    
                    if (!isDeleted(geoRecord.id)) {
                        inMemoryIndex.add(geoRecord);
                    }
                }
            } finally {
                currentIndex.close();
            }
        }
    }
    
    private boolean isDeleted(byte[] uuid) {
        for (byte[] deletedUUID : removedRecords) {
            if (Arrays.equals(uuid, deletedUUID)) {
                return true;
            }
        }
        
        return false;
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
        
        info.setBytesPerRecord(IDGeoRecordSerializer.INTERLACE_BYTES + config.getBytesForId());
        
        //now write field -> filterByte mapping info
        IFieldNameFilterConverter fieldNameFilterConverter = converter.makeFieldNameFilterConverter();
        if (fieldNameFilterConverter != null) {
            info.setFieldNameFilterConverter(fieldNameFilterConverter);
        }
        
        return info;
    }
}
