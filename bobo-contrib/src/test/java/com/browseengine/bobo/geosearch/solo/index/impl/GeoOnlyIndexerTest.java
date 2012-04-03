package com.browseengine.bobo.geosearch.solo.index.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
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
import com.browseengine.bobo.geosearch.solo.bo.IndexTooLargeException;

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
    
    int idByteLength = GeoSearchConfig.DEFAULT_ID_BYTE_COUNT;
    
    GeoOnlyIndexer indexer;
    private GeoSearchConfig config;
    private Directory directory;
    private String indexName;
    
    private GeoSegmentReader<IDGeoRecord> mockGeoSegmentReader;
    private GeoSegmentWriter<IDGeoRecord> mockGeoSegmentWriter;
    private IGeoConverter geoConverter;
    
    private Lock mockLock;
    
    Set<IDGeoRecord> lastDataFlushed = null;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws IOException {
        indexName = UUID.randomUUID().toString();
        config = new GeoSearchConfig();
        directory = new RAMDirectory();
        directory.setLockFactory(new LockFactory() {
            @Override
            public void clearLock(String arg0) throws IOException {
                //do nothing
            }

            @Override
            public Lock makeLock(String arg0) {
                return mockLock;
            }
        });
        
        mockGeoSegmentReader = context.mock(GeoSegmentReader.class);
        mockGeoSegmentWriter = context.mock(GeoSegmentWriter.class);
        mockLock = context.mock(Lock.class);

        expectLockObtain();
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
        
        context.assertIsSatisfied();
    }
    
    @After
    public void tearDown() {
        context.assertIsSatisfied();
    }
                            
    
    private void expectLockObtain() throws IOException {
        context.checking(new Expectations() {
            {
                one(mockLock).obtain();
                will(returnValue(true));
            }
        });
    }
    
    @Test
    public void testIndex_and_flush_emptyIndex() throws IOException, IndexTooLargeException {
        int countEntries = 50;
        
        index_and_flush(countEntries, false, Collections.<IDGeoRecord>emptySet());
    }
    
    @Test
    public void testIndex_and_flush_existingIndex() throws IOException, IndexTooLargeException {
        int countEntries = 50;
        int idByteLength = GeoSearchConfig.DEFAULT_ID_BYTE_COUNT;
        
        
        Set<IDGeoRecord> existingData = new HashSet<IDGeoRecord>();
        for (int i = 0; i < 50; i++) {
            byte[] uuid = generateRandomUUIDAsBytes(idByteLength);
            GeoCoordinate geoCoordinate = new GeoCoordinate(randomLattitude(), randomLongitude());
            
            existingData.add(geoConverter.toIDGeoRecord(
                    geoCoordinate.getLatitude(), geoCoordinate.getLongitude(), uuid));
        }
        
        createIndexFileDummyData();
        
        index_and_flush(countEntries, true, existingData);
    }
    
    @Test
    public void testIndex_and_flush_maxEntries() throws IOException, IndexTooLargeException {
        int maxEntries = 100;
        config.setMaxIndexSize(maxEntries);
        
        index_and_flush(maxEntries, false, Collections.<IDGeoRecord>emptySet());
    }
    
    @Test(expected = IndexTooLargeException.class)
    public void testIndex_and_flush_tooManyEntries() throws IOException, IndexTooLargeException {
        int maxEntries = 100;
        config.setMaxIndexSize(maxEntries);
        
        index_and_flush(maxEntries + 1, false, Collections.<IDGeoRecord>emptySet());
    }
    
    private void createIndexFileDummyData() throws IOException {
        IndexOutput output = directory.createOutput(indexName + ".geo");
        output.writeString("dummyData");
        output.flush();
    }
    
    private void index_and_flush(final int countEntries, final boolean fileExists, final Set<IDGeoRecord> existingData) throws IOException, IndexTooLargeException {
        int idByteLength = GeoSearchConfig.DEFAULT_ID_BYTE_COUNT;
        String fieldName = "field1";
        
        for (int i = 0; i < countEntries; i++) {
            byte[] uuid = generateRandomUUIDAsBytes(idByteLength);
            GeoCoordinate geoCoordinate = new GeoCoordinate(randomLattitude(), randomLongitude());
            GeoCoordinateField field = new GeoCoordinateField(fieldName, geoCoordinate);
            
            indexer.index(uuid, field);
        }

        context.checking(new Expectations() {
            { 
                if (fileExists) {
                    one(mockGeoSegmentReader).getIterator(with(IDGeoRecord.MIN_VALID_GEORECORD), with(IDGeoRecord.MAX_VALID_GEORECORD));
                    will(returnValue(existingData.iterator()));
                    
                    one(mockGeoSegmentReader).close();
                }
                
                if ((countEntries + existingData.size()) <= config.getMaxIndexSize()) {
                    one(mockGeoSegmentWriter).close();
                }
                
                one(mockLock).release();
            }
        });
        
        try {
            indexer.flush();
        } finally {
            indexer.close();
        }
        
        context.assertIsSatisfied();
        
        assertEquals(countEntries + existingData.size(), lastDataFlushed.size());
    }
    
    @Test
    public void test_delete_and_flush_emptyIndex() throws IOException, IndexTooLargeException {
        int countEntries = 10;
        
        Set<byte[]> toDelete = new HashSet<byte[]>();
        for (int i = 0; i < countEntries; i++) {
            byte[] uuid = generateRandomUUIDAsBytes(idByteLength);
            toDelete.add(uuid);
        }
        delete_and_flush(toDelete, false, Collections.<IDGeoRecord>emptySet(), 0);
    }
    
    @Test
    public void test_delete_and_flush_existingIndex() throws IOException, IndexTooLargeException {
        int countToDelete = 10;
        
        createIndexFileDummyData();
        
        Set<IDGeoRecord> existingData = new HashSet<IDGeoRecord>();
        Set<byte[]> toDelete = new HashSet<byte[]>();
        for (int i = 0; i < 50; i++) {
            byte[] uuid = generateRandomUUIDAsBytes(idByteLength);
            GeoCoordinate geoCoordinate = new GeoCoordinate(randomLattitude(), randomLongitude());
            
            existingData.add(geoConverter.toIDGeoRecord(
                    geoCoordinate.getLatitude(), geoCoordinate.getLongitude(), uuid));
            
            if (i < countToDelete) {
                toDelete.add(uuid);
            }
        }
        
        delete_and_flush(toDelete, true, existingData, 40);
    }
    
    @Test
    public void test_delete_and_flush_multipleRecords() throws IOException, IndexTooLargeException {
        int countToDelete = 5;
        
        createIndexFileDummyData();
        
        Set<IDGeoRecord> existingData = new HashSet<IDGeoRecord>();
        Set<byte[]> toDelete = new HashSet<byte[]>();
        for (int i = 0; i < 10; i++) {
            byte[] uuid = generateRandomUUIDAsBytes(idByteLength);
            
            for (int j = 0; j < i + 1; j++) {
                GeoCoordinate geoCoordinate = new GeoCoordinate(randomLattitude(), randomLongitude());
                existingData.add(geoConverter.toIDGeoRecord(
                        geoCoordinate.getLatitude(), geoCoordinate.getLongitude(), uuid));
            }
            
            if (i < countToDelete) {
                toDelete.add(uuid);
            }
        }
        
        delete_and_flush(toDelete, true, existingData, 40);
    }
    
    private void delete_and_flush(Set<byte[]> toDelete, final boolean fileExists, 
            final Set<IDGeoRecord> existingData,
            int expectedIndexSize) throws IOException, IndexTooLargeException {
        for (byte[] uuid: toDelete) {
            indexer.delete(Arrays.copyOf(uuid, uuid.length));
        }

        context.checking(new Expectations() {
            { 
                if (fileExists) {
                    one(mockGeoSegmentReader).getIterator(with(IDGeoRecord.MIN_VALID_GEORECORD), with(IDGeoRecord.MAX_VALID_GEORECORD));
                    will(returnValue(existingData.iterator()));
                    
                    one(mockGeoSegmentReader).close();
                }
                
                one(mockGeoSegmentWriter).close();
                
                one(mockLock).release();
            }
        });
        
        indexer.flush();
        
        indexer.close();
        
        context.assertIsSatisfied();
        
        assertEquals(expectedIndexSize, lastDataFlushed.size());
    }
    
    @Test
    public void test_index_delete_and_flush_maxEntries() throws IOException, IndexTooLargeException {
        config.setMaxIndexSize(50);
        doTest_index_delete_and_flush();
    }
    
    @Test(expected = IndexTooLargeException.class)
    public void test_index_delete_and_flush_tooManyEntries() throws IOException, IndexTooLargeException {
        config.setMaxIndexSize(49);
        doTest_index_delete_and_flush();
    }
    
    @Test
    public void test_index_delete_and_flush() throws IOException, IndexTooLargeException {
        doTest_index_delete_and_flush();
    }
    
    private void doTest_index_delete_and_flush() throws IOException, IndexTooLargeException {
        final Set<IDGeoRecord> existingData = new HashSet<IDGeoRecord>();
        Set<byte[]> toDelete = new HashSet<byte[]>();
        Set<byte[]> toIndex = new HashSet<byte[]>();
        for (int i = 0; i < 10; i++) {
            byte[] uuid = generateRandomUUIDAsBytes(idByteLength);
            
            for (int j = 0; j < i + 1; j++) {
                GeoCoordinate geoCoordinate = new GeoCoordinate(randomLattitude(), randomLongitude());
                existingData.add(geoConverter.toIDGeoRecord(
                        geoCoordinate.getLatitude(), geoCoordinate.getLongitude(), uuid));
            }
            
            if (i < 5) {
                toDelete.add(uuid);
            }
            
            toIndex.add(uuid);
        }
        
        for (byte[] uuid: toDelete) {
            indexer.delete(Arrays.copyOf(uuid, uuid.length));
        }
        
        for (byte[] uuid: toIndex) {
            GeoCoordinate geoCoordinate = new GeoCoordinate(randomLattitude(), randomLongitude());
            GeoCoordinateField field = new GeoCoordinateField("fieldname", geoCoordinate);
            
            indexer.index(Arrays.copyOf(uuid, uuid.length), field);
        }

        createIndexFileDummyData();
        
        context.checking(new Expectations() {
            { 
                one(mockGeoSegmentReader).getIterator(with(IDGeoRecord.MIN_VALID_GEORECORD), with(IDGeoRecord.MAX_VALID_GEORECORD));
                will(returnValue(existingData.iterator()));
                
                one(mockGeoSegmentReader).close();
                
                if (50 <= config.getMaxIndexSize()) {
                    one(mockGeoSegmentWriter).close();
                }
                
                one(mockLock).release();
            }
        });
        
        try {
            indexer.flush();
        } finally {
            indexer.close();
        }
        
        context.assertIsSatisfied();
        
        assertEquals(50, lastDataFlushed.size());
    }
    
    @Test(expected = IOException.class)
    public void index_and_flush_error_on_read() throws IOException, IndexTooLargeException {
        String fieldName = "field1";
        
        for (int i = 0; i < 10; i++) {
            byte[] uuid = generateRandomUUIDAsBytes(idByteLength);
            GeoCoordinate geoCoordinate = new GeoCoordinate(randomLattitude(), randomLongitude());
            GeoCoordinateField field = new GeoCoordinateField(fieldName, geoCoordinate);
            
            indexer.index(uuid, field);
        }
        
        createIndexFileDummyData();

        context.checking(new Expectations() {
            { 
                one(mockGeoSegmentReader).getIterator(with(IDGeoRecord.MIN_VALID_GEORECORD), with(IDGeoRecord.MAX_VALID_GEORECORD));
                will(throwException(new IOException()));
                
                one(mockGeoSegmentReader).close();
            }
        });
        
        indexer.flush();
    }
    
    @Test(expected = LockObtainFailedException.class)
    public void index_and_flush_lockObtainFailed() throws IOException {
        context.checking(new Expectations() {
            {
                one(mockLock).obtain();
                will(returnValue(false));
            }
        });
        
        GeoOnlyIndexer indexer = new GeoOnlyIndexer(config, directory, indexName);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void index_baduuid() throws IOException {
        String fieldName = "myField";
        GeoCoordinate geoCoordinate = new GeoCoordinate(randomLattitude(), randomLongitude());
        GeoCoordinateField field = new GeoCoordinateField(fieldName, geoCoordinate);
        
        indexer.index(new byte[] {(byte)0}, field);
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
