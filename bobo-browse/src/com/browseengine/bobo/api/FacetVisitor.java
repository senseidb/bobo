/**
 * Interface defines methods used to accumulate facet counts from various segments
 */
package com.browseengine.bobo.api;

/**
 * @author nnarkhed
 *
 */
public interface FacetVisitor {

	/**
	 * Accumulate the hitcount for the given facet
	 * @param facet		the facet value for which the hit count is to be accumulated
	 * @param count		the facet hit count
	 */
	public void visit(String facet, int count);
}
