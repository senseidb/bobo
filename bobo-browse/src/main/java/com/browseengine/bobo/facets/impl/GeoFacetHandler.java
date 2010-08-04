/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.Arrays;
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
import com.browseengine.bobo.facets.filter.GeoFacetFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.impl.GeoFacetCountCollector.GeoRange;
import com.browseengine.bobo.sort.DocComparatorSource;
import com.browseengine.bobo.util.BigFloatArray;
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
		super(name);
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
      super(name);
      _latFieldName = latFieldName;
      _lonFieldName = lonFieldName;
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
			if(reader == null) throw new NullPointerException("reader object is null");
			if(latFieldName == null) throw new NullPointerException("latitude Field Name is null");
			if(lonFieldName == null) throw new NullPointerException("longitude Field Name is null");
			
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
			
			Term latTerm = new Term(latFieldName, "");
			TermDocs termDocs = reader.termDocs(latTerm);
			TermEnum termEnum = reader.terms(latTerm);
			
			float docLat, docLon;
			int termCount = 1;
			String lonValue = null;
			int length = maxDoc+1;
			int doc;
			termDocs.next();
			do {
			  Term term = termEnum.term();
			  if(term == null || !term.field().equals(latFieldName)) break;

			  if(termCount > xVals.capacity()) throw new IOException("Maximum number of values cannot exceed: " + xVals.capacity());
			  if(termCount >= length) throw new RuntimeException("There are more terms than documents in field " + latFieldName + " or " + lonFieldName + 
			  ", but its impossible to sort on tokenized fields");

			  // pull the termDocs to point to the document for the current term in the termEnum
			  termDocs.seek(termEnum);
			  while(termDocs.next())
			  {
			    doc = termDocs.doc();

			    // read the latitude value in the current document
			    docLat = Float.parseFloat(term.text().trim());
			    // read the longitude value in the current document
			    Document docVal = reader.document(doc, null);
			    lonValue = docVal.get(lonFieldName);
			    if(lonValue == null)
			      continue;
			    else
			      docLon = Float.parseFloat(lonValue);

			    // convert the lat, lon values to x,y,z coordinates
			    float[] coords = GeoMatchUtil.geoMatchCoordsFromDegrees(docLat, docLon);
			    _xValArray.add(doc, coords[0]);
			    _yValArray.add(doc, coords[1]);
			    _zValArray.add(doc, coords[2]);
			  }
			}while(termEnum.next());
			if(termDocs != null)
			  termDocs.close();
			if(termEnum != null)
			  termEnum.close();
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
