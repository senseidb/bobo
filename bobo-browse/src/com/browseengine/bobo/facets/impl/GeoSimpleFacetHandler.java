/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandler.FacetDataNone;
import com.browseengine.bobo.facets.RuntimeFacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.impl.GeoFacetCountCollector.GeoRange;
import com.browseengine.bobo.sort.DocComparator;
import com.browseengine.bobo.sort.DocComparatorSource;

/**
 * @author nnarkhed
 *
 */

public class GeoSimpleFacetHandler extends RuntimeFacetHandler<FacetDataNone> {

	private static Logger logger = Logger.getLogger(RangeFacetHandler.class);
	protected final String _latFacetName;
	protected final String _longFacetName;
	protected RangeFacetHandler _latFacetHandler;
	protected RangeFacetHandler _longFacetHandler;
	
	public static class GeoLatLonRange{
		public final String latRange;
		public final String lonRange;
		public final float latStart;
		public final float latEnd;
		public final float lonStart;
		public final float lonEnd;
		public final float radius;
		
		private GeoLatLonRange(String latRange,String lonRange,float latStart,float latEnd,float lonStart,float lonEnd,float radius){
			this.latRange = latRange;
			this.lonRange = lonRange;
			this.latStart = latStart;
			this.latEnd = latEnd;
			this.lonStart = lonStart;
			this.lonEnd = lonEnd;
			this.radius = radius;
		}
		
		public static GeoLatLonRange parse(String val){
			GeoRange range = GeoFacetCountCollector.parse(val);
			float latStart = range.getLat() - range.getRad();
			float latEnd = range.getLat() + range.getRad();
			float lonStart = range.getLon() - range.getRad();
			float lonEnd = range.getLon() + range.getRad();
			
			StringBuilder buf = new StringBuilder();
			buf.append("[").append(String.valueOf(latStart)).append(" TO ").append(String.valueOf(latEnd)).append("]");
			String latRange = buf.toString();
			
			buf = new StringBuilder();
			buf.append("[").append(String.valueOf(lonStart)).append(" TO ").append(String.valueOf(lonEnd)).append("]");
			String lonRange = buf.toString();
			
			return new GeoLatLonRange(latRange,lonRange,latStart,latEnd,lonStart,lonEnd,range.getRad());
		}
	}
	
	public GeoSimpleFacetHandler(String name, String latFacetName, String longFacetName) {
		super(name, new HashSet<String>(Arrays.asList(new String[]{latFacetName, longFacetName})));
		_latFacetName = latFacetName;
		_longFacetName = longFacetName;
	}

	@Override
	public RandomAccessFilter buildRandomAccessFilter(String val, Properties props) throws IOException {
		GeoLatLonRange range = GeoLatLonRange.parse(val);
		
		RandomAccessFilter latFilter = _latFacetHandler.buildRandomAccessFilter(range.latRange, props);
		RandomAccessFilter longFilter = _longFacetHandler.buildRandomAccessFilter(range.lonRange, props);		
		return new RandomAccessAndFilter(Arrays.asList(new RandomAccessFilter[]{latFilter, longFilter}));
	}

	@Override
	public RandomAccessFilter buildRandomAccessAndFilter(String[] vals, Properties props) throws IOException {
		List<String> latValList = new ArrayList<String>(vals.length);
		List<String> longValList = new ArrayList<String>(vals.length);
		for(String val: vals) {
			GeoLatLonRange range = GeoLatLonRange.parse(val);
			latValList.add(range.latRange);
			longValList.add(range.lonRange);
		}
		RandomAccessFilter latFilter = _latFacetHandler.buildRandomAccessAndFilter(latValList.toArray(new String[latValList.size()]), props);
		RandomAccessFilter longFilter = _longFacetHandler.buildRandomAccessAndFilter(longValList.toArray(new String[longValList.size()]), props);		
		return new RandomAccessAndFilter(Arrays.asList(new RandomAccessFilter[]{latFilter, longFilter}));
	}
	
	@Override
	public RandomAccessFilter buildRandomAccessOrFilter(String[] vals, Properties props, boolean isNot) throws IOException {
		List<String> latValList = new ArrayList<String>(vals.length);
		List<String> longValList = new ArrayList<String>(vals.length);
		for(String val: vals) {
			GeoLatLonRange range = GeoLatLonRange.parse(val);
			latValList.add(range.latRange);
			longValList.add(range.lonRange);
		}
		RandomAccessFilter latFilter = _latFacetHandler.buildRandomAccessOrFilter(latValList.toArray(new String[latValList.size()]), props, isNot);
		RandomAccessFilter longFilter = _longFacetHandler.buildRandomAccessOrFilter(longValList.toArray(new String[longValList.size()]), props, isNot);		
		return new RandomAccessAndFilter(Arrays.asList(new RandomAccessFilter[]{latFilter, longFilter}));
	}

	private static List<String> buildAllRangeStrings(String[] values) {
		if(values == null)  return Collections.EMPTY_LIST;
		List<String> ranges = new ArrayList<String>(values.length);
		String[] range = null;
		for(String value: values) {
			ranges.add(value);
		}
		return ranges;
	}
	
	@Override
	public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel,final FacetSpec fspec) {

		final List<String> list = buildAllRangeStrings(sel.getValues());
		return new FacetCountCollectorSource() {
		    // every string in the above list is of the form <latitude, longitude, radius>, which can be interpreted by GeoSimpleFacetCountCollector
			@Override
			public FacetCountCollector getFacetCountCollector(BoboIndexReader reader,
					int docBase) {
				FacetDataCache latDataCache = _latFacetHandler.getFacetData(reader);
				FacetDataCache longDataCache = _longFacetHandler.getFacetData(reader);
				return new GeoSimpleFacetCountCollector(_name, latDataCache, longDataCache, docBase, fspec, list);
			}
		};
		
	}
	
	@Override
	public String[] getFieldValues(BoboIndexReader reader,int docid)
	{
		String[] latValues = _latFacetHandler.getFieldValues(reader,docid);
		String[] longValues = _longFacetHandler.getFieldValues(reader, docid);
		String[] allValues = new String[latValues.length + longValues.length];
		int index = 0;
		for(String value: latValues) {
			allValues[index++] = value;
		}
		for(String value: longValues) {
			allValues[index++] = value;
		}
		return allValues;
	}

	@Override
	public Object[] getRawFieldValues(BoboIndexReader reader,int docid)
	{
		Object[] latValues = _latFacetHandler.getRawFieldValues(reader,docid);
		Object[] longValues = _longFacetHandler.getRawFieldValues(reader, docid);
		Object[] allValues = new Object[latValues.length + longValues.length];
		int index = 0;
		for(Object value: latValues) {
			allValues[index++] = value;
		}
		for(Object value: longValues) {
			allValues[index++] = value;
		}
		return allValues;
	}

	@Override
	public FacetDataNone load(BoboIndexReader reader) throws IOException
	{
		_latFacetHandler = (RangeFacetHandler)getDependedFacetHandler(_latFacetName);
		_longFacetHandler = (RangeFacetHandler)getDependedFacetHandler(_longFacetName);
		return FacetDataNone.instance;
	}

	@Override
	public DocComparatorSource getDocComparatorSource() {
		return new GeoFacetDocComparatorSource(this);
	}
	
	public static class GeoFacetDocComparatorSource extends DocComparatorSource{                                                    
		private FacetHandler<FacetDataNone> _facetHandler;
		public GeoFacetDocComparatorSource(GeoSimpleFacetHandler geoSimpleFacetHandler) {
			_facetHandler = geoSimpleFacetHandler; 
		}

		@Override                                                                                                                   
		public DocComparator getComparator(IndexReader reader, int docbase)                                                         
		throws IOException {                                                                                                      
			if (!(reader instanceof BoboIndexReader)) throw new IllegalStateException("reader not instance of "+BoboIndexReader.class);
			BoboIndexReader boboReader = (BoboIndexReader)reader;                                                                      
			final FacetDataNone dataCache = _facetHandler.getFacetData((BoboIndexReader) reader);                                                 
			return new DocComparator() {                                                                                               

				@Override                                                                                                                 
				public Comparable value(ScoreDoc doc) {
					return 1;                                                                                   
				}                                                                                                                         

				@Override                                                                                                                 
				public int compare(ScoreDoc doc1, ScoreDoc doc2) {                                                                        
					return 0;
				}                                                                                                                         
			};                                                                                                                         
		}
	}
}
