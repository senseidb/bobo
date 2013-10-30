/**
 * 
 */
package com.browseengine.bobo.geosearch.merge.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.MergeState.DocMap;

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.impl.BTree;

/**
 * Returns GeoRecords in some order.
 * The returned GeoRecords have already been translated into the new merged docid 
 * space, that is, their values have been shifted.  
 * However, it is possible to have local out-of-order regions of the iterator, 
 * for the purposes of the shifted/merged docid dimension, since the 
 * order of records is the derived from their ordering in the oridinal docid dimension.
 * 
 * <p>
 * Said another way, for all returned <tt>GeoRecord</tt> instances, their <tt>.docid</tt> 
 * property will be properly shifted to the after-merge value, but the <tt>GeoRecord</tt> 
 * returned by <tt>next()</tt> is not guaranteed greater than all those that preceded it.
 * 
 * @author Ken McCracken
 *
 */
public class ConvertedGeoRecordIterator implements Iterator<CartesianGeoRecord> {

    private static final Logger LOGGER = Logger.getLogger(ConvertedGeoRecordIterator.class);

    private final IGeoConverter geoConverter;
    private final int absoluteDocidOffset;
    private final DocMap docMap;

    private CartesianGeoRecord next;

    private final Iterator<CartesianGeoRecord> iteratorWithOriginalDocIds;
    
    public ConvertedGeoRecordIterator(
            IGeoConverter geoConverter,
            BTree<CartesianGeoRecord> geoRecords,
            int absoluteDocidOffset,
            DocMap docMap) throws IOException {
        this.absoluteDocidOffset = absoluteDocidOffset;

        this.geoConverter = geoConverter;

        this.docMap = docMap;

        // we need to shift docid from what is requested for the merged partition 
        // and what we know in this tree
        
        iteratorWithOriginalDocIds = geoRecords.getIterator(CartesianGeoRecord.MIN_VALID_GEORECORD, CartesianGeoRecord.MAX_VALID_GEORECORD);
        advance();
    }
    
    private void printInOriginalRange(CartesianGeoRecord candidate, CartesianCoordinateDocId raw) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("candidate in original range, geoRecord "+candidate+", raw "+raw);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return next != null;
    }
    
    private void advance() {
        if (!iteratorWithOriginalDocIds.hasNext()) {
            next = null;
            return;
        }
        CartesianGeoRecord geoRecordInNewRange = null;
        CartesianGeoRecord candidate = null;
        do {
            if (iteratorWithOriginalDocIds.hasNext()) {
                candidate = iteratorWithOriginalDocIds.next();
                if (null != candidate) {
                    CartesianCoordinateDocId rawInOriginalDocId = geoConverter.toCartesianCoordinateDocId(candidate);
                    if (docMap.get(rawInOriginalDocId.docid) == -1) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("skipping deleted docid before merge for raw "+rawInOriginalDocId);
                        }
                    } else {
                        printInOriginalRange(candidate, rawInOriginalDocId);
                        geoRecordInNewRange = getGeoRecordInNewRange(candidate, rawInOriginalDocId);
                    }
                }
            } else {
                candidate = null;
            }
            if (null != candidate && null == geoRecordInNewRange) {
                String msg = "skipping candidate "+candidate+", geoRecordInNewRange null";
                LOGGER.warn(msg);
            }
        } while (null != candidate && null == geoRecordInNewRange);
        if (null != geoRecordInNewRange) {
            next = geoRecordInNewRange;
        } else {
            next = null;
        }
    }
    
    private CartesianGeoRecord getGeoRecordInNewRange(CartesianGeoRecord candidate, 
            CartesianCoordinateDocId rawInOriginalDocId) {
        // translate into merged space
        CartesianCoordinateDocId merged = rawInOriginalDocId.clone();
        int mergedDocId = docMap.get(merged.docid) + absoluteDocidOffset;
        if (mergedDocId >= 0) {
            merged.docid = mergedDocId;
            return geoConverter.toCartesianGeoRecord(merged, candidate.filterByte);
        }
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public CartesianGeoRecord next() {
        CartesianGeoRecord next = this.next;
        advance();
        if (null != next) {
            return next;
        }
        throw new NoSuchElementException();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
        

}
