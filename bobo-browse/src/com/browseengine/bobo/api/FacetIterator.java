/**
 * Iterator to iterate over facets
 */
package com.browseengine.bobo.api;

import java.util.Iterator;

/**
 * @author nnarkhed
 *
 */
public interface FacetIterator extends Iterator{

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
	 * @return	null
	 */
	Object next();
}
