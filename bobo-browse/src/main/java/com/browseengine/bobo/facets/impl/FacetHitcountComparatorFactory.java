package com.browseengine.bobo.facets.impl;

import java.util.Comparator;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FieldValueAccessor;
import com.browseengine.bobo.util.BigSegmentedArray;
import com.browseengine.bobo.util.IntBoundedPriorityQueue.IntComparator;

public class FacetHitcountComparatorFactory implements ComparatorFactory {
  @Override
  public IntComparator newComparator(FieldValueAccessor valueList, final BigSegmentedArray counts) {

    return new IntComparator() {

      @Override
      public int compare(Integer f1, Integer f2) {
        int val = counts.get(f1) - counts.get(f2);
        if (val == 0) {
          val = f2 - f1;
        }
        return val;
      }

      // use ploymorphism to avoid auto-boxing
      @Override
      public int compare(int f1, int f2) {
        int val = counts.get(f1) - counts.get(f2);
        if (val == 0) {
          val = f2 - f1;
        }
        return val;
      }

    };
  }

  public static final Comparator<BrowseFacet> FACET_HITS_COMPARATOR = new Comparator<BrowseFacet>() {
    @Override
    public int compare(BrowseFacet f1, BrowseFacet f2) {
      int val = f2.getFacetValueHitCount() - f1.getFacetValueHitCount();
      if (val == 0) {
        val = f1.getValue().compareTo(f2.getValue());
      }
      return val;
    }
  };

  @Override
  public Comparator<BrowseFacet> newComparator() {
    return FACET_HITS_COMPARATOR;
  }
}
