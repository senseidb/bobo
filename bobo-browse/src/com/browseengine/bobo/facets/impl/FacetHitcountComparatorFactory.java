package com.browseengine.bobo.facets.impl;

import java.util.Comparator;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FieldValueAccessor;

public class FacetHitcountComparatorFactory implements ComparatorFactory {
	public Comparator<Integer> newComparator(FieldValueAccessor valueList,
			final int[] counts) {
		return new Comparator<Integer>(){

			public int compare(Integer f1, Integer f2) {
				int val = counts[f1] - counts[f2];
				if (val==0)
		        {
		            val=f2-f1;
		        }
		        return val;
			}
			
		};
	}
	
	public static Comparator<BrowseFacet> FACET_HITS_COMPARATOR = new Comparator<BrowseFacet>(){

		public int compare(BrowseFacet f1, BrowseFacet f2) {
			int val = f2.getHitCount() - f1.getHitCount();
			if (val==0)
	        {
	            val=f1.getValue().compareTo(f2.getValue());
	        }
	        return val;
		}		
	};

	public Comparator<BrowseFacet> newComparator() {
		return FACET_HITS_COMPARATOR;
	}
}
