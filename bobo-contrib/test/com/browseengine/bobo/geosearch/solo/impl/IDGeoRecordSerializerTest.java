package com.browseengine.bobo.geosearch.solo.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.junit.Test;
import org.springframework.test.annotation.IfProfileValue;

import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.impl.IGeoRecordSerializerTezt;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;

/**
 * 
 * @author gcooney
 *
 */
@IfProfileValue(name = "test-suite", values = { "unit", "all" })
public class IDGeoRecordSerializerTest extends IGeoRecordSerializerTezt<IDGeoRecord> {

    @Override
    public IGeoRecordSerializer<IDGeoRecord> getGeoRecordSerializer() {
        return new IDGeoRecordSerializer();
    }

    @Test
    public void testSerializeAndDeserialize() throws IOException {
        int idByteCount = 1  + IDGeoRecordSerializer.INTERLACE_BYTES;
        IDGeoRecord geoRecord = new IDGeoRecord(Long.MAX_VALUE, Integer.MAX_VALUE, new byte[] {Byte.MAX_VALUE});
        serializeAndDeserialize(geoRecord,  idByteCount);
        
        geoRecord = new IDGeoRecord(0, 0, new byte[] {(byte)0});
        serializeAndDeserialize(geoRecord, idByteCount);
        
        geoRecord = new IDGeoRecord(0, Integer.MAX_VALUE, new byte[] {(byte)0});
        serializeAndDeserialize(geoRecord, idByteCount);
        
        geoRecord = new IDGeoRecord(Long.MAX_VALUE, 0, new byte[] {(byte)(Byte.MAX_VALUE / 2)});
        serializeAndDeserialize(geoRecord, idByteCount);
    }
    
    @Test
    public void testSerializeAndDeserialize_multiplebyteUUID() throws IOException {
        int idbyteCount = 16 ;
        byte[] id = new byte[idbyteCount];
        for (int i = 0; i< idbyteCount; i++) {
            id[i] = (byte)i; 
        }
        
        IDGeoRecord geoRecord = new IDGeoRecord(Long.MAX_VALUE, 0, id);
        serializeAndDeserialize(geoRecord, idbyteCount + IDGeoRecordSerializer.INTERLACE_BYTES);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testSerializeAndDeserialize_tooManyBytes() throws IOException {
        int byteCount = 1;
        byte[] id = new byte[16];
        for (int i = 0; i< id.length; i++) {
            id[i] = (byte)i; 
        }
        
        IDGeoRecord geoRecord = new IDGeoRecord(Long.MAX_VALUE, 0, id);
        
        IndexOutput output = directory.createOutput(testFileName);
        try {
            geoRecordSerializer.writeGeoRecord(output, geoRecord, byteCount + IDGeoRecordSerializer.INTERLACE_BYTES);
        } finally {
            output.close();
        }
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testSerializeAndDeserialize_notEnoughManyBytes() throws IOException {
        int byteCount = 2;
        byte[] id = new byte[1];
        for (int i = 0; i< id.length; i++) {
            id[i] = (byte)i; 
        }
        
        IDGeoRecord geoRecord = new IDGeoRecord(Long.MAX_VALUE, 0, id);

        IndexOutput output = directory.createOutput(testFileName);
        try {
            geoRecordSerializer.writeGeoRecord(output, geoRecord, byteCount  + IDGeoRecordSerializer.INTERLACE_BYTES);
        } finally {
            output.close();
        }
    }
    
    @Test
    public void testSerializeAndDeserialize_multipleRecords() throws IOException {
        int byteCount = 1 + IDGeoRecordSerializer.INTERLACE_BYTES;
        IndexOutput output = directory.createOutput(testFileName);
        
        for (long highIdx = 0; highIdx < 10; highIdx++) {
            for (int lowIdx = 0; lowIdx < 10; lowIdx++) {
                for (byte byteIdx = 0; byteIdx < 10; byteIdx++) {
                    IDGeoRecord geoRecord = new IDGeoRecord(highIdx, lowIdx, new byte[] {byteIdx});
                    
                    geoRecordSerializer.writeGeoRecord(output, geoRecord, byteCount);
                }
            }
        }
        
        output.close();
        
        IndexInput input = directory.openInput(testFileName);
        
        for (long highIdx = 0; highIdx < 10; highIdx++) {
            for (int lowIdx = 0; lowIdx < 10; lowIdx++) {
                for (byte byteIdx = 0; byteIdx < 10; byteIdx++) {
                    IDGeoRecord expectedRecord = new IDGeoRecord(highIdx, lowIdx, new byte[] {byteIdx});
                    IDGeoRecord actualRecord = geoRecordSerializer.readGeoRecord(input, byteCount);
                    assertEquals(expectedRecord, actualRecord);
                }
            }
        }

        input.close();
    }
    
}
