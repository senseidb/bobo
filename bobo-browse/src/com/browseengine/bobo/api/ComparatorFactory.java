package com.browseengine.bobo.api;

import java.util.Comparator;

/**
 * Comparator for custom sorting a facet value
 * @author jwang
 */
public interface ComparatorFactory{
	/**
	 * Providers a Comparator from field values and counts. This is called within a browse.
	 * @param fieldValueAccessor accessor for field values
	 * @param counts hit counts
	 * @return Comparator instance
	 */
	Comparator<Integer> newComparator(FieldValueAccessor fieldValueAccessor,int[] counts);
	
	/**
	 * Providers a Comparator. This is called when doing a merge across browses.
	 * @return Comparator instance
	 */
	Comparator<BrowseFacet> newComparator();
}
