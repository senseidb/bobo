package com.browseengine.bobo.geosearch.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.UUID;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.Before;

import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.bo.IGeoRecord;

/**
 * 
 * @author gcooney
 *
 * @param <T>
 */
public abstract class IGeoRecordSerializerTezt<T extends IGeoRecord> {
    
    protected IGeoRecordSerializer<T> geoRecordSerializer;
    
    protected Directory directory;
    protected String testFileName;
    
    @Before
    public void setUp() {
        testFileName = UUID.randomUUID().toString();
        directory = new RAMDirectory(); 
        
        geoRecordSerializer = getGeoRecordSerializer();
    }
    
    @After
    public void tearDown() throws IOException {
        if (directory.fileExists(testFileName)) {
            directory.deleteFile(testFileName);
        }
    }
    
    public abstract IGeoRecordSerializer<T> getGeoRecordSerializer();
    
    public void serializeAndDeserialize(T expectedRecord, int byteCount) throws IOException {
        String fileName = UUID.randomUUID().toString();
        
        IndexOutput output = directory.createOutput(fileName);
        geoRecordSerializer.writeGeoRecord(output, expectedRecord, byteCount);
        output.close();
        
        IndexInput input = directory.openInput(fileName);
        T actualRecord = geoRecordSerializer.readGeoRecord(input, byteCount);
        input.close();
        
        assertEquals(expectedRecord, actualRecord);
        
        directory.deleteFile(fileName);
    }
}
