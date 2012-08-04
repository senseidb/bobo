package com.browseengine.bobo.geosearch.impl;

import java.io.IOException;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;

/**
 * Basic Serializer for CartesianGeoRecords
 * 
 * @author gcooney
 *
 */
public class CartesianGeoRecordSerializer implements IGeoRecordSerializer<CartesianGeoRecord> {

    @Override
    public void writeGeoRecord(IndexOutput output, CartesianGeoRecord record, int recordByteCount) throws IOException {
        output.writeLong(record.highOrder);
        output.writeLong(record.lowOrder);
        output.writeByte(record.filterByte);
    }

    @Override
    public CartesianGeoRecord readGeoRecord(IndexInput input, int recordByteCount) throws IOException {
        return new CartesianGeoRecord(input.readLong(),
                input.readLong(),
                input.readByte());
    }
    
}
