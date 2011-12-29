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

    private final int idByteCount;

    public IDGeoRecordSerializer(int idByteCount) {
        this.idByteCount = idByteCount;
    }
    
    @Override
    public void writeGeoRecord(IndexOutput output, IDGeoRecord record) throws IOException {
        if (record.id.length != idByteCount) {
            throw new IllegalArgumentException("Incorrect number of id bytes given.  " +
                    "This is most likely a bug!  ExpectedBytes=" + idByteCount + 
                    "; receivedBytes=" + record.id.length);
        }
        
        output.writeLong(record.highOrder);
        output.writeInt(record.lowOrder);
        output.writeBytes(record.id, record.id.length);
    }

    @Override
    public IDGeoRecord readGeoRecord(IndexInput input) throws IOException {
        long highOrder = input.readLong();
        int lowOrder = input.readInt();
        int countIdBytes = idByteCount;
        byte[] id = new byte[countIdBytes];
        input.readBytes(id, 0, countIdBytes, false);
        return new IDGeoRecord(highOrder, lowOrder, id);
    }

}
