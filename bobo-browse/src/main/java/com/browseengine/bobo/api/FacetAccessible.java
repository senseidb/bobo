package com.browseengine.bobo.api;

import java.util.List;

public interface FacetAccessible 
{
	/**
	 * Gets gathered top facets
	 * @return list of facets
	 */
	List<BrowseFacet> getFacets();
	
	/**
   * Gets the facet given a value. This is a way for random accessing
   * into the facet data structure.
	 * @param value Facet value
	 * @return a facet with count filled in
	 */
	BrowseFacet getFacet(String value);
  
	/**
	 * Gets the facet count given a value. This is a way for random
   * accessing the facet count.
	 * @param value Facet value
	 * @return a facet with count filled in
	 */
	int getFacetHitsCount(Object value);

	/**
   * Responsible for release resources used. If the implementing class
   * does not use a lot of resources,
	 * it does not have to do anything.
	 */
	public void close();
	
	/**
	 * Returns an iterator to visit all the facets
	 * @return	Returns a FacetIterator to iterate over all the facets
	 */
	FacetIterator iterator();
}
