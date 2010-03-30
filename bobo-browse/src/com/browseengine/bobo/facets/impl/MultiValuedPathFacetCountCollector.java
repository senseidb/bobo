package com.browseengine.bobo.facets.impl;

import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.util.BigNestedIntArray;

public class MultiValuedPathFacetCountCollector extends PathFacetCountCollector {

    private final BigNestedIntArray _array;
    
	public MultiValuedPathFacetCountCollector(String name, String sep,
			BrowseSelection sel, FacetSpec ospec, FacetDataCache dataCache) {
		super(name, sep, sel, ospec, dataCache);
		_array = ((MultiValueFacetDataCache)(dataCache))._nestedArray;
	}

	@Override
    public final void collect(int docid) 
    {
      _array.countNoReturn(docid, _count);
    }

    @Override
    public final void collectAll()
    {
      _count = _dataCache.freqs;
    }
}
