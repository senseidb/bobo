/**
 * Iterator to iterate over facets
 */
package com.browseengine.bobo.api;

import java.util.Iterator;

/**
 * @author nnarkhed
 *
 */
public interface FacetIterator extends Iterator<String>{

	/**
	 * Returns the facet name of the current facet in the iteration
	 * @return	the facet name of the current facet
	 * @throws	NoSuchElementException	if the iteration has no more facets
	 */
	String getFacet();
	
	/**
	 * Returns the facet count of the current facet in the iteration
	 * @return	the facet count of the current facet
	 * @throws	NoSuchElementException	if the iteration has no more facets
	 */
	int getFacetCount();
	
	/**
	 * Moves the iteration to the next facet
	 * @return	 the next facet value
	 */
	String next();
	
    /**
     * Moves the iteration to the next facet whose hitcount >= minHits. returns null if there is no facet whose hitcount >= minHits.
     * Hence while using this method, it is useless to use hasNext() with it.
     * After the next() method returns null, calling it repeatedly would result in undefined behavior 
     * @return   The next facet value. It returns null if there is no facet whose hitcount >= minHits.
     */	
	String next(int minHits);
}
