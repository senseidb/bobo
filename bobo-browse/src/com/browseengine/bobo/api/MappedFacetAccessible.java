package com.browseengine.bobo.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MappedFacetAccessible implements FacetAccessible, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final HashMap<String,BrowseFacet> _facetMap;
	private final BrowseFacet[] _facets;
	
	public MappedFacetAccessible(BrowseFacet[] facets){
		_facetMap = new HashMap<String,BrowseFacet>();
		for (BrowseFacet facet : facets){
			_facetMap.put(facet.getValue(), facet);
		}
		_facets = facets;
	}

	public BrowseFacet getFacet(String value) {
		return _facetMap.get(value);
	}

	public List<BrowseFacet> getFacets() {
		return Arrays.asList(_facets);
	}

}
