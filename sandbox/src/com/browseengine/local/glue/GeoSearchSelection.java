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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.browseengine.local.service.Conversions;

/**
 * A parsed selection, of a geosearch field.
 * 
 * @author spackle
 *
 */
public class GeoSearchSelection {
	private double _lon;
	private double _lat;
	private float _rangeInMiles;
	private static final Pattern KILOMETER_PATTERN = Pattern.compile("k[mi](lometer)?", Pattern.CASE_INSENSITIVE);
	private static final Pattern PARSE_GEO_SEARCH = 
		Pattern.compile("\\A\\(\\s*(-?\\d+\\.?\\d*)\\s*,\\s*(-?\\d+\\.?\\d*)\\s*\\)\\s*:\\s*(\\d+\\.?\\d*)\\s*(\\p{Alpha}*)\\s*\\z");
		
	private GeoSearchSelection(double lon, double lat, float rangeInMiles) {
		_lon = lon;
		_lat = lat;
		_rangeInMiles = rangeInMiles;
	}
	
	public double getLon() {
		return _lon;
	}
	public double getLat() {
		return _lat;
	}
	public float getRangeInMiles() {
		return _rangeInMiles;
	}
	
	public static GeoSearchSelection[] parse(String[] vals) {
		double[] lons = null, lats = null;
		float[] rangesInMiles = null;
		int successCount = 0;
		try {
			if (vals != null && vals.length > 0) {
				lons = new double[vals.length];
				lats = new double[vals.length];
				rangesInMiles = new float[vals.length];
				for (int i = 0; i < vals.length; i++) {
					Matcher m = GeoSearchSelection.PARSE_GEO_SEARCH.matcher(vals[i]);
					if (m.find(0)) {
						String lonStr = m.group(1);
						String latStr = m.group(2);
						String rangeStr = m.group(3);
						String unitsStr = m.group(4);
						lons[i] = Double.parseDouble(lonStr);
						lats[i] = Double.parseDouble(latStr);
						rangesInMiles[i] = Float.parseFloat(rangeStr);
						if (unitsStr != null && unitsStr.length() >= 2) {
							m = GeoSearchSelection.KILOMETER_PATTERN.matcher(unitsStr);
							if (m.find(0)) {
								rangesInMiles[i] = Conversions.mi2km(rangesInMiles[i]);
							}
						}
						successCount++;
					}
				}
			} else {
				successCount--;
			}
		} catch (NumberFormatException nfe) {
			// will report bad args
		} catch (NullPointerException npe) {
			// will report bad args
		}
		if (successCount > 0 && successCount != vals.length) {
			return null;
		}
		GeoSearchSelection[] sels = new GeoSearchSelection[lons.length];
		for (int i = 0; i < lons.length; i++) {
			sels[i] = new GeoSearchSelection(lons[i], lats[i], rangesInMiles[i]);
		}
		return sels;
	}
}
