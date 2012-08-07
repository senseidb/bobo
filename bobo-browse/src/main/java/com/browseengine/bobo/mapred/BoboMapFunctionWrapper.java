package com.browseengine.bobo.mapred;

import java.util.List;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.FacetCountCollector;

/**
 * Is the part of the bobo request, that maintains the map result intermediate state
 *
 */
public interface BoboMapFunctionWrapper {
	/**
	 * When there is no filter, map reduce will try to map the entire segment
	 * @param reader
	 */
	public void mapFullIndexReader(BoboIndexReader reader, FacetCountCollector[] facetCountCollectors);
	/**
	 * The basic callback method for a single doc
	 * @param docId
	 * @param reader
	 */
	public void mapSingleDocument(int docId, BoboIndexReader reader);
	/**
	 * The callback method, after the segment was processed
	 * @param reader
	 */
	public void finalizeSegment(BoboIndexReader reader,  FacetCountCollector[] facetCountCollectors);
	/**
   * The callback method, after the partition was processed
   * 
   */
	public void finalizePartition();	
	public MapReduceResult getResult();
}
