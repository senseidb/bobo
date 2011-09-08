/**
 * 
 */
package com.browseengine.bobo.geosearch;

import java.io.IOException;
import java.util.Iterator;

import com.browseengine.bobo.geosearch.bo.GeoRecord;

/**
 * The method to use on the GeoRecord tree/index.
 * 
 * @author Ken McCracken
 *
 */
public interface IGeoRecordIterator {
    
    /**
     * Returns an iterator on the matching records on 
     * [minValue, maxValue].
     * 
     * @param minValue
     * @param maxValue
     * @return
     */
    Iterator<GeoRecord> getIterator(GeoRecord minValue, GeoRecord maxValue) throws IOException;
}
