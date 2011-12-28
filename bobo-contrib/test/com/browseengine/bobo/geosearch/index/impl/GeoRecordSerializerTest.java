package com.browseengine.bobo.geosearch.index.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.UUID;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.IfProfileValue;

import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.impl.GeoRecordSerializer;

@IfProfileValue(name = "test-suite", values = { "unit", "all" })
public class GeoRecordSerializerTest {
    GeoRecordSerializer geoRecordSerializer;
    
    Directory directory;
    String testFileName;
    
    @Before
    public void setUp() {
        testFileName = UUID.randomUUID().toString();
        directory = new RAMDirectory(); 
        
        geoRecordSerializer = new GeoRecordSerializer();
    }
    
    @After
    public void tearDown() throws IOException {
        if (directory.fileExists(testFileName)) {
            directory.deleteFile(testFileName);
        }
    }
    
    @Test
    public void testSerializeAndDeserialize() throws IOException {
        GeoRecord geoRecord = new GeoRecord(Long.MAX_VALUE, Integer.MAX_VALUE, Byte.MAX_VALUE);
        serializeAndDeserialize(geoRecord);
        
        geoRecord = new GeoRecord(0, 0, (byte)0);
        serializeAndDeserialize(geoRecord);
        
        geoRecord = new GeoRecord(0, Integer.MAX_VALUE, (byte)0);
        serializeAndDeserialize(geoRecord);
        
        geoRecord = new GeoRecord(Long.MAX_VALUE, 0, (byte)(Byte.MAX_VALUE / 2));
        serializeAndDeserialize(geoRecord);
    }
    
    @Test
    public void testSerializeAndDeserialize_multipleRecords() throws IOException {
        IndexOutput output = directory.createOutput(testFileName);
        
        for (long highIdx = 0; highIdx < 10; highIdx++) {
            for (int lowIdx = 0; lowIdx < 10; lowIdx++) {
                for (byte byteIdx = 0; byteIdx < 10; byteIdx++) {
                    GeoRecord geoRecord = new GeoRecord(highIdx, lowIdx, byteIdx);
                    
                    geoRecordSerializer.writeGeoRecord(output, geoRecord);
                }
            }
        }
        
        output.close();
        
        IndexInput input = directory.openInput(testFileName);
        
        for (long highIdx = 0; highIdx < 10; highIdx++) {
            for (int lowIdx = 0; lowIdx < 10; lowIdx++) {
                for (byte byteIdx = 0; byteIdx < 10; byteIdx++) {
                    GeoRecord expectedRecord = new GeoRecord(highIdx, lowIdx, byteIdx);
                    GeoRecord actualRecord = geoRecordSerializer.readGeoRecord(input);
                    assertEquals(expectedRecord, actualRecord);
                }
            }
        }

        input.close();
    }
    
    private void serializeAndDeserialize(GeoRecord expectedRecord) throws IOException {
        String fileName = UUID.randomUUID().toString();
        
        IndexOutput output = directory.createOutput(fileName);
        geoRecordSerializer.writeGeoRecord(output, expectedRecord);
        output.close();
        
        IndexInput input = directory.openInput(fileName);
        GeoRecord actualRecord = geoRecordSerializer.readGeoRecord(input);
        input.close();
        
        assertEquals(expectedRecord, actualRecord);
        
        directory.deleteFile(fileName);
    }
}
