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
 * please go to https://sourceforge.net/projects/bobo-browse/.
 */
package com.browseengine.local.service;

import java.io.Serializable;

/**
 * Represents a physical address on the earth.
 * immutable.
 * 
 * @author spackle
 *
 */
public class Address implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String number;
	private String streetPrefix;
	private String streetName;
	private String streetType;
	private String streetSuffix;
	private String aptNo;
	private String city;
	private String state;
	private int zip5;
	private String country;
	
	public static final int NO_ZIP5 = -1;

	public Address(String number, String streetPrefix, 
			String streetName, String streetType, 
			String suffix, String aptNo, String city, 
			String state, int zip5, 
			String country) {
		this.number = number != null ? number.trim() : null;
		this.streetPrefix = streetPrefix != null ? streetPrefix.trim() : null;
		this.streetName = streetName != null ? streetName.trim() : null;
		this.streetType = streetType != null ? streetType.trim() : null;
		this.streetSuffix = suffix != null ? suffix.trim() : null;
		this.aptNo = aptNo != null ? aptNo.trim() : null;
		this.city = city != null ? city.trim() : null;
		this.state = state != null ? state.trim() : null;
		this.zip5 = zip5;
		this.country = country != null ? country.trim() : null;
	}
	
	public String getStreetSuffix() {
		return streetSuffix;
	}
	public String getAptNo() {
		return aptNo;
	}
	public String getCity() {
		return city;
	}
	public String getCountry() {
		return country;
	}
	public String getNumber() {
		return number;
	}
	public String getState() {
		return state;
	}
	public String getStreetName() {
		return streetName;
	}
	public String getStreetPrefix() {
		return streetPrefix;
	}
	public String getStreetType() {
		return streetType;
	}
	public int getZip5() {
		return zip5;
	}

	public String toString() {
		return
		"number: "+number+
		", prefix: "+streetPrefix+
		", streetName: "+streetName+
		", type: "+streetType+
		", suffix: "+streetSuffix+
		", apt: "+aptNo+
		", city: "+city+
		", state: "+state+
		", zip: "+zip5+
		", country: "+country;
	}
}
