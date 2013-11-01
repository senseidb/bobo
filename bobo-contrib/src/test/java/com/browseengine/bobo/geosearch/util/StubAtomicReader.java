package com.browseengine.bobo.geosearch.util;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.util.Bits;

public class StubAtomicReader extends AtomicReader {

    
    private String segmentName;

    public StubAtomicReader() {
        this("");
    }
    
    public StubAtomicReader(String segmentName) {
        this.segmentName = segmentName;
    }
    
    public String getSegmentName() {
        return segmentName;
    }
    
    @Override
    public Fields fields() throws IOException {
        return null;
    }

    @Override
    public NumericDocValues getNumericDocValues(String field) throws IOException {
        return null;
    }

    @Override
    public BinaryDocValues getBinaryDocValues(String field) throws IOException {
        return null;
    }

    @Override
    public SortedDocValues getSortedDocValues(String field) throws IOException {
        return null;
    }

    @Override
    public SortedSetDocValues getSortedSetDocValues(String field) throws IOException {
        return null;
    }

    @Override
    public NumericDocValues getNormValues(String field) throws IOException {
        return null;
    }

    @Override
    public FieldInfos getFieldInfos() {
        return null;
    }

    @Override
    public Bits getLiveDocs() {
        return null;
    }

    @Override
    public Fields getTermVectors(int docID) throws IOException {
        return null;
    }

    @Override
    public int numDocs() {
        return 0;
    }

    @Override
    public int maxDoc() {
        return 0;
    }

    @Override
    public void document(int docID, StoredFieldVisitor visitor) throws IOException {
        
    }

    @Override
    protected void doClose() throws IOException {
        
    }
    
}
