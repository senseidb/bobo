/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.browseengine.bobo.api.FacetVisitor;

/**
 * @author nnarkhed
 *
 */
public class DefaultFacetVisitor implements FacetVisitor {

	private Map<String, Integer> _facetMap;
	
	public DefaultFacetVisitor() {
		_facetMap = new HashMap<String, Integer>();
	}
	
	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetVisitor#visit(java.lang.String, int)
	 */
	public void visit(String facet, int count) {
		Integer facetCount = _facetMap.get(facet);
		if(facetCount == null) {
			// new facet
			_facetMap.put(facet, count);
		}else {
			// update count for existing facet
			_facetMap.put(facet, count+facetCount);
		}
	}

	/**
	 * returns the mapping of facets to their cumulative hit counts
	 * @return facetMap
	 */
	public Map<String, Integer> getFacetMap() {
		return Collections.unmodifiableMap(_facetMap);
	}
}
