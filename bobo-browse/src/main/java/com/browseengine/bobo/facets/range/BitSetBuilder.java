package com.browseengine.bobo.facets.range;

import org.apache.lucene.util.OpenBitSet;

import com.browseengine.bobo.facets.data.FacetDataCache;

public interface BitSetBuilder {
  OpenBitSet bitSet(FacetDataCache dataCache);
}
