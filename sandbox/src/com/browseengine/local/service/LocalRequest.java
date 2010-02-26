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

/**
 * Represents a request to search the {@link LocalService}.
 * A request to the {@link LocalService} must include at 
 * least one of a <code>point</code>, a parsed <code>address</code>, 
 * or an <code>addressStr</code>.  If multiple apear, the one that 
 * is used as the centroid of the local search is chosen in 
 * that preference order: point, address, then query.  If 
 * any is null, the latter is used instead.  Note that for a 
 * query, it is free-form, and the <code>LocalService</code>
 * must determine and automatically extract the address 
 * portions of the input query.
 * 
 * @author spackle
 *
 */
public class LocalRequest {
	private transient boolean mutable = true;
	private String addressStr;
	private Address parsedAddress;
	private Locatable point;
	/**
	 * range, in miles.
	 */
	private float range;

	public LocalRequest() {
		//
	}
	
	public LocalRequest(LocalRequest request) {
		// float is primitive
		setRangeInMiles(request.getRangeInMiles());
		// Strings are immutable
		setAddressStr(request.getAddressStr());
		// Addresses are immutable
		setAddress(request.getAddress());
		// clone the locatable, with the info. we need for local search
		Locatable locatable = request.getPoint();
		if (null != locatable) {
			LonLat lonLat = LonLat.getLonLatDeg(locatable.getLongitudeDeg(), locatable.getLatitudeDeg());
			setPoint(lonLat);
		}
		markImmutable();
	}
	
	public Locatable getPoint() {
		return point;
	}
	public void setPoint(Locatable point) {
		if (mutable) 
			this.point = point;
	}
	public void setAddress(Address parsedAddress) {
		if (mutable) 
			this.parsedAddress = parsedAddress;
	}
	public Address getAddress() {
		return parsedAddress;
	}
	public String getAddressStr() {
		return addressStr;
	}
	public void setAddressStr(String addressStr) {
		if (mutable) 
			this.addressStr = addressStr;
	}
	public float getRangeInMiles() {
		return range;
	}
	public void setRangeInMiles(float range) {
		if (mutable) 
			this.range = range;
	}
	public void markImmutable() {
		this.mutable = false;
	}
}
