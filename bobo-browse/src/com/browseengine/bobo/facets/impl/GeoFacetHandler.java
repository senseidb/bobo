/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermValueList;
import com.browseengine.bobo.facets.filter.GeoFacetFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.impl.GeoFacetCountCollector.GeoRange;
import com.browseengine.bobo.sort.DocComparatorSource;
import com.browseengine.bobo.util.BigFloatArray;
import com.browseengine.bobo.util.BigSegmentedArray;
import com.browseengine.bobo.util.GeoMatchUtil;

/**
 * @author nnarkhed
 *
 */

/** Constructor for GeoFacetHandler
 * @param name - name of the Geo facet
 *
 */

public class GeoFacetHandler extends FacetHandler<GeoFacetHandler.GeoFacetData> {
	
	private static Logger logger = Logger.getLogger(GeoFacetHandler.class);
	private String _latFieldName;
	private String _lonFieldName;
	// variable to specify if the geo distance calculations are in miles. Default is miles
	private boolean _miles;
	
	public GeoFacetHandler(String name, String latFieldName, String lonFieldName) {
		super(name,new HashSet<String>(Arrays.asList(new String[]{latFieldName,lonFieldName})));
		_latFieldName = latFieldName;
		_lonFieldName = lonFieldName;
		_miles = true;
	}
	
	/**
	 * Constructor for GeoFacetHandler
	 * @param name         name of the geo facet
	 * @param latFieldName name of the index field that stores the latitude value
	 * @param lonFieldName name of the index field that stores the longitude value
	 * @param miles        variable to specify if the geo distance calculations are in miles. False indicates distance calculation is in kilometers
	 */
    public GeoFacetHandler(String name, String latFieldName, String lonFieldName, boolean miles) {
      this(name,latFieldName,lonFieldName);
      _miles = miles;
    }
    
	public static class GeoFacetData {
		private BigFloatArray _xValArray;
		private BigFloatArray _yValArray;
		private BigFloatArray _zValArray;
	
		public GeoFacetData() {
			_xValArray = null;
			_yValArray = null;
			_zValArray = null;
		}
		
		public GeoFacetData(BigFloatArray xvals, BigFloatArray yvals, BigFloatArray zvals) {
			_xValArray = xvals;
			_yValArray = yvals;
			_zValArray = zvals;
		}
		
		public static BigFloatArray newInstance(int maxDoc) {
			BigFloatArray array = new BigFloatArray(maxDoc);
			array.ensureCapacity(maxDoc);
			return array;
		}
		
		/**
		 * @return the _xValArray
		 */
		public BigFloatArray get_xValArray() {
			return _xValArray;
		}

		/**
		 * @param xValArray the _xValArray to set
		 */
		public void set_xValArray(BigFloatArray xValArray) {
			_xValArray = xValArray;
		}

		/**
		 * @return the _yValArray
		 */
		public BigFloatArray get_yValArray() {
			return _yValArray;
		}

		/**
		 * @param yValArray the _yValArray to set
		 */
		public void set_yValArray(BigFloatArray yValArray) {
			_yValArray = yValArray;
		}

		/**
		 * @return the _zValArray
		 */
		public BigFloatArray get_zValArray() {
			return _zValArray;
		}

		/**
		 * @param zValArray the _zValArray to set
		 */
		public void set_zValArray(BigFloatArray zValArray) {
			_zValArray = zValArray;
		}

		public void load(String latFieldName, String lonFieldName, BoboIndexReader reader) throws IOException {
			if(reader == null) throw new IOException("reader object is null");
			
			FacetDataCache<?> latCache = (FacetDataCache<?>)reader.getFacetData(latFieldName);
			FacetDataCache<?> lonCache = (FacetDataCache<?>)reader.getFacetData(lonFieldName);
			
			int maxDoc = reader.maxDoc();
			
			BigFloatArray xVals = this._xValArray;
			BigFloatArray yVals = this._yValArray;
			BigFloatArray zVals = this._zValArray;
			
			if(xVals == null) 
				xVals = newInstance(maxDoc);
			else
				xVals.ensureCapacity(maxDoc);
			if(yVals == null)
				yVals = newInstance(maxDoc);
			else
				yVals.ensureCapacity(maxDoc);
			if(zVals == null)
				zVals = newInstance(maxDoc);
			else
				zVals.ensureCapacity(maxDoc);
			
			this._xValArray = xVals;
			this._yValArray = yVals;
			this._zValArray = zVals;
			
			BigSegmentedArray latOrderArray = latCache.orderArray;
			TermValueList<?> latValList = latCache.valArray;

			BigSegmentedArray lonOrderArray = lonCache.orderArray;
			TermValueList<?> lonValList = lonCache.valArray;
			
			for (int i=0;i<maxDoc;++i){
				String docLatString = latValList.get(latOrderArray.get(i)).trim();
				String docLonString = lonValList.get(lonOrderArray.get(i)).trim();

				float docLat = 0;
				if (docLatString.length() > 0){
					docLat = Float.parseFloat(docLatString);
				}
				
				float docLon = 0;
				if (docLonString.length() > 0){
					docLon = Float.parseFloat(docLonString);
				}
				
				float[] coords = GeoMatchUtil.geoMatchCoordsFromDegrees(docLat,docLon);
			    _xValArray.add(i, coords[0]);
			    _yValArray.add(i, coords[1]);
			    _zValArray.add(i, coords[2]);
			}
		}
	}
	
	/**
	 * Builds a random access filter.
	 * @param value Should be of the form: lat, lon: rad
	 * @param selectionProperty
	 */
	@Override
	public RandomAccessFilter buildRandomAccessFilter(String value,
			Properties selectionProperty) throws IOException {
		GeoRange range = GeoFacetCountCollector.parse(value);
		return new GeoFacetFilter(this, range.getLat(), range.getLon(), range.getRad(), _miles);
	}

	@Override
	public DocComparatorSource getDocComparatorSource() {
		throw new UnsupportedOperationException("Doc comparator not yet supported for Geo Facets");
	}

	@Override
	public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel, final FacetSpec fspec) {
		return new FacetCountCollectorSource() {
			final List<String> ranges = Arrays.asList(sel.getValues());
			
			@Override
			public FacetCountCollector getFacetCountCollector(BoboIndexReader reader, int docBase) {
				GeoFacetData dataCache = getFacetData(reader);
				return new GeoFacetCountCollector(_name, dataCache, docBase, fspec, ranges, _miles);
			}		
		};
	}

	@Override
	public String[] getFieldValues(BoboIndexReader reader, int id) {
		GeoFacetData dataCache = getFacetData(reader);
		BigFloatArray xvals = dataCache.get_xValArray();
		BigFloatArray yvals = dataCache.get_yValArray();
		BigFloatArray zvals = dataCache.get_zValArray();
		
		float xvalue = xvals.get(id);
		float yvalue = yvals.get(id);
		float zvalue = zvals.get(id);
		float lat = GeoMatchUtil.getMatchLatDegreesFromXYZCoords(xvalue, yvalue, zvalue);
		float lon = GeoMatchUtil.getMatchLonDegreesFromXYZCoords(xvalue, yvalue, zvalue);
		
		String[] fieldValues = new String[2];
		fieldValues[0] = String.valueOf(lat);
		fieldValues[1] = String.valueOf(lon);
		return fieldValues;
	}

	@Override
	public GeoFacetData load(BoboIndexReader reader) throws IOException {
	    GeoFacetData dataCache = new GeoFacetData();
		dataCache.load(_latFieldName, _lonFieldName, reader);
		return dataCache;
	}
	
}
