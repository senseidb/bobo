/**
 * 
 */
package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.impl.GeoFacetHandler.GeoFacetData;
import com.browseengine.bobo.util.BigFloatArray;
import com.browseengine.bobo.util.GeoMatchUtil;

/**
 * @author nnarkhed
 *
 */
public class GeoFacetFilter extends RandomAccessFilter{

	private static final long serialVersionUID = 1L;
	private final FacetHandler<GeoFacetData> _handler;
	private final float _lat;
	private final float _lon;
	private final float _rad;
	
	/**
	 * @param  facetHandler The Geo Facet Handler for this instance
	 * @param  lat latitude value of the user's point of interest
	 * @param  lon longitude value of the user's point of interest
	 * @param  radius Radius from the point of interest
	 */
	public GeoFacetFilter(FacetHandler<GeoFacetData> facetHandler, float lat, float lon, float radius) {
		_handler = facetHandler;
		_lat = lat;
		_lon = lon;
		_rad = radius;
	}

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.facets.filter.RandomAccessFilter#getRandomAccessDocIdSet(com.browseengine.bobo.api.BoboIndexReader)
	 */
	@Override
	public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboIndexReader reader)
			throws IOException {
		int maxDoc = reader.maxDoc();

		final GeoFacetData dataCache = _handler.getFacetData(reader);
		return new GeoDocIdSet(dataCache.get_xValArray(), dataCache.get_yValArray(), dataCache.get_zValArray(),
				_lat, _lon, _rad, maxDoc);
	}

	private static final class GeoDocIdSet extends RandomAccessDocIdSet {
		private final BigFloatArray _xvals;
		private final BigFloatArray _yvals;
		private final BigFloatArray _zvals;
		private final float _radius;
		private final float _targetX;
		private final float _targetY;
		private final float _targetZ;
		private final float _delta;
		private final int _maxDoc;
		
		/**
		 * 
		 * @param xvals array of x coordinate values for docid
		 * @param yvals array of y coordinate values for docid
		 * @param zvals array of z coordinate values for docid
		 * @param lat target latitude 
		 * @param lon target longitude
		 * @param radius target radius
		 * @param maxdoc max doc in the docid set
		 */
		GeoDocIdSet(final BigFloatArray xvals, final BigFloatArray yvals, final BigFloatArray zvals, final float lat, final float lon, 
				final float radius, final int maxdoc) {
			_xvals = xvals;
			_yvals = yvals;
			_zvals = zvals;
			_radius = GeoMatchUtil.getMilesRadiusCosine(radius);
			float[] coords = GeoMatchUtil.geoMatchCoordsFromDegrees(lat, lon);
			_targetX = coords[0];
			_targetY = coords[1];
			_targetZ = coords[2];
			_delta = (float)(radius/GeoMatchUtil.EARTH_RADIUS_MILES);
			_maxDoc = maxdoc;
		}
		
		@Override
		public boolean get(int docid) {
			float docX = _xvals.get(docid);
			float docY = _yvals.get(docid);
			float docZ = _zvals.get(docid);
			
			return inCircle(docX, docY, docZ, _targetX, _targetY, _targetZ, _radius);
		}
		
		@Override
		public DocIdSetIterator iterator() {
			return new GeoDocIdSetIterator(_xvals, _yvals, _zvals, _targetX, _targetY, _targetZ, _delta, _radius, _maxDoc);
		}
	}
	
	private static class GeoDocIdSetIterator extends DocIdSetIterator {
		private final BigFloatArray _xvals;
		private final BigFloatArray _yvals;
		private final BigFloatArray _zvals;
		private final float _radius;
		private final float _targetX;
		private final float _targetY;
		private final float _targetZ;
		private final float _delta;
		private final int _maxDoc;
		private int _doc;
		
		GeoDocIdSetIterator(BigFloatArray xvals, BigFloatArray yvals, BigFloatArray zvals, float targetX, float targetY, float targetZ, 
				float delta, float radiusCosine, int maxdoc) {
			_xvals = xvals;
			_yvals = yvals;
			_zvals = zvals;
			_targetX = targetX;
			_targetY = targetY;
			_targetZ = targetZ;
			_delta = delta;
			_radius = radiusCosine;
			_maxDoc = maxdoc;
			_doc = -1;
		}
		
		@Override
		final public int docID() {
			return _doc;
		}
		
		@Override 
		final public int nextDoc() throws IOException {
			final float x = _targetX;
			final float xu = x + _delta;
			final float xl = x - _delta;
			final float y = _targetY;
			final float yu = y + _delta;
			final float yl = y - _delta;
			final float z = _targetZ;
			final float zu = z + _delta;
			final float zl = z - _delta;
			
			int docid = _doc;
			while(docid < _maxDoc) {
				docid++;
				
				float docX = _xvals.get(docid);
				if(docX > xu || docX < xl) continue;
				
				float docY = _yvals.get(docid);
				if(docY > yu || docY < yl) continue;
				
				float docZ = _zvals.get(docid);
				if(docZ > zu || docZ < zl) continue;
				
				if(GeoFacetFilter.inCircle(docX, docY, docZ, _targetX, _targetY, _targetZ, _radius)) {
					_doc = docid;
					return _doc;
				}
			}
			_doc = DocIdSetIterator.NO_MORE_DOCS;
			return DocIdSetIterator.NO_MORE_DOCS;
		}
		
		@Override
		final public int advance(int targetId) throws IOException {
			if(_doc < targetId) 
				_doc = targetId - 1;

			final float x = _targetX;
			final float xu = x + _delta;
			final float xl = x - _delta;
			final float y = _targetY;
			final float yu = y + _delta;
			final float yl = y - _delta;
			final float z = _targetZ;
			final float zu = z + _delta;
			final float zl = z - _delta;
			
			int docid = _doc;
			while(docid < _maxDoc) {
				docid++;
				
				float docX = _xvals.get(docid);
				if(docX > xu || docX < xl) continue;
				
				float docY = _yvals.get(docid);
				if(docY > yu || docY < yl) continue;
				
				float docZ = _zvals.get(docid);
				if(docZ > zu || docZ < zl) continue;
				
				if(GeoFacetFilter.inCircle(docX, docY, docZ, _targetX, _targetY, _targetZ, _radius)) {
					_doc = docid;
					return _doc;
				}				
			}
			_doc = docid;
			return _doc;
		}
	}
	
	public static boolean inCircle(float docX, float docY, float docZ, float targetX, float targetY, float targetZ, float radCosine) {
		if(docX == -1.0f && docY == -1.0f && docZ == -1.0f) 
			return false;
		float dotProductCosine = (docX * targetX) + (docY * targetY) + (docZ * targetZ);
		return (radCosine <= dotProductCosine);
	}
}
