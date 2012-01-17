package com.browseengine.bobo.geosearch.solo.impl;

import java.io.IOException;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;

/**
 * 
 * @author gcooney
 *
 */
public class IDGeoRecordSerializer implements IGeoRecordSerializer<IDGeoRecord>{

    public static final int INTERLACE_BYTES = 12;
    
    @Override
    public void writeGeoRecord(IndexOutput output, IDGeoRecord record, int recordByteCount) throws IOException {
        if (record.id.length != recordByteCount - INTERLACE_BYTES) {
            throw new IllegalArgumentException("Incorrect number of id bytes given.  " +
                    "This is most likely a bug!  ExpectedBytes=" + 
                    (recordByteCount - INTERLACE_BYTES)  + 
                    "; receivedBytes=" + record.id.length);
        }
        
        output.writeLong(record.highOrder);
        output.writeInt(record.lowOrder);
        output.writeBytes(record.id, record.id.length);
    }

    @Override
    public IDGeoRecord readGeoRecord(IndexInput input, int recordByteCount) throws IOException {
        long highOrder = input.readLong();
        int lowOrder = input.readInt();
        int countIdBytes = recordByteCount - INTERLACE_BYTES;
        byte[] id = new byte[countIdBytes];
        input.readBytes(id, 0, countIdBytes, false);
        return new IDGeoRecord(highOrder, lowOrder, id);
    }

}
