/**
 * 
 */
package com.browseengine.bobo.geosearch.merge.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.index.MergeState.DocMap;

import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.impl.BTree;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordComparator;

/**
 * Can merge multiple BTreeAsArray&lg;GeoRecord&gt; instances.
 * Implements an Iterator that walks the GeoRecords in ascending 
 * order, with correctly assigned docids for the merged output partition.
 * 
 * @author Ken McCracken
 *
 */
public class ChainedConvertedGeoRecordIterator implements Iterator<CartesianGeoRecord> {
    
    private static final Logger LOGGER = Logger.getLogger(ChainedConvertedGeoRecordIterator.class);

    private static final CartesianGeoRecordComparator geoRecordCompareByBitMag = new CartesianGeoRecordComparator();
    
    protected IGeoConverter geoConverter;
    //protected Iterator<CartesianGeoRecord> mergedIterator;
    protected OrderedIteratorChain<CartesianGeoRecord> orderedIteratorChain;
    
    public ChainedConvertedGeoRecordIterator(IGeoConverter geoConverter, 
            List<BTree<CartesianGeoRecord>> partitions,
            DocMap[] docMaps, int[] docOffset,
            int totalBufferCapacity) throws IOException {
        this.geoConverter = geoConverter;
        
        int numberOfPartitions = partitions.size();
        if (numberOfPartitions != docMaps.length) {
            throw new RuntimeException("bad input, partitions.size() "
                    + numberOfPartitions + ", deletedDocsList.size() " + docMaps.length);
        }
        
        int bufferCapacityPerIterator = totalBufferCapacity / numberOfPartitions;
        List<Iterator<CartesianGeoRecord>> mergedIterators = new ArrayList<Iterator<CartesianGeoRecord>>(partitions.size());
        
        for (int i = 0; i < partitions.size(); i++) {
            BTree<CartesianGeoRecord> partition = partitions.get(i);
            Iterator<CartesianGeoRecord> mergedIterator = 
                new ConvertedGeoRecordIterator(geoConverter, partition, 
                        docOffset[i], docMaps[i]);
            mergedIterator = new BufferedOrderedIterator<CartesianGeoRecord>(mergedIterator, 
                    geoRecordCompareByBitMag, bufferCapacityPerIterator);
            mergedIterators.add(mergedIterator);
        }

        orderedIteratorChain = 
            new OrderedIteratorChain<CartesianGeoRecord>(mergedIterators, geoRecordCompareByBitMag);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return orderedIteratorChain.hasNext();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public CartesianGeoRecord next() {
        return orderedIteratorChain.next();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
}
