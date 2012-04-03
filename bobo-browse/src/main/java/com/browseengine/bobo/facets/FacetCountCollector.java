package com.browseengine.bobo.facets;

import java.util.LinkedList;
import java.util.List;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.FacetAccessible;

/**
 *  Collects facet counts for a given browse request
 */
public interface FacetCountCollector extends FacetAccessible
{
	/**
	 * Collect a hit. This is called for every hit, thus the implementation needs to be super-optimized.
	 * @param docid doc
	 */
	void collect(int docid);
	
	/**
	 * Collects all hits. This is called once per request by the facet engine in certain scenarios. 
	 */
	void collectAll();
	
	/**
	 * Gets the name of the facet
	 * @return facet name
	 */
	String getName();
	
	/**
	 * Returns an integer array representing the distribution function of a given facet.
	 * @return integer array of count values representing distribution of the facet values.
	 */
	int[] getCountDistribution();
	
	/**
	 * Empty facet list. 
	 */
	public static List<BrowseFacet> EMPTY_FACET_LIST = new LinkedList<BrowseFacet>();

}
