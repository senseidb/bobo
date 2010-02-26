/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  spackle
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * contact owner@browseengine.com.
 */

package com.browseengine.local.glue;

import java.io.IOException;
import java.util.BitSet;

import org.apache.lucene.index.IndexReader;

import com.browseengine.bobo.filter.CacheableFilter;
import com.browseengine.local.glue.GeoSearchFieldPlugin.GeoPluginFieldData;
import com.browseengine.local.service.Locatable;
import com.browseengine.local.service.LonLat;
import com.browseengine.local.service.geosearch.HaversineWrapper;
import com.browseengine.local.service.index.GeoSearchFields;

/**
 * For caching Geo Local Search result sets.
 * 
 * @author spackle
 *
 */
public class GeoSearchFilter extends CacheableFilter {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private transient GeoPluginFieldData _lonLats;
	private double _lonDegrees;
	private double _latDegrees;
	private float _rangeInMiles;
	
	public GeoSearchFilter(GeoPluginFieldData lonLat, double longitudeDegrees, double latitudeDegrees, float rangeInMiles) {
		_lonLats = lonLat;
		_lonDegrees = longitudeDegrees;
		_latDegrees = latitudeDegrees;
		_rangeInMiles = rangeInMiles;		
	}
	
	public GeoSearchFilter(GeoPluginFieldData lonLat, Locatable centroid, float rangeInMiles) {
		this(lonLat,centroid.getLongitudeDeg(),centroid.getLatitudeDeg(),rangeInMiles);
	}
	
	@Override
	public String getFieldName() {
		return _lonLats.fieldName;
	}

	@Override
	public String getFieldValue() {
		return new StringBuilder().
		  append('(').append(_lonDegrees).append(',').
		  append(_latDegrees).append(',').
		  append(_rangeInMiles).append(')').toString();
	}

	@Override
	public String getKey() {
		return new StringBuilder().
		  append(getFieldName()).append(':').
		  append(getFieldValue()).toString();
	}

	/**
	 * Broken at the poles.
	 * 
	 * NOTE that this isn't actually precise, since it means the result 
	 * fits in the box, not in the hypercircle.  notably, for all 
	 * valid results, bits.get(i) is true.  however, bits.get(i) might 
	 * be inside the box but ouside the radius of the search.
	 * 
	 * The ratio of correct answers to incorrect answers, if taken on 
	 * a flat plane assuming a circle rather than elipse, is somewhere 
	 * around PI*r^2/(2*r)^2 = PI/4 = 0.785.
	 * 
	 * We could also optionally compute a bit set within whose bounds 
	 * bits.get(i) implies it's a result, but !bits.get(i) doesn't 
	 * tell us if it's a result or not.  
	 * 
	 * However, the former gives us the option of refinement at scoring 
	 * time, in particular if the user has chosen to sort by distance.  
	 *
	 * But hit counts and appearance or disappearance of results 
	 * during browse might lead to confusion, and necessitate actual 
	 * result set inclusion at this step.  If this is the case, we 
	 * can use the inner and outer bounds as rules to only actually 
	 * compute distance here if it is between the inner box and outer 
	 * box.
	 */
	public BitSet makeBitSet(IndexReader reader) throws IOException {
		if (_rangeInMiles < 0f) {
			// all bits on by default
			int maxDoc = reader.maxDoc();
			BitSet bits = new BitSet(maxDoc);
			bits.set(0, maxDoc);
			return bits;
		}
		return makeBitSetFast(reader);
	}
	
	/**
	 * The fastest way to make the bit set.  The rule is that iff it is 
	 * a possible candidate for being a result, it is set to true.  
	 * This means that it just has to be inside the outer bounding "box" 
	 * created by the min/max lon/lat values that are possible as 
	 * resutls.  Hence the returned bit set represents a set that 
	 * contains every result, but some of whom may not be within the 
	 * true distance specified (we estimate this represents on 
	 * average less than 22% of the total set size).
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public BitSet makeBitSetFast(IndexReader reader) throws IOException {
		int maxDoc = reader.maxDoc();
		BitSet bits = new BitSet(maxDoc);
		Locatable centroid = LonLat.getLonLatDeg(_lonDegrees, _latDegrees);
		
		// outer box only
		int[] bounds = HaversineWrapper.computeLonLatMinMaxAsInt(centroid, _rangeInMiles);
		int minLon = bounds[HaversineWrapper.LON_MIN];
		int maxLon = bounds[HaversineWrapper.LON_MAX];
		int minLat = bounds[HaversineWrapper.LAT_MIN];
		int maxLat = bounds[HaversineWrapper.LAT_MAX];
		int lonAsInt;
		int latAsInt;
		for (int i = 0; i < maxDoc; i++) {
			lonAsInt = _lonLats.lons[i];
			latAsInt = _lonLats.lats[i];
			if (lonAsInt >= minLon && lonAsInt <= maxLon &&
					latAsInt >= minLat && latAsInt <= maxLat) {
				bits.set(i);
			}
		}
		return bits;
	}

	private static final double SQRT_TWO = Math.sqrt(2);
	
	/**
	 * Broken at the poles.  
	 * 
	 * A more accurate representation of the result set, 
	 * computed by using actual distance measures for everything outside an 
	 * inner bounding box, but inside the outer bounding box.  
	 * The improved accuracy comes at a performance hit when compared to 
	 * {@link #makeBitSetFast(IndexReader).
	 * 
	 * The inaccuracies 
	 * would come from an incorrect computation of the inner bounding box 
	 * (this should be improved upon if there's time--maybe just make it 
	 * a little smaller for added computation cost?).
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public BitSet makeBitSetMoreAccurate(IndexReader reader) throws IOException {
		int maxDoc = reader.maxDoc();
		BitSet bits = new BitSet(maxDoc);
		Locatable centroid = LonLat.getLonLatDeg(_lonDegrees, _latDegrees);
		
		// outer box
		int[] bounds = HaversineWrapper.computeLonLatMinMaxAsInt(centroid, _rangeInMiles);
		int minLon = bounds[HaversineWrapper.LON_MIN];
		int maxLon = bounds[HaversineWrapper.LON_MAX];
		int minLat = bounds[HaversineWrapper.LAT_MIN];
		int maxLat = bounds[HaversineWrapper.LAT_MAX];
		
		// inner box approximation, test all outside inner box
		int lonSpread = maxLon-minLon;
		lonSpread = (int)(Math.round(lonSpread/SQRT_TWO)/2);
		int latSpread = maxLat-minLat;
		latSpread = (int)(Math.round(latSpread/SQRT_TWO)/2);
		int lonAsInt = GeoSearchFields.dubToInt(_lonDegrees);
		int latAsInt = GeoSearchFields.dubToInt(_latDegrees);
		int iminLon = lonAsInt-lonSpread;
		int imaxLon = lonAsInt+lonSpread;
		int iminLat = latAsInt-latSpread;
		int imaxLat = latAsInt+latSpread;

		double centerLonRad = centroid.getLongitudeRad();
		double centerLatRad = centroid.getLatitudeRad();		
		
		for (int i = 0; i < maxDoc; i++) {
			lonAsInt = _lonLats.lons[i];
			latAsInt = _lonLats.lats[i];
			if (lonAsInt >= minLon && lonAsInt <= maxLon &&
					latAsInt >= minLat && latAsInt <= maxLat) {
				if (lonAsInt >= iminLon && lonAsInt <= imaxLon &&
						latAsInt >= iminLat && latAsInt <= imaxLat) {
					bits.set(i);
				} else if (HaversineWrapper.computeHaversineDistanceMiles(centerLonRad, centerLatRad, lonAsInt, latAsInt) <= _rangeInMiles) {
					bits.set(i);
				}
			}
		}
		return bits;

	}
	
	/**
	 * Broken at the poles.
	 * 
	 * Otherwise, this is an accurate representation of the true result set, but runs slower than 
	 * {@link #makeBitSetMoreAccurate(IndexReader)}.  It computes the actual distance for every 
	 * member in the set, and includes it iff it is within the bounds.
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public BitSet makeBitSetCompletelyAccurate(IndexReader reader) throws IOException {
		int maxDoc = reader.maxDoc();
		BitSet bits = new BitSet(maxDoc);
		Locatable centroid = LonLat.getLonLatDeg(_lonDegrees, _latDegrees);
		
		// outer box only
		int[] bounds = HaversineWrapper.computeLonLatMinMaxAsInt(centroid, _rangeInMiles);
		int minLon = bounds[HaversineWrapper.LON_MIN];
		int maxLon = bounds[HaversineWrapper.LON_MAX];
		int minLat = bounds[HaversineWrapper.LAT_MIN];
		int maxLat = bounds[HaversineWrapper.LAT_MAX];
		int lonAsInt;
		int latAsInt;

		double centerLonRad = centroid.getLongitudeRad();
		double centerLatRad = centroid.getLatitudeRad();		
		
		for (int i = 0; i < maxDoc; i++) {
			lonAsInt = _lonLats.lons[i];
			latAsInt = _lonLats.lats[i];
			if (lonAsInt >= minLon && lonAsInt <= maxLon &&
					latAsInt >= minLat && latAsInt <= maxLat) {
				if (HaversineWrapper.computeHaversineDistanceMiles(centerLonRad, centerLatRad, lonAsInt, latAsInt) <= _rangeInMiles) {
					bits.set(i);
				}
			}
		}
		return bits;
		
	}
}
