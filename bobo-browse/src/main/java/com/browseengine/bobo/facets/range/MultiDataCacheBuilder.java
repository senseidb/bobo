package com.browseengine.bobo.facets.range;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.facets.filter.AdaptiveFacetFilter.FacetDataCacheBuilder;

public class MultiDataCacheBuilder implements FacetDataCacheBuilder{
  private final String name;
  private final String indexFieldName;

  public MultiDataCacheBuilder(String name, String indexFieldName) {
    this.name = name;
    this.indexFieldName = indexFieldName;
  }

  @Override
  public MultiValueFacetDataCache<?> build(BoboSegmentReader reader) {
    return (MultiValueFacetDataCache<?>) reader.getFacetData(name);
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
