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

    @Override
    public void writeGeoRecord(IndexOutput output, IDGeoRecord record) throws IOException {
        output.writeLong(record.highOrder);
        output.writeInt(record.lowOrder);
        output.writeBytes(record.id, record.id.length);
    }

    @Override
    public IDGeoRecord readGeoRecord(IndexInput input) throws IOException {
        long highOrder = input.readLong();
        int lowOrder = input.readInt();
        //FIXME:  Hardcoding 16 id bytes is dangerous.  Once we start storing the
        //entry byte length in the tree, we should pass in that value to this function
        //so we can infer how many bytes remain for the id
        int countIdBytes = 16;
        byte[] id = new byte[countIdBytes];
        input.readBytes(id, 0, countIdBytes, false);
        return new IDGeoRecord(highOrder, lowOrder, id);
    }

}
