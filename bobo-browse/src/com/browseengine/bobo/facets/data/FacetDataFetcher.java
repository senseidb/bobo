package com.browseengine.bobo.facets.data;

import com.browseengine.bobo.api.BoboIndexReader;

public interface FacetDataFetcher
{
  public Object fetch(BoboIndexReader reader, int doc);

  public void cleanup(BoboIndexReader reader);
}
