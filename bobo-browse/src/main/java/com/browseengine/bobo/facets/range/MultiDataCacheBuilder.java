package com.browseengine.bobo.facets.range;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.facets.filter.AdaptiveFacetFilter.FacetDataCacheBuilder;

public class MultiDataCacheBuilder implements FacetDataCacheBuilder{
  private String name;
  public MultiDataCacheBuilder( String name) {      
    this.name = name;
    
  }
  public MultiValueFacetDataCache build(BoboIndexReader reader) {
    return (MultiValueFacetDataCache) reader.getFacetData(name);
  }
  public String getName() {
    return name;
  }   
}