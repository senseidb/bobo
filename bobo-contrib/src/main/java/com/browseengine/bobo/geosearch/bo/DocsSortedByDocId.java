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
    
    private TreeMap<Integer, Collection<GeRecordAndCartesianDocId>> docs;
    
    public DocsSortedByDocId() {
        docs = new TreeMap<Integer, Collection<GeRecordAndCartesianDocId>>(new IntegerComparator());
    }
    
    public void add(int docid, GeRecordAndCartesianDocId data) {
        Collection<GeRecordAndCartesianDocId> collection = docs.get(docid);
        if (null != collection) {
            collection.add(data);
        } else {
            collection = new HashSet<GeRecordAndCartesianDocId>();
            collection.add(data);
            docs.put(docid, collection);
        }
    }
    
    public Iterator<Entry<Integer,Collection<GeRecordAndCartesianDocId>>> getScoredDocs() {
        return docs.entrySet().iterator();
    }
    
    public Entry<Integer, Collection<GeRecordAndCartesianDocId>> pollFirst() {
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
