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
 * Represents a named entity on the earth, which has a 
 * defined longitude and latitude, and which might have a 
 * physical address associated with it (or it could be 
 * a point in the desert ;0).
 * immutable.
 * 
 * @author spackle
 *
 */
public class LocatedAddress implements Locatable, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * longitude, in degrees, + means E, - means W, 
	 * values are on (-180,180].
	 */
	private double lon;
	/**
	 * latitude, in degrees, + means N, - means S, 
	 * values are on [-90,90].
	 */
	private double lat;
	private Address address;
	
	public LocatedAddress(Address address, 
			double lon, double lat) {
		this.address = address;
		this.lat = lat;
		this.lon = lon;
	}
	
	public double getLatitudeDeg() {
		return lat;
	}
	public double getLongitudeDeg() {
		return lon;
	}
	public final double getLongitudeRad() {
		return Conversions.d2r(getLongitudeDeg());
	}	
	public final double getLatitudeRad() {
		return Conversions.d2r(getLatitudeDeg());
	}

	/**
	 * a resource may or may not have an address associated with it.
	 * @return
	 */
	public Address getAddress() {
		return address;
	}
	
	public String toString() {
		return "address: ["+getAddress()+
		"], degrees longitude: "+getLongitudeDeg()+
		", degrees latitude: "+getLatitudeDeg();
	}
}
