package com.browseengine.bobo.geosearch.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.springframework.stereotype.Component;

import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.bo.GeoRecord;

/**
 * Provides a map backed implementation of a File name to filter converter
 * 
 * @author Geoff Cooney
 *
 */
@Component
public class MappedFieldNameFilterConverter implements IFieldNameFilterConverter {
    public static final int FIELD_FILTER_VERSION = 0;
    
    Map<String, Byte> bitmasks;
    
    public MappedFieldNameFilterConverter() {
        bitmasks = new HashMap<String, Byte>();
    }
    
    public void addFieldBitMask(String fieldName, byte bitMask) {
        bitmasks.put(fieldName, bitMask);
    }
    
    @Override
    public byte getFilterValue(String[] fieldNames) {
        if (fieldNames == null || fieldNames.length == 0) {
            return GeoRecord.DEFAULT_FILTER_BYTE;
        }
            
        byte filterByte = (byte)0;
        for (String fieldName: fieldNames) {
            Byte bitmask = bitmasks.get(fieldName);
            if (bitmask != null) {
                filterByte = (byte) (filterByte | bitmask.byteValue());
            }
        }
            
        return filterByte;
    }

    @Override
    public List<String> getFields(byte filterValue) {
        List<String> filterFields = new Vector<String>(); 
        
        for (Map.Entry<String, Byte> bitmaskEntry : bitmasks.entrySet()) {
            String field = bitmaskEntry.getKey();
            Byte bitmask = bitmaskEntry.getValue();
            
            if ((filterValue & bitmask.byteValue()) != 0) {
                filterFields.add(field);
            }
        }
        
        return filterFields;
    }

    @Override
    public boolean fieldIsInFilter(String fieldName, byte filterValue) {
        Byte bitmask = bitmasks.get(fieldName);
        
        return bitmask != null && (filterValue & bitmask.byteValue()) != 0;
    }

    @Override
    public void writeToOutput(DataOutput output) throws IOException {
        output.writeVInt(FIELD_FILTER_VERSION);
        
        if (bitmasks != null) {
            output.writeVInt(bitmasks.size());
            for (Map.Entry<String, Byte> filterEntry: bitmasks.entrySet()) {
                output.writeString(filterEntry.getKey());
                output.writeByte(filterEntry.getValue());
            }
        } else {
            output.writeVInt(0);
        }
    }

    @Override
    public void loadFromInput(DataInput input) throws IOException {
        input.readVInt();  //read version
        
        int mapSize = input.readVInt();
        bitmasks = new HashMap<String, Byte>(mapSize);
        for (int i = 0; i < mapSize; i++) {
            String fieldName = input.readString();
            Byte filterByte = input.readByte();
            
            bitmasks.put(fieldName, filterByte);
        }
    }
    
}
