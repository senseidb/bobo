package com.browseengine.bobo.facets.filter;

import com.browseengine.bobo.facets.data.FacetDataCache;

public interface FacetValueConverter {
	public static FacetValueConverter DEFAULT = new DefaultFacetDataCacheConverter();
	int[] convert(FacetDataCache dataCache,String[] vals);
	
	public static class DefaultFacetDataCacheConverter implements FacetValueConverter{		
		public DefaultFacetDataCacheConverter(){
			
		}
		public int[] convert(FacetDataCache dataCache,String[] vals){
			return FacetDataCache.convert(dataCache, vals);
		}
	}
}
