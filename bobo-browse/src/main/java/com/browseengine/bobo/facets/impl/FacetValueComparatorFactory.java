package com.browseengine.bobo.facets.impl;

import java.util.Comparator;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FieldValueAccessor;
import com.browseengine.bobo.util.BigSegmentedArray;
import com.browseengine.bobo.util.IntBoundedPriorityQueue.IntComparator;

public class FacetValueComparatorFactory implements ComparatorFactory {

  @Override
  public IntComparator newComparator(FieldValueAccessor fieldValueAccessor, BigSegmentedArray counts) {
    return new IntComparator() {
      @Override
      public int compare(Integer o1, Integer o2) {
        return o2 - o1;
      }

      // use polymorphism to avoid auto-boxing
      @Override
      public int compare(int o1, int o2) {
        return o2 - o1;
      }
    };
  }

  @Override
  public Comparator<BrowseFacet> newComparator() {
    return new Comparator<BrowseFacet>() {
      @Override
      public int compare(BrowseFacet o1, BrowseFacet o2) {
        return o1.getValue().compareTo(o2.getValue());
      }
    };
  }
}
