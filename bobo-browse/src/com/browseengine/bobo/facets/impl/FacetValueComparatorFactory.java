package com.browseengine.bobo.facets.impl;

import java.util.Comparator;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FieldValueAccessor;

public class FacetValueComparatorFactory implements ComparatorFactory {

	public Comparator<Integer> newComparator(
			FieldValueAccessor fieldValueAccessor, int[] counts) {
		return new Comparator<Integer>(){
			public int compare(Integer o1, Integer o2) {
				return o2-o1;
			}			
		};
	}

	public Comparator<BrowseFacet> newComparator() {
		return new Comparator<BrowseFacet>(){
			public int compare(BrowseFacet o1, BrowseFacet o2) {				
				return o1.getValue().compareTo(o2.getValue());
			}	
		};
	}
}
