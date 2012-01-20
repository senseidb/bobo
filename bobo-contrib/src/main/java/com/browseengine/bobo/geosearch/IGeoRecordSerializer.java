package com.browseengine.bobo.geosearch;

import java.io.IOException;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

import com.browseengine.bobo.geosearch.bo.IGeoRecord;

/**
 * 
 * @author gcooney
 *
 */
public interface IGeoRecordSerializer<G extends IGeoRecord> {
    public void writeGeoRecord(IndexOutput output, G record, int recordByteCount) throws IOException;
    public G readGeoRecord(IndexInput input, int recordByteCount) throws IOException;
}
