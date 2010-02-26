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

package com.browseengine.local.service.geosearch;

import com.browseengine.local.service.Locatable;
import com.browseengine.local.service.index.GeoSearchFields;

/**
 * @author spackle
 *
 */
public class HaversineWrapper {
	public static final int LON_MIN = 0;
	public static final int LON_MAX = 1;
	public static final int LAT_MIN = 2;
	public static final int LAT_MAX = 3;
	
	public static int[] computeLonLatMinMaxAsInt(Locatable point, float radius) {
		int[] bounds = new int[4];

		double dlon = HaversineFormula.computeLonBoundary(point.getLongitudeRad(), point.getLatitudeRad(), radius);
		double dlat = HaversineFormula.computeLatBoundary(point.getLongitudeRad(), point.getLatitudeRad(), radius);
		double lonmin = point.getLongitudeRad()-dlon;
		double lonmax = point.getLongitudeRad()+dlon;
		double latmin = point.getLatitudeRad()-dlat;
		double latmax = point.getLatitudeRad()+dlat;
		bounds[LON_MIN] = GeoSearchFields.radToInt(lonmin);
		bounds[LON_MAX] = GeoSearchFields.radToInt(lonmax);
		bounds[LAT_MIN] = GeoSearchFields.radToInt(latmin);
		bounds[LAT_MAX] = GeoSearchFields.radToInt(latmax);
		
		return bounds;
	}
	
	public static float computeHaversineDistanceMiles(double lon1rad, double lat1rad, int lon2AsInt, int lat2AsInt) {
		double lon2 = GeoSearchFields.intToRad(lon2AsInt);
		double lat2 = GeoSearchFields.intToRad(lat2AsInt);
		return HaversineFormula.computeHaversineDistanceMiles(lon1rad, lat1rad, lon2, lat2);
	}
}
