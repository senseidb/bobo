package com.browseengine.bobo.facets.range;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.facets.filter.AdaptiveFacetFilter.FacetDataCacheBuilder;

public class MultiDataCacheBuilder implements FacetDataCacheBuilder{
  private String name;
  private String indexFieldName;

  public MultiDataCacheBuilder(String name, String indexFieldName) {
    this.name = name;
    this.indexFieldName = indexFieldName;
  }

  public MultiValueFacetDataCache build(BoboIndexReader reader) {
    return (MultiValueFacetDataCache) reader.getFacetData(name);
  }

  public String getName() {
    return name;
  }

  public String getIndexFieldName() {
    return indexFieldName;
  }
}
