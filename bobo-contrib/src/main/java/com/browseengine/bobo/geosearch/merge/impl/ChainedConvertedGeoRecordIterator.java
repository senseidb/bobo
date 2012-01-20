/**
 * 
 */
package com.browseengine.bobo.geosearch.merge.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.util.BitVector;

import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.impl.BTree;
import com.browseengine.bobo.geosearch.impl.GeoRecordComparator;

/**
 * Can merge multiple BTreeAsArray&lg;GeoRecord&gt; instances.
 * Implements an Iterator that walks the GeoRecords in ascending 
 * order, with correctly assigned docids for the merged output partition.
 * 
 * @author Ken McCracken
 *
 */
public class ChainedConvertedGeoRecordIterator implements Iterator<GeoRecord> {
    
    private static final Logger LOGGER = Logger.getLogger(ChainedConvertedGeoRecordIterator.class);

    private static final GeoRecordComparator geoRecordCompareByBitMag = new GeoRecordComparator();
    
    protected IGeoConverter geoConverter;
    protected Iterator<GeoRecord> mergedIterator;
    protected OrderedIteratorChain<GeoRecord> orderedIteratorChain;
    
    public ChainedConvertedGeoRecordIterator(IGeoConverter geoConverter, 
            List<BTree<GeoRecord>> partitions,
            List<BitVector> deletedDocsList, 
            int totalBufferCapacity) throws IOException {
        this.geoConverter = geoConverter;
        
        int numberOfPartitions = partitions.size();
        if (numberOfPartitions != deletedDocsList.size()) {
            throw new RuntimeException("bad input, partitions.size() "
                    + numberOfPartitions + ", deletedDocsList.size() " + deletedDocsList.size());
        }
        
        int docid = 0;
        int bufferCapacityPerIterator = totalBufferCapacity / numberOfPartitions;
        List<Iterator<GeoRecord>> mergedIterators = new ArrayList<Iterator<GeoRecord>>(partitions.size());
        
        for (int i = 0; i < partitions.size(); i++) {
            BTree<GeoRecord> partition = partitions.get(i);
            BitVector deletedDocs = deletedDocsList.get(i);
            Iterator<GeoRecord> mergedIterator = 
                new ConvertedGeoRecordIterator(geoConverter, partition, 
                        docid, deletedDocs);
            mergedIterator = new BufferedOrderedIterator<GeoRecord>(mergedIterator, 
                    geoRecordCompareByBitMag, bufferCapacityPerIterator);
            mergedIterators.add(mergedIterator);
            docid += deletedDocs.size() - deletedDocs.count();
        }

        orderedIteratorChain = 
            new OrderedIteratorChain<GeoRecord>(mergedIterators, geoRecordCompareByBitMag);
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
    public GeoRecord next() {
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
