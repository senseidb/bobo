package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;

public class EmptyFilter extends RandomAccessFilter {
  private static EmptyFilter instance = new EmptyFilter();

  private EmptyFilter() {

  }

  @Override
  public double getFacetSelectivity(BoboSegmentReader reader) {
    return 0.0;
  }

  @Override
  public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboSegmentReader reader) throws IOException {
    return EmptyDocIdSet.getInstance();
  }

  public static EmptyFilter getInstance() {
    return instance;
  }

}
