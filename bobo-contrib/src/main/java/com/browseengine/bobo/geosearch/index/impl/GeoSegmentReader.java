package com.browseengine.bobo.geosearch.index.impl;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;

import com.browseengine.bobo.geosearch.GeoVersion;
import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.bo.IGeoRecord;
import com.browseengine.bobo.geosearch.impl.BTree;

public class GeoSegmentReader<G extends IGeoRecord> extends BTree<G> implements Closeable{
    
    private static final Logger LOGGER = Logger.getLogger(GeoSegmentReader.class);

    IndexInput indexInput;
    IGeoRecordSerializer<G> geoRecordSerializer;
    Comparator<G> geoRecordComparator;
    int geoVersion = GeoVersion.VERSION_0;
    int seekPositionOfIndexZero;
    int bytesPerRecord = GeoSegmentInfo.BYTES_PER_RECORD_V1;
    private final int maxDoc;
    
    /*
     * Initial part of the file includes:
     *   vInt version number
     *   int position of the root of the data tree
     *   vInt tree size (number of BYTES_PER_RECORD byte chunks the tree has)
     *   
     * For each record::
     *  indexOutput.seek(treeDataStart + BYTES_PER_RECORD * index);
        indexOutput.writeLong(geoRecord.highOrder);
        indexOutput.writeInt(geoRecord.lowOrder);
        indexOutput.writeByte(geoRecord.filterByte);
     */
    public GeoSegmentReader(Directory dir, String fileName, int maxDoc, int bufferSize,
            IGeoRecordSerializer<G> geoRecordSerializer, Comparator<G> geoRecordComparator) throws IOException {
        this(0, maxDoc, geoRecordSerializer, geoRecordComparator);
        try {
            this.indexInput = dir.openInput(fileName, bufferSize);
            init();
        } catch (FileNotFoundException e) {
            LOGGER.warn("file not found: "+e+", treating this as no "+GeoRecord.class+"s");
            init(0, nullCheckChecksValues);
        }
    }
    
    protected void init() throws IOException {
        geoVersion = indexInput.readVInt(); // Version Number Will be Supported Later
        this.seekPositionOfIndexZero = indexInput.readInt();
        // ArrayLength is the number of 13 Byte GeoRecord (long, int, byte) triplets
        int arrayLength = indexInput.readVInt();
        if (geoVersion > GeoVersion.VERSION_0) {
            this.bytesPerRecord = indexInput.readVInt();
        }
        this.init(arrayLength, nullCheckChecksValues);
    }
    
    /**
     * Test constructor that lets us override and not use a formal IndexInput in certain cases.
     * 
     * @param arrayLength
     * @param maxDoc
     */
    protected GeoSegmentReader(int arrayLength, int maxDoc, 
            IGeoRecordSerializer<G> geoRecordSerializer, Comparator<G> geoRecordComparator) {
        super(arrayLength, false);
        this.maxDoc = maxDoc;
        
        this.geoRecordSerializer = geoRecordSerializer;
        this.geoRecordComparator = geoRecordComparator;
    }
    
    public int getMaxDoc() {
        return maxDoc;
    }
    
    @Override
    public int getArrayLength() {
        return this.arrayLength;
    }

    @Override
    protected int compareValuesAt(int leftIndex, int rightIndex) throws IOException {
        return geoRecordComparator.compare(getValueAtIndex(leftIndex),
                               getValueAtIndex(rightIndex));
    }

    @Override
    protected int compare(int index, G value) throws IOException {
        return geoRecordComparator.compare(getValueAtIndex(index),
                               value);
    }

    @Override
    protected boolean isNullNoRangeCheck(int index) {
        return index < arrayLength;
    }

    @Override
    protected G getValueAtIndex(int index) throws IOException {
        indexInput.seek(this.seekPositionOfIndexZero + 
                        bytesPerRecord*index);
        
        return geoRecordSerializer.readGeoRecord(indexInput, bytesPerRecord);
    }

    @Override
    protected void setValueAtIndex(int index, G value) {//throws IOException {
        throw new UnsupportedOperationException(
                "SetValueAtIndex is currently unsupported for class " +
                "GeoRecordRandomAccessAsArray.java");
    }

    @Override
    public void close() throws IOException {
        if (null != indexInput) {
            indexInput.close();
        }
    }

}
