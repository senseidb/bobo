package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandler.FacetDataNone;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.sort.DocComparatorSource;

public class FilteredRangeFacetHandler extends FacetHandler<FacetDataNone> {
    private final List<String> _predefinedRanges;
    private final String _inner;
    private RangeFacetHandler _innerHandler;
    
	public FilteredRangeFacetHandler(String name, String underlyingHandler,List<String> predefinedRanges) {
		super(name, new HashSet<String>(Arrays.asList(underlyingHandler)));
		_predefinedRanges = predefinedRanges;
		_inner = underlyingHandler;
		_innerHandler = null;
	}

	@Override
	public RandomAccessFilter buildRandomAccessFilter(String value,
			Properties selectionProperty) throws IOException {
		return _innerHandler.buildRandomAccessFilter(value, selectionProperty);
	}

	
	@Override
	public RandomAccessFilter buildRandomAccessAndFilter(String[] vals,
			Properties prop) throws IOException {
		return _innerHandler.buildRandomAccessAndFilter(vals, prop);
	}

	@Override
	public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,
			Properties prop, boolean isNot) throws IOException {
		return _innerHandler.buildRandomAccessOrFilter(vals, prop, isNot);
	}

	@Override
	public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel,final FacetSpec fspec) {
		return new FacetCountCollectorSource() {
			
			@Override
			public FacetCountCollector getFacetCountCollector(BoboIndexReader reader,
					int docBase) {
				FacetDataCache dataCache = _innerHandler.getFacetData(reader);
				return new RangeFacetCountCollector(_name, dataCache,docBase, fspec, _predefinedRanges);
			}
		};
		
	}

	@Override
	public String[] getFieldValues(BoboIndexReader reader,int id) {
		return _innerHandler.getFieldValues(reader, id);
	}
	
	@Override
	public Object[] getRawFieldValues(BoboIndexReader reader,int id){
		return _innerHandler.getRawFieldValues(reader,id);
	}

	@Override
	public DocComparatorSource getDocComparatorSource() {
		return _innerHandler.getDocComparatorSource();
	}

	@Override
	public FacetDataNone load(BoboIndexReader reader) throws IOException {
		FacetHandler<?> handler = reader.getFacetHandler(_inner);
		if (handler instanceof RangeFacetHandler){
			_innerHandler = (RangeFacetHandler)handler;
			return FacetDataNone.instance;
		}
		else{
			throw new IOException("inner handler is not instance of "+RangeFacetHandler.class);
		}
	}
}
