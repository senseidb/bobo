/**
 * 
 */
package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.impl.GeoSimpleFacetHandler;
import com.browseengine.bobo.util.BigSegmentedArray;

/**
 * @author nnarkhed
 *
 */
public final class GeoSimpleFacetFilter extends RandomAccessFilter {

	private static final long serialVersionUID = 1L;
	private final FacetHandler<FacetDataCache> _latFacetHandler;
	private final FacetHandler<FacetDataCache> _longFacetHandler;
	private final String _latRangeString;
	private final String _longRangeString;

	/**
	 * @param latHandler
	 * @param longHandler
	 * @param latRangeString
	 * @param longRangeString
	 */
	public GeoSimpleFacetFilter(FacetHandler<FacetDataCache> latHandler, FacetHandler<FacetDataCache> longHandler, String latRangeString, String longRangeString) {
		_latFacetHandler = latHandler;
		_longFacetHandler = longHandler;
		_latRangeString = latRangeString;
		_longRangeString = longRangeString;
	}

	private final static class GeoSimpleDocIdSetIterator extends DocIdSetIterator {
		private int _doc = -1;
		private int _totalFreq;
		private int _minID = Integer.MAX_VALUE;
		private int _maxID = -1;
		private final int _latStart;
		private final int _latEnd;
		private final int _longStart;
		private final int _longEnd;
		private final BigSegmentedArray _latOrderArray;
		private final BigSegmentedArray _longOrderArray;
		
		GeoSimpleDocIdSetIterator(int latStart, int latEnd, int longStart, int longEnd, FacetDataCache latDataCache, FacetDataCache longDataCache) {
			_totalFreq = 0;
			_latStart = latStart;
			_longStart = longStart;
			_latEnd = latEnd;
			_longEnd = longEnd;
			for(int i = latStart;i <= latEnd; ++i) {
				_minID = Math.min(_minID, latDataCache.minIDs[i]);
				_maxID = Math.max(_maxID, latDataCache.maxIDs[i]);				
			}
			for(int i = longStart;i <= longEnd; ++i) {
				_minID = Math.min(_minID, longDataCache.minIDs[i]);
				_maxID = Math.max(_maxID, longDataCache.maxIDs[i]);				
			}
			_doc = Math.max(-1, _minID-1);
			_latOrderArray = latDataCache.orderArray;
			_longOrderArray = longDataCache.orderArray;
		}
		
		@Override
		final public int docID() {
			return _doc;
		}
		
		@Override
		final public int nextDoc() throws IOException {
			int latIndex;
			int longIndex;
			while(_doc < _maxID) {	//not yet reached end
				latIndex = _latOrderArray.get(++_doc);
				longIndex = _latOrderArray.get(_doc);
				if((latIndex >= _latStart && latIndex <= _latEnd) && (longIndex >= _longStart && longIndex <= _longEnd)) 
					return _doc;
			}
			return DocIdSetIterator.NO_MORE_DOCS;
		}
		
		@Override
		final public int advance(int id) throws IOException {
			if(_doc < id) {
				_doc = id - 1;
			}
			int latIndex;
			int longIndex;
			while(_doc < _maxID) {	//not yet reached end
				latIndex = _latOrderArray.get(++_doc);
				longIndex = _latOrderArray.get(_doc);
				if((latIndex >= _latStart && latIndex <= _latEnd) && (longIndex >= _longStart && longIndex <= _longEnd)) 
					return _doc;
			}
			return DocIdSetIterator.NO_MORE_DOCS;			
		}
	}

	@Override
	public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboIndexReader reader)
			throws IOException {
		final FacetDataCache latDataCache = _latFacetHandler.getFacetData(reader);
		final FacetDataCache longDataCache = _longFacetHandler.getFacetData(reader);
		
		final int[] latRange = FacetRangeFilter.parse(latDataCache, _latRangeString);
		final int[] longRange = FacetRangeFilter.parse(longDataCache, _longRangeString);
		if((latRange == null) || (longRange == null)) return null;
		
		return new RandomAccessDocIdSet() {
			int _latStart = latRange[0];
			int _latEnd = latRange[1];
			int _longStart = longRange[0];
			int _longEnd = longRange[1];
			
			@Override
			final public boolean get(int docid) {
				int latIndex = latDataCache.orderArray.get(docid);
				int longIndex = longDataCache.orderArray.get(docid);
				return latIndex >= _latStart && latIndex <= _latEnd && longIndex >= _longStart && longIndex <= _longEnd;
			}
			
			@Override
			public DocIdSetIterator iterator() {
				return new GeoSimpleDocIdSetIterator(_latStart, _latEnd, _longStart, _longEnd, latDataCache, longDataCache);
			}
		};
	}
	
	public static int[] parse(FacetDataCache latDataCache, FacetDataCache longDataCache, String rangeString)
	{
		GeoSimpleFacetHandler.GeoLatLonRange range = GeoSimpleFacetHandler.GeoLatLonRange.parse(rangeString);
		// ranges[0] is latRangeStart, ranges[1] is latRangeEnd, ranges[2] is longRangeStart, ranges[3] is longRangeEnd
	    String latLower = String.valueOf(range.latStart);
	    String latUpper = String.valueOf(range.latEnd);
	    String longLower = String.valueOf(range.lonStart);
	    String longUpper = String.valueOf(range.lonEnd);
	    
	    int latStart,latEnd,longStart,longEnd;
	    if (latLower == null)
	    	latStart = 1;
	    else
	    {
	    	latStart = latDataCache.valArray.indexOf(latLower);
	    	if (latStart < 0)
	    	{
	    		latStart = -(latStart + 1);
	    	}
	    }
	    
	    if(longLower == null)
	    	longStart = 1;
	    else
	    {
	    	longStart = longDataCache.valArray.indexOf(longLower);
	    	if (longStart < 0)
	    	{
	    		longStart = -(longStart + 1);
	    	}
	    }

	    if (latUpper==null)
	    {
	    	latEnd = latDataCache.valArray.size()-1;
	    }
	    else
	    {
	    	latEnd = latDataCache.valArray.indexOf(latUpper);
	    	if (latEnd<0)
	    	{
	    		latEnd = -(latEnd + 1);
	    		latEnd = Math.max(0, latEnd-1);
	    	}
	    }

	    if (longUpper==null)
	    {
	    	longEnd = longDataCache.valArray.size()-1;
	    }
	    else
	    {
	    	longEnd = longDataCache.valArray.indexOf(longUpper);
	    	if (longEnd<0)
	    	{
	    		longEnd = -(longEnd + 1);
	    		longEnd = Math.max(0, longEnd-1);
	    	}
	    }

	    return new int[]{latStart, latEnd, longStart, longEnd};
	}

}
