package com.browseengine.bobo.facets.filter;

import com.browseengine.bobo.facets.data.FacetDataCache;

public interface FacetValueConverter {
  public static FacetValueConverter DEFAULT = new DefaultFacetDataCacheConverter();

  int[] convert(FacetDataCache<String> dataCache, String[] vals);

  public static class DefaultFacetDataCacheConverter implements FacetValueConverter {
    public DefaultFacetDataCacheConverter() {

    }

    @Override
    public int[] convert(FacetDataCache<String> dataCache, String[] vals) {
      return FacetDataCache.convert(dataCache, vals);
    }
  }
}
