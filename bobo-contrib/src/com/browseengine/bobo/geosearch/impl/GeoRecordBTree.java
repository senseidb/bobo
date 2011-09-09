/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.lucene.store.Directory;

import com.browseengine.bobo.geosearch.IGeoRecordIterator;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;

/**
 * @author Ken McCracken
 *
 */
public class GeoRecordBTree  extends BTree<GeoRecord> implements IGeoRecordIterator {
    public static final long HIGH_ORDER_NULL_NODE_VALUE = -1L;
    
    private final long[] highOrders;
    private final int[] lowOrders;
    private final byte[] filterBytes;

    public GeoRecordBTree(int treeSize, Iterator<GeoRecord> inputIterator, Directory directory, 
            String fileName, GeoSegmentInfo geoSegmentInfo) throws IOException {
        super(treeSize, false);
        
        highOrders = new long[this.arrayLength];
        lowOrders = new int [this.arrayLength];
        filterBytes = new byte [this.arrayLength];
        
        buildTreeFromIterator(inputIterator);
        
        nullCheckChecksValues = true;
    }
    
    public GeoRecordBTree(Set<GeoRecord> tree) throws IOException {
        super(tree.size(), false);
        highOrders = new long[this.arrayLength];
        lowOrders = new int [this.arrayLength];
        filterBytes = new byte [this.arrayLength];
        
        Iterator<GeoRecord> it = tree.iterator();
        buildTreeFromIterator(it);
        nullCheckChecksValues = true;
    }
    
    private void buildTreeFromIterator(Iterator<GeoRecord> geoIter) throws IOException {
        int index = leftMostLeafIndex;
        while(geoIter.hasNext()) {
            GeoRecord geoRecord = geoIter.next();
            highOrders [index] = geoRecord.highOrder;
            lowOrders [index] = geoRecord.lowOrder;
            filterBytes [index] = geoRecord.filterByte;
            index = this.getNextIndex(index);
        }
    }
    
    public GeoRecordBTree(long[] highOrders, int[] lowOrders, byte[] filterBytes) {
        super(highOrders.length, true);
        this.highOrders = highOrders;
        this.lowOrders = lowOrders;
        this.filterBytes = filterBytes;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getArrayLength() {
        return highOrders.length;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected int compare(int index, GeoRecord value) {
        return compare(highOrders[index], lowOrders[index], value.highOrder, value.lowOrder);
    }
    
    private int compare(long leftHighOrder, int leftLowOrder, long rightHighOrder, int rightLowOrder) {
        long diff = leftHighOrder - rightHighOrder;
        if (diff < 0) {
            return -1;
        } else if (diff > 0) {
            return 1;
        }
        int idiff = leftLowOrder - rightLowOrder;
        if(idiff < 0) {
            return -1;
        } else if (idiff > 0) {
            return 1;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int compareValuesAt(int leftIndex, int rightIndex) {
        return compare(highOrders[leftIndex], lowOrders[leftIndex], highOrders[rightIndex], lowOrders[rightIndex]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GeoRecord getValueAtIndex(int index) {
        return new GeoRecord(highOrders[index], lowOrders[index], filterBytes[index]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isNullNoRangeCheck(int index) {
        long highOrder = highOrders[index];
        return HIGH_ORDER_NULL_NODE_VALUE == highOrder;
    }

    public long[] getHighOrders() {
        return highOrders;
    }

    

    public int[] getLowOrders() {
        return lowOrders;
    }

    

    public byte[] getFilterBytes() {
        return filterBytes;
    }

   

    public static long getHighOrderNullNodeValue() {
        return HIGH_ORDER_NULL_NODE_VALUE;
    }

    @Override
    protected void setValueAtIndex(int index, GeoRecord value) throws IOException {
        highOrders[index] = value.highOrder;
        lowOrders[index] = value.lowOrder;
        filterBytes[index] = value.filterByte;
    }

    @Override
    public void close() throws IOException {
    }
    
}
