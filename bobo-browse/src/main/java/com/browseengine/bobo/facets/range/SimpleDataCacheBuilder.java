package com.browseengine.bobo.facets.range;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.filter.AdaptiveFacetFilter.FacetDataCacheBuilder;

public class SimpleDataCacheBuilder implements FacetDataCacheBuilder {
  private final String name;
  private final String indexFieldName;

  public SimpleDataCacheBuilder(String name, String indexFieldName) {
    this.name = name;
    this.indexFieldName = indexFieldName;
  }

  @Override
  public FacetDataCache<?> build(BoboSegmentReader reader) {
    return (FacetDataCache<?>) reader.getFacetData(name);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getIndexFieldName() {
    return indexFieldName;
  }
}
