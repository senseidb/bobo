package com.browseengine.bobo.geosearch.solo.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.junit.Test;

import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.impl.IGeoRecordSerializerTezt;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;

/**
 * 
 * @author gcooney
 *
 */
public class IDGeoRecordSerializerTest extends IGeoRecordSerializerTezt<IDGeoRecord> {

    @Override
    public IGeoRecordSerializer<IDGeoRecord> getGeoRecordSerializer() {
        return new IDGeoRecordSerializer(1);
    }

    @Test
    public void testSerializeAndDeserialize() throws IOException {
        IDGeoRecord geoRecord = new IDGeoRecord(Long.MAX_VALUE, Integer.MAX_VALUE, new byte[] {Byte.MAX_VALUE});
        serializeAndDeserialize(geoRecord);
        
        geoRecord = new IDGeoRecord(0, 0, new byte[] {(byte)0});
        serializeAndDeserialize(geoRecord);
        
        geoRecord = new IDGeoRecord(0, Integer.MAX_VALUE, new byte[] {(byte)0});
        serializeAndDeserialize(geoRecord);
        
        geoRecord = new IDGeoRecord(Long.MAX_VALUE, 0, new byte[] {(byte)(Byte.MAX_VALUE / 2)});
        serializeAndDeserialize(geoRecord);
    }
    
    @Test
    public void testSerializeAndDeserialize_multiplebyteUUID() throws IOException {
        int byteCount = 16;
        geoRecordSerializer = new IDGeoRecordSerializer(byteCount);
        byte[] id = new byte[byteCount];
        for (int i = 0; i< byteCount; i++) {
            id[i] = (byte)i; 
        }
        
        IDGeoRecord geoRecord = new IDGeoRecord(Long.MAX_VALUE, 0, id);
        serializeAndDeserialize(geoRecord);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testSerializeAndDeserialize_tooManyBytes() throws IOException {
        int byteCount = 1;
        geoRecordSerializer = new IDGeoRecordSerializer(byteCount);
        byte[] id = new byte[16];
        for (int i = 0; i< id.length; i++) {
            id[i] = (byte)i; 
        }
        
        IDGeoRecord geoRecord = new IDGeoRecord(Long.MAX_VALUE, 0, id);
        
        IndexOutput output = directory.createOutput(testFileName);
        try {
            geoRecordSerializer.writeGeoRecord(output, geoRecord);
        } finally {
            output.close();
        }
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testSerializeAndDeserialize_notEnoughManyBytes() throws IOException {
        int byteCount = 2;
        geoRecordSerializer = new IDGeoRecordSerializer(byteCount);
        byte[] id = new byte[1];
        for (int i = 0; i< id.length; i++) {
            id[i] = (byte)i; 
        }
        
        IDGeoRecord geoRecord = new IDGeoRecord(Long.MAX_VALUE, 0, id);

        IndexOutput output = directory.createOutput(testFileName);
        try {
            geoRecordSerializer.writeGeoRecord(output, geoRecord);
        } finally {
            output.close();
        }
    }
    
    @Test
    public void testSerializeAndDeserialize_multipleRecords() throws IOException {
        IndexOutput output = directory.createOutput(testFileName);
        
        for (long highIdx = 0; highIdx < 10; highIdx++) {
            for (int lowIdx = 0; lowIdx < 10; lowIdx++) {
                for (byte byteIdx = 0; byteIdx < 10; byteIdx++) {
                    IDGeoRecord geoRecord = new IDGeoRecord(highIdx, lowIdx, new byte[] {byteIdx});
                    
                    geoRecordSerializer.writeGeoRecord(output, geoRecord);
                }
            }
        }
        
        output.close();
        
        IndexInput input = directory.openInput(testFileName);
        
        for (long highIdx = 0; highIdx < 10; highIdx++) {
            for (int lowIdx = 0; lowIdx < 10; lowIdx++) {
                for (byte byteIdx = 0; byteIdx < 10; byteIdx++) {
                    IDGeoRecord expectedRecord = new IDGeoRecord(highIdx, lowIdx, new byte[] {byteIdx});
                    IDGeoRecord actualRecord = geoRecordSerializer.readGeoRecord(input);
                    assertEquals(expectedRecord, actualRecord);
                }
            }
        }

        input.close();
    }
    
}
