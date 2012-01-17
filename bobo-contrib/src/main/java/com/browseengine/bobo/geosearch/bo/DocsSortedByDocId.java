/**
 * 
 */
package com.browseengine.bobo.geosearch.bo;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * @author Ken McCracken
 *
 */
public class DocsSortedByDocId {
    
    private TreeMap<Integer, Collection<GeoRecordAndLongitudeLatitudeDocId>> docs;
    
    public DocsSortedByDocId() {
        docs = new TreeMap<Integer, Collection<GeoRecordAndLongitudeLatitudeDocId>>(new IntegerComparator());
    }
    
    public void add(int docid, GeoRecordAndLongitudeLatitudeDocId data) {
        Collection<GeoRecordAndLongitudeLatitudeDocId> collection = docs.get(docid);
        if (null != collection) {
            collection.add(data);
        } else {
            collection = new HashSet<GeoRecordAndLongitudeLatitudeDocId>();
            collection.add(data);
            docs.put(docid, collection);
        }
    }
    
    public Iterator<Entry<Integer,Collection<GeoRecordAndLongitudeLatitudeDocId>>> getScoredDocs() {
        return docs.entrySet().iterator();
    }
    
    public Entry<Integer, Collection<GeoRecordAndLongitudeLatitudeDocId>> pollFirst() {
        return docs.pollFirstEntry();
    }
    
    public int size() {
        return docs.size();
    }
    
    private static class IntegerComparator implements Comparator<Integer> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(Integer arg0, Integer arg1) {
            return arg0.compareTo(arg1);
        }
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "DocsSortedByDocId [docs=" + docs + "]";
    }
    
    

}
