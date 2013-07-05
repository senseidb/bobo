package com.browseengine.bobo.facets.data;

import com.browseengine.bobo.api.BoboSegmentReader;

public interface FacetDataFetcher {

  public Object fetch(BoboSegmentReader reader, int doc);

  public void cleanup(BoboSegmentReader reader);
}
