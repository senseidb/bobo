/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2006  Spackle
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
 * please go to https://sourceforge.net/projects/bobo-browse/.
 */

package com.browseengine.local.service;

import java.io.Serializable;

/**
 * Represents a longitude/latitude point, in degrees.
 * immutable.
 * 
 * @author spackle
 *
 */
public class LonLat implements Locatable, Serializable {
	private static final long serialVersionUID = 1L;

	private double lon;
	private double lat;

	/**
	 * lon/lat point.  lon and lat are both in degrees.
	 * @param lon
	 * @param lat
	 */
	private LonLat(double lon, double lat) {
		this.lat = lat;
		this.lon = lon;
	}

	public static LonLat getLonLatDeg(double lonDegrees, double latDegrees) {
		return new LonLat(lonDegrees, latDegrees);
	}
	
	public static LonLat getLonLatRad(double lonRadians, double latRadians) {
		return new LonLat(Conversions.r2d(lonRadians), Conversions.r2d(latRadians));
	}
	
	public LonLat moveDeg(double lonDisplacementDeg, double latDisplacementDeg) {
		return new LonLat(lon+lonDisplacementDeg, lat+latDisplacementDeg);
	}
	
	public LonLat moveRad(double lonDisplacementRad, double latDisplacementRad) {
		return new LonLat(lon+Conversions.r2d(lonDisplacementRad), lat+Conversions.r2d(latDisplacementRad));
	}
	
	public double getLongitudeDeg() {
		return lon;
	}
	public double getLatitudeDeg() {
		return lat;
	}
	public final double getLongitudeRad() {
		return Conversions.d2r(getLongitudeDeg());
	}	
	public final double getLatitudeRad() {
		return Conversions.d2r(getLatitudeDeg());
	}
	public String toString() {
		return new StringBuilder().append("lon(").append(lon).
			append("),lat(").append(lat).append(')').toString();
	}
}
