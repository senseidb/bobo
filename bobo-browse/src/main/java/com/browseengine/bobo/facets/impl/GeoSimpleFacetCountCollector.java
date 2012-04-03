/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermStringList;
import com.browseengine.bobo.facets.filter.FacetRangeFilter;
import com.browseengine.bobo.facets.filter.GeoSimpleFacetFilter;
import com.browseengine.bobo.util.BigSegmentedArray;

/**
 * @author nnarkhed
 *
 */
public class GeoSimpleFacetCountCollector implements FacetCountCollector {

  private static final Logger log = Logger.getLogger(GeoSimpleFacetCountCollector.class.getName());
	private final FacetSpec _spec;
	private final String _name;
	private int[] _latCount;
	private int[] _longCount;
	private final BigSegmentedArray _latOrderArray;
	private FacetDataCache _latDataCache;
	private final TermStringList _predefinedRanges;
	private int[][] _latPredefinedRangeIndexes;
	private final BigSegmentedArray _longOrderArray;
	private FacetDataCache _longDataCache;
	private int[][] _longPredefinedRangeIndexes;
	private int _docBase;

	protected GeoSimpleFacetCountCollector(String name, FacetDataCache latDataCache, FacetDataCache longDataCache, int docBase, FacetSpec spec, List<String> predefinedRanges) {
		_name = name;
		_latDataCache = latDataCache;
		_longDataCache = longDataCache;
		_latCount = new int[_latDataCache.freqs.length];
		_longCount = new int[_longDataCache.freqs.length];
		log.info("latCount: " + _latDataCache.freqs.length + " longCount: " + _longDataCache.freqs.length);
		_latOrderArray = _latDataCache.orderArray;
		_longOrderArray = _longDataCache.orderArray;
		_docBase = docBase;
		_spec = spec;
		_predefinedRanges = new TermStringList();
		Collections.sort(predefinedRanges);
		_predefinedRanges.addAll(predefinedRanges);
		
		if(predefinedRanges != null) {
			_latPredefinedRangeIndexes = new int[_predefinedRanges.size()][2];
			_longPredefinedRangeIndexes = new int[_predefinedRanges.size()][2];
			int i = 0;
			for(String range: _predefinedRanges) {
				int[] ranges = GeoSimpleFacetFilter.parse(_latDataCache, _longDataCache, range);
				_latPredefinedRangeIndexes[i][0] = ranges[0];   // latStart 
				_latPredefinedRangeIndexes[i][1] = ranges[1];   // latEnd
				_longPredefinedRangeIndexes[i][0] = ranges[2];  // longStart
				_longPredefinedRangeIndexes[i][1] = ranges[3];  // longEnd
				i++;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.browseengine.bobo.facets.FacetCountCollector#collect(int)
	 */
	public void collect(int docid) {
		// increment the count only if both latitude and longitude ranges are true for a particular docid
		for(int[] range: _latPredefinedRangeIndexes) {
			int latValue = _latOrderArray.get(docid);
			int longValue = _longOrderArray.get(docid);
			int latStart = range[0];
			int latEnd = range[1];
			if(latValue >= latStart && latValue <= latEnd) {
				for(int[] longRange: _longPredefinedRangeIndexes) {
					int longStart = longRange[0];
					int longEnd = longRange[1];
					if(longValue >= longStart && longValue <= longEnd) {
		          		_latCount[_latOrderArray.get(docid)]++;		
		          		_longCount[_longOrderArray.get(docid)]++;
					}
				}
          	}
		}
	}

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.facets.FacetCountCollector#collectAll()
	 */
	public void collectAll() {
		_latCount = _latDataCache.freqs;
		_longCount = _longDataCache.freqs;
	}

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.facets.FacetCountCollector#getCountDistribution()
	 */
	public int[] getCountDistribution() {
		int[] dist = null;
		if(_latPredefinedRangeIndexes != null) {
			dist = new int[_latPredefinedRangeIndexes.length];
			int n = 0;
			int start;
			int end;
			for(int[] range: _latPredefinedRangeIndexes) {
				start = range[0];
				end = range[1];
				int sum = 0;
				for(int i = start; i < end; i++) {
					sum += _latCount[i];
				}
				dist[n++] = sum;
			}
		}
		return dist;
	}

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.facets.FacetCountCollector#getName()
	 */
	public String getName() {
		return _name;
	}

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetAccessible#getFacet(java.lang.String)
	 */
	public BrowseFacet getFacet(String value) {
		BrowseFacet facet = null;
		int[] range = FacetRangeFilter.parse(_latDataCache, value);
		
		if(range != null) {
			int sum = 0;
			for(int i = range[0]; i <= range[1]; ++i) {
				sum += _latCount[i];
			}
			facet = new BrowseFacet(value, sum);
		}
		return facet;
	}

  public int getFacetHitsCount(Object value) 
  {
    int[] range = FacetRangeFilter.parse(_latDataCache, (String)value);

    if(range != null)
    {
      int sum = 0;
      for(int i = range[0]; i <= range[1]; ++i)
      {
        sum += _latCount[i];
      }
      return sum;
    }
    return 0;
  }

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetAccessible#getFacets()
	 */
	public List<BrowseFacet> getFacets() {
		if (_spec!=null){
			if (_latPredefinedRangeIndexes!=null)
			{
				int minCount=_spec.getMinHitCount();
				int[] rangeCounts = new int[_latPredefinedRangeIndexes.length];
				for (int i=0;i<_latCount.length;++i){
					if (_latCount[i] >0 ){
						for (int k=0;k<_latPredefinedRangeIndexes.length;++k)
						{
							if (i>=_latPredefinedRangeIndexes[k][0] && i<=_latPredefinedRangeIndexes[k][1])
							{
								rangeCounts[k]+=_latCount[i];
							}
						}
					}
				}
				List<BrowseFacet> list=new ArrayList<BrowseFacet>(rangeCounts.length);
				for (int i=0;i<rangeCounts.length;++i)
				{
					if (rangeCounts[i]>=minCount)
					{
						BrowseFacet choice=new BrowseFacet();
						choice.setHitCount(rangeCounts[i]);
						choice.setValue(_predefinedRanges.get(i));
						list.add(choice);
					}
				}
				return list;
			}
			else
			{
				return FacetCountCollector.EMPTY_FACET_LIST;
			}
		}
		else
		{
			return FacetCountCollector.EMPTY_FACET_LIST;
		}
	}

	public void close()
	{
		// TODO Auto-generated method stub

	}
	
	public FacetIterator iterator() {
		// each range is of the form <lat, lon, radius>
		int[] rangeCounts = new int[_latPredefinedRangeIndexes.length];
		for (int i=0;i<_latCount.length;++i){
			if (_latCount[i] >0 ){
				for (int k=0;k<_latPredefinedRangeIndexes.length;++k)
				{
					if (i>=_latPredefinedRangeIndexes[k][0] && i<=_latPredefinedRangeIndexes[k][1])
					{
						rangeCounts[k]+=_latCount[i];
					}
				}
			}
		}
		return new DefaultFacetIterator(_predefinedRanges, rangeCounts, rangeCounts.length, true);
	}	
}
