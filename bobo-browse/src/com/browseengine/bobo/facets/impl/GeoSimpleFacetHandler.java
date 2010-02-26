/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.browseengine.bobo.facets.FacetHandlerFactory;
import com.browseengine.bobo.facets.RuntimeFacetHandler;
import com.browseengine.bobo.facets.FacetHandler.FacetDataNone;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.sort.DocComparator;
import com.browseengine.bobo.sort.DocComparatorSource;

/**
 * @author nnarkhed
 *
 */

public abstract class GeoSimpleFacetHandler extends RuntimeFacetHandler<FacetDataNone> implements FacetHandlerFactory<GeoSimpleFacetHandler> {

	private static Logger logger = Logger.getLogger(RangeFacetHandler.class);
	protected final String _latFacetName;
	protected final String _longFacetName;
	protected RangeFacetHandler _latFacetHandler;
	protected RangeFacetHandler _longFacetHandler;
	
	public GeoSimpleFacetHandler(String name, String latFacetName, String longFacetName) {
		super(name, new HashSet<String>(Arrays.asList(new String[]{latFacetName, longFacetName})));
		_latFacetName = latFacetName;
		_longFacetName = longFacetName;
	}

	protected abstract String buildLatRangeString(String val);
	protected abstract String buildLongRangeString(String val);
	protected abstract List<String> buildAllRangeStrings(String[] values);
	protected abstract String getValueFromRangeString(String rangeString);
	public abstract GeoSimpleFacetHandler newInstance();
	
	@Override
	public RandomAccessFilter buildRandomAccessFilter(String val, Properties props) throws IOException {
		RandomAccessFilter latFilter = _latFacetHandler.buildRandomAccessFilter(buildLatRangeString(val), props);
		RandomAccessFilter longFilter = _longFacetHandler.buildRandomAccessFilter(buildLongRangeString(val), props);		
		return new RandomAccessAndFilter(Arrays.asList(new RandomAccessFilter[]{latFilter, longFilter}));
	}

	@Override
	public RandomAccessFilter buildRandomAccessAndFilter(String[] vals, Properties props) throws IOException {
		List<String> latValList = new ArrayList<String>(vals.length);
		List<String> longValList = new ArrayList<String>(vals.length);
		for(String val: vals) {
			latValList.add(buildLatRangeString(val));
			longValList.add(buildLongRangeString(val));
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
			latValList.add(buildLatRangeString(val));
			longValList.add(buildLongRangeString(val));
		}
		RandomAccessFilter latFilter = _latFacetHandler.buildRandomAccessOrFilter(latValList.toArray(new String[latValList.size()]), props, isNot);
		RandomAccessFilter longFilter = _longFacetHandler.buildRandomAccessOrFilter(longValList.toArray(new String[longValList.size()]), props, isNot);		
		return new RandomAccessAndFilter(Arrays.asList(new RandomAccessFilter[]{latFilter, longFilter}));
	}

	@Override
	public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel,final FacetSpec fspec) {
		return new FacetCountCollectorSource() {
			
		    final List<String> list = buildAllRangeStrings(sel.getValues());
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
	
	public static String[] getRangeStrings(String rangeString)
	{
		// rangeString looks like <latitude, longitude, radius>
		int index=rangeString.indexOf('<');
		int index2=rangeString.indexOf('>');

		String range;
		float latitude, longitude, radius;
		try{
			range = rangeString.substring(index+1, index2).trim();
			String[] values = range.split(",");
			// values[0] is latitude, values[1] is longitude, values[2] is radius
			latitude = Float.parseFloat(values[0]);
			longitude = Float.parseFloat(values[1]);
			radius = Float.parseFloat(values[2]);
			
			float latStart = latitude - radius;
			float latEnd = latitude + radius;
			float longStart = longitude - radius;
			float longEnd = longitude + radius;
			
			return new String[]{String.valueOf(latStart), String.valueOf(latEnd), String.valueOf(longStart), String.valueOf(longEnd)};
		}
		catch(RuntimeException re){
			logger.error("problem parsing range string: "+rangeString+":"+re.getMessage(),re);
			throw re;
		}
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
//			final BigSegmentedArray orderArray = dataCache.;                                                                 
			return new DocComparator() {                                                                                               

				@Override                                                                                                                 
				public Comparable value(ScoreDoc doc) {
					return 1;                                                                                   
//					int index = orderArray.get(doc.doc);                                                                                     
//					return dataCache.valArray.get(index);                                                                              
				}                                                                                                                         

				@Override                                                                                                                 
				public int compare(ScoreDoc doc1, ScoreDoc doc2) {                                                                        
//					return orderArray.get(doc1.doc) - orderArray.get(doc2.doc);
					return 0;
				}                                                                                                                         
			};                                                                                                                         
		}
	}
}
