package com.browseengine.bobo.geosearch.solo.index.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import org.apache.lucene.store.Directory;

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
    GeoSearchConfig config;
    Directory directory;
    String indexName;
    
    IDGeoRecordComparator geoComparator = new IDGeoRecordComparator();
    IDGeoRecordSerializer geoRecordSerializer = new IDGeoRecordSerializer(); 
    TreeSet<IDGeoRecord> inMemoryIndex =  new TreeSet<IDGeoRecord>(new IDGeoRecordComparator());
    List<IDGeoRecord> newRecords = new LinkedList<IDGeoRecord>();

    public GeoOnlyIndexer(GeoSearchConfig config, Directory directory, String indexName) {
        this.config = config;
        this.directory = directory;
        this.indexName = indexName;
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
        loadCurrentIndex();
        
        for (IDGeoRecord newRecord: newRecords) {
            inMemoryIndex.add(newRecord);
        }
        
        flushInMemoryIndex();
    }

    private void flushInMemoryIndex() throws IOException {
        String fileName = indexName + "." + config.getGeoFileExtension();
        GeoSegmentWriter<IDGeoRecord> segmentWriter = new GeoSegmentWriter<IDGeoRecord>(
                inMemoryIndex, directory, fileName, 
                buildGeoSegmentInfo(indexName), geoRecordSerializer);
        
        segmentWriter.close();
    }

    private void loadCurrentIndex() throws IOException {
        String fileName = indexName + "." + config.getGeoFileExtension();
        GeoSegmentReader<IDGeoRecord> currentIndex = new GeoSegmentReader<IDGeoRecord>(
                directory, fileName, -1, 
                config.getBufferSizePerGeoSegmentReader(), geoRecordSerializer, 
                geoComparator); 
        
        Iterator<IDGeoRecord> currentIndexIterator = 
            currentIndex.getIterator(IDGeoRecord.MIN_VALID_GEORECORD, IDGeoRecord.MAX_VALID_GEORECORD);
        
        while (currentIndexIterator.hasNext()) {
            IDGeoRecord geoRecord = currentIndexIterator.next();
            inMemoryIndex.add(geoRecord);
        }
    }
    
    private GeoSegmentInfo buildGeoSegmentInfo(String segmentName) throws IOException {
        IGeoConverter converter = config.getGeoConverter();
        
        //write version
        GeoSegmentInfo info = new GeoSegmentInfo();
        info.setGeoVersion(GeoVersion.CURRENT_GEOONLY_VERSION);

        info.setSegmentName(segmentName);
        
        //now write field -> filterByte mapping info
        IFieldNameFilterConverter fieldNameFilterConverter = converter.makeFieldNameFilterConverter();
        if (fieldNameFilterConverter != null) {
            info.setFieldNameFilterConverter(fieldNameFilterConverter);
        }
        
        return info;
    }
}
