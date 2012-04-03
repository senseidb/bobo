package com.browseengine.bobo.geosearch;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;

public interface IFieldNameFilterConverter {
    byte getFilterValue(String[] fieldNames);
    List<String> getFields(byte filterValue);
    boolean fieldIsInFilter(String fieldName, byte filterValue);
    
    void writeToOutput(DataOutput output) throws IOException;
    void loadFromInput(DataInput input) throws IOException;
}
