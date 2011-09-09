package com.browseengine.bobo.geosearch.index.impl;

import java.io.Closeable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexOutput;

import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.impl.BTree;

/**
 * Assumes a full binary tree.
 * 
 * @author Shane Detsch
 * @author Geoff Cooney
 */
public class GeoSegmentWriter extends BTree<GeoRecord> implements Closeable {

    public static final int BYTES_PER_RECORD = 13; 
    
    IndexOutput indexOutput;
    GeoSegmentInfo geoSegmentInfo;
    int maxIndex;
    
    int arrayLength;
    long treeDataStart;
    
    public GeoSegmentWriter(Set<GeoRecord> tree, Directory directory, String fileName, 
            GeoSegmentInfo geoSegmentInfo) throws IOException {
        super(tree.size(), false);
        this.arrayLength = tree.size();
        this.geoSegmentInfo = geoSegmentInfo;
        
        indexOutput = directory.createOutput(fileName);
        
        buildBTreeFromSet(tree);
    }
    
    public GeoSegmentWriter(int treeSize, Iterator<GeoRecord> inputIterator, Directory directory, String fileName,
            GeoSegmentInfo geoSegmentInfo) throws IOException {
        super(treeSize, false);
        this.arrayLength = treeSize;
        this.geoSegmentInfo = geoSegmentInfo;
        
        indexOutput = directory.createOutput(fileName);
        
        buildBTreeFromIterator(inputIterator);
    }
    
    private void buildBTreeFromIterator(Iterator<GeoRecord> geoIter) throws IOException {
        writeGeoInfo();
        
        int index = getLeftMostLeafIndex();
        ensureNotWritingPastEndOfFile(index);
        while (geoIter.hasNext()) {
            setValueAtIndex(index, geoIter.next());
            index = getNextIndex(index);
            
            if(index >= this.arrayLength) {
                throw new IllegalArgumentException("Tree only created for " + arrayLength + " nodes but iterator contains more than that");
            }
        }
        
        maxIndex = index;
    }
    
    private void buildBTreeFromSet(Set<GeoRecord> geoSet) throws IOException {
        buildBTreeFromIterator(geoSet.iterator());
    }
    
    @Override
    public int getArrayLength() {
        return arrayLength;
    }

    @Override
    protected int compareValuesAt(int leftIndex, int rightIndex) {
        throw new UnsupportedOperationException("GeoRecordOutputBTree only supports write operations");
    }

    @Override
    protected boolean isNullNoRangeCheck(int index) {
        throw new UnsupportedOperationException("GeoRecordOutputBTree only supports write operations");
    }

    @Override
    protected GeoRecord getValueAtIndex(int index) {
        throw new UnsupportedOperationException("GeoRecordOutputBTree only supports write operations");
    }

    @Override
    protected int compare(int index, GeoRecord value) {
        throw new UnsupportedOperationException("GeoRecordOutputBTree only supports write operations");
    }
    
    private static final int ZERO_BUFFER_SIZE = 4096;

    @Override
    protected void setValueAtIndex(int index, GeoRecord value) throws IOException {
        indexOutput.seek(getSeekPosForIndex(index));
        writeRecord(value);
    }
    
    protected long getSeekPosForIndex(int index) {
        return treeDataStart + BYTES_PER_RECORD * index;
    }
    
    protected void ensureNotWritingPastEndOfFile(int leftMostLeafIndex) throws IOException {
        if (leftMostLeafIndex == 0) {
            return;
        }
        long seekPos = getSeekPosForIndex(leftMostLeafIndex);
        long indexOutputLength;
        if (seekPos > (indexOutputLength = indexOutput.length())) {
            long pos = indexOutputLength;
            indexOutput.seek(pos);
            long size = Math.min(ZERO_BUFFER_SIZE, seekPos - indexOutputLength);
            int intSize = (int)size;
            byte[] zeroBuf = new byte[intSize];
            Arrays.fill(zeroBuf, (byte)0);
            do
             {
                size = Math.min(ZERO_BUFFER_SIZE, seekPos - indexOutputLength);
                intSize = (int)size;
                indexOutput.writeBytes(zeroBuf, 0, intSize);
                pos += intSize;
                indexOutput.seek(pos);
            }while (seekPos > (indexOutputLength = indexOutput.length()));
        }
    }

    private void writeRecord(GeoRecord geoRecord) throws IOException {
        indexOutput.writeLong(geoRecord.highOrder);
        indexOutput.writeInt(geoRecord.lowOrder);
        indexOutput.writeByte(geoRecord.filterByte);
    }
    
    private void writeGeoInfo() throws IOException {
        //write version
        indexOutput.writeVInt(geoSegmentInfo.getGeoVersion());
        long afterVersionFilePointer = indexOutput.getFilePointer();
        indexOutput.writeInt(0); //placeholder for tree position
        indexOutput.writeVInt(arrayLength);  //tree size
        
        //now write field -> filterByte mapping info
        IFieldNameFilterConverter filterConverter = geoSegmentInfo.getFieldNameFilterConverter();
        filterConverter.writeToOutput(indexOutput);
        
        //seek back to just after version and write actual tree location
        treeDataStart = indexOutput.getFilePointer();
        indexOutput.seek(afterVersionFilePointer);
        indexOutput.writeInt((int)treeDataStart);
    }
    
    @Override
    public void close() throws IOException {
        indexOutput.close();
    }
    
    // Test if full binary tree.
    public int getMaxIndex() {
        return maxIndex;
    }
    
}
