package com.browseengine.bobo.geosearch.solo.index.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinate;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinateField;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentWriter;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;

/**
 * 
 * @author gcooney
 *
 */
@IfProfileValue(name = "test-suite", values = { "unit", "all" })
public class GeoOnlyIndexerTest {
    Mockery context = new Mockery() {{ 
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
    GeoOnlyIndexer indexer;
    private GeoSearchConfig config;
    private Directory directory;
    private String indexName;
    
    private GeoSegmentReader<IDGeoRecord> mockGeoSegmentReader;
    private GeoSegmentWriter<IDGeoRecord> mockGeoSegmentWriter;
    private IGeoConverter geoConverter;
    
    Set<IDGeoRecord> lastDataFlushed = null;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws IOException {
        indexName = UUID.randomUUID().toString();
        config = new GeoSearchConfig();
        directory = new RAMDirectory();
        
        mockGeoSegmentReader = context.mock(GeoSegmentReader.class);
        mockGeoSegmentWriter = context.mock(GeoSegmentWriter.class);
        
        geoConverter = new GeoConverter();
        
        indexer = new GeoOnlyIndexer(config, directory, indexName) {
            @Override
            GeoSegmentWriter<IDGeoRecord> getGeoSegmentWriter(Set<IDGeoRecord> dataToFlush) {
                lastDataFlushed = dataToFlush;
                return mockGeoSegmentWriter;
            }
            
            @Override
            GeoSegmentReader<IDGeoRecord> getGeoSegmentReader() {
                return mockGeoSegmentReader;
            }
        };
    }
    
    @Test
    public void testIndex_and_flush_emptyIndex() throws IOException {
        int countEntries = 50;
        
        index_and_flush(countEntries, Collections.<IDGeoRecord>emptySet());
    }
    
    @Test
    public void testIndex_and_flush_existingIndex() throws IOException {
        int countEntries = 50;
        int idByteLength = GeoOnlyIndexer.IDByteCount;
        
        
        Set<IDGeoRecord> existingData = new HashSet<IDGeoRecord>();
        for (int i = 0; i < 50; i++) {
            byte[] uuid = generateRandomUUIDAsBytes(idByteLength);
            GeoCoordinate geoCoordinate = new GeoCoordinate(randomLattitude(), randomLongitude());
            
            existingData.add(geoConverter.toIDGeoRecord(geoConverter.toCartesianCoordinate(
                    geoCoordinate.getLatitude(), geoCoordinate.getLongitude(), uuid)));
        }
        
        index_and_flush(countEntries, Collections.<IDGeoRecord>emptySet());
    }
    
    private void index_and_flush(int countEntries, final Set<IDGeoRecord> existingData) throws IOException {
        int idByteLength = GeoOnlyIndexer.IDByteCount;
        String fieldName = "field1";
        
        for (int i = 0; i < countEntries; i++) {
            byte[] uuid = generateRandomUUIDAsBytes(idByteLength);
            GeoCoordinate geoCoordinate = new GeoCoordinate(randomLattitude(), randomLongitude());
            GeoCoordinateField field = new GeoCoordinateField(fieldName, geoCoordinate);
            
            indexer.index(uuid, field);
        }

        context.checking(new Expectations() {
            { 
                one(mockGeoSegmentReader).getIterator(with(IDGeoRecord.MIN_VALID_GEORECORD), with(IDGeoRecord.MAX_VALID_GEORECORD));
                will(returnValue(existingData.iterator()));
                
                one(mockGeoSegmentReader).close();
                
                one(mockGeoSegmentWriter).close();
            }
        });
        
        indexer.flush();
        
        context.assertIsSatisfied();
        
        assertEquals(countEntries + existingData.size(), lastDataFlushed.size());
    }
    
    private double randomLongitude() {
        return Math.random() * 360.0 - 180.0;
    }
    
    private double randomLattitude() {
        return Math.random() * 180.0 - 90.0;
    }
    
    private byte[] generateRandomUUIDAsBytes(int byteSize) {
        byte[] uuid = new byte[byteSize];
        for (int i = 0; i < byteSize; i++) {
            uuid[i] = (byte)((Math.random()*(Byte.MAX_VALUE-Byte.MIN_VALUE)) + Byte.MIN_VALUE); 
        }
        
        return uuid;
    }
}
