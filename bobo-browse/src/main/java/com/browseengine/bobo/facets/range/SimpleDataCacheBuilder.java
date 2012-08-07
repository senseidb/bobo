package com.browseengine.bobo.facets.range;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.filter.AdaptiveFacetFilter.FacetDataCacheBuilder;

public class SimpleDataCacheBuilder implements FacetDataCacheBuilder{
  private String name;
  private String indexFieldName;

  public SimpleDataCacheBuilder(String name, String indexFieldName) {
    this.name = name;
    this.indexFieldName = indexFieldName;
  }

  public FacetDataCache build(BoboIndexReader reader) {
    return (FacetDataCache) reader.getFacetData(name);
  }

  public String getName() {
    return name;
  }

  public String getIndexFieldName() {
    return indexFieldName;
  }
}
