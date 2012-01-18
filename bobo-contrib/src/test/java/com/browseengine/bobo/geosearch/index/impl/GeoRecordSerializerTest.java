package com.browseengine.bobo.geosearch.index.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.junit.Test;
import org.springframework.test.annotation.IfProfileValue;

import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.impl.GeoRecordSerializer;
import com.browseengine.bobo.geosearch.impl.IGeoRecordSerializerTezt;

@IfProfileValue(name = "test-suite", values = { "unit", "all" })
public class GeoRecordSerializerTest extends IGeoRecordSerializerTezt<GeoRecord> {
    
    @Override
    public IGeoRecordSerializer<GeoRecord> getGeoRecordSerializer() {
        return new GeoRecordSerializer();
    }
    
    @Test
    public void testSerializeAndDeserialize() throws IOException {
        GeoRecord geoRecord = new GeoRecord(Long.MAX_VALUE, Integer.MAX_VALUE, Byte.MAX_VALUE);
        serializeAndDeserialize(geoRecord, GeoSegmentInfo.BYTES_PER_RECORD_V1);
        
        geoRecord = new GeoRecord(0, 0, (byte)0);
        serializeAndDeserialize(geoRecord, GeoSegmentInfo.BYTES_PER_RECORD_V1);
        
        geoRecord = new GeoRecord(0, Integer.MAX_VALUE, (byte)0);
        serializeAndDeserialize(geoRecord, GeoSegmentInfo.BYTES_PER_RECORD_V1);
        
        geoRecord = new GeoRecord(Long.MAX_VALUE, 0, (byte)(Byte.MAX_VALUE / 2));
        serializeAndDeserialize(geoRecord, GeoSegmentInfo.BYTES_PER_RECORD_V1);
    }
    
    @Test
    public void testSerializeAndDeserialize_multipleRecords() throws IOException {
        IndexOutput output = directory.createOutput(testFileName);
        
        for (long highIdx = 0; highIdx < 10; highIdx++) {
            for (int lowIdx = 0; lowIdx < 10; lowIdx++) {
                for (byte byteIdx = 0; byteIdx < 10; byteIdx++) {
                    GeoRecord geoRecord = new GeoRecord(highIdx, lowIdx, byteIdx);
                    
                    geoRecordSerializer.writeGeoRecord(output, geoRecord, GeoSegmentInfo.BYTES_PER_RECORD_V1);
                }
            }
        }
        
        output.close();
        
        IndexInput input = directory.openInput(testFileName);
        
        for (long highIdx = 0; highIdx < 10; highIdx++) {
            for (int lowIdx = 0; lowIdx < 10; lowIdx++) {
                for (byte byteIdx = 0; byteIdx < 10; byteIdx++) {
                    GeoRecord expectedRecord = new GeoRecord(highIdx, lowIdx, byteIdx);
                    GeoRecord actualRecord = geoRecordSerializer.readGeoRecord(input, GeoSegmentInfo.BYTES_PER_RECORD_V1);
                    assertEquals(expectedRecord, actualRecord);
                }
            }
        }

        input.close();
    }

}
