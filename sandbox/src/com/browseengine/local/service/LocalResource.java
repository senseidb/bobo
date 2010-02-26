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

package com.browseengine.local.service;


/**
 * @author spackle
 *
 */
public class LocalResource implements Locatable {
	private LonLat _lonLat;
	private String _name;
	private long _phone;
	private String _description;
	private String _addressStr;
	private float _distance;
	
	public static final long NO_PHONE_NUMBER = -1L;
	
	public LocalResource(String name, String description, String addressStr, long phoneNumber, double lon, double lat) {
		this(name,description,addressStr,phoneNumber,lon,lat,-1f);
	}
	
	public LocalResource(String name, String description, String addressStr, long phoneNumber, double lon, double lat, float distance) {
		_lonLat = LonLat.getLonLatDeg(lon, lat);
		_name = name;
		_description = description;
		_addressStr = addressStr;
		_phone = phoneNumber;
		_distance = distance;
	}
	
	public float getDistanceInMiles() {
		return _distance;
	}
	
	public float getDistanceInKM() {
		return Conversions.mi2km(_distance);
	}

	public String getDescription() {
		return _description;
	}
	
	public String getName() {
		return _name;
	}

	public String getPrettyPhoneNumber() {
		if (_phone > 0L && _phone < 10000000000L) {
			char[] chars = new char[13];
			chars[0] = '(';
			int tmp = (int)(_phone/10000000L);
			int cnt = 0;
			while (cnt < 3) {
				chars[3-cnt] = (char)(tmp%10+'0');
				tmp /= 10;
				cnt++;
			}
			chars[4] = ')';
			int rest = (int)(_phone%10000000L);
			tmp = rest/10000;
			cnt = 0;
			while (cnt < 3) {
				chars[7-cnt] = (char)(tmp%10+'0');
				tmp /= 10;
				cnt++;
			}
			chars[8] = '-';
			rest = rest%10000;
			tmp = rest;
			cnt = 0;
			while (cnt < 4) {
				chars[12-cnt] = (char)(tmp%10+'0');
				tmp /= 10;
				cnt++;
			}
			return new String(chars);
		}
		return null;
	}
	
	public String getPhoneNumber() {
		if (_phone > 0L && _phone < 10000000000L) {
			return ""+_phone;
		}
		return null;
	}
	
	public String getAddressStr() {
		return _addressStr;
	}
	
	public double getLatitudeDeg() {
		return _lonLat.getLatitudeDeg();
	}

	public double getLatitudeRad() {
		return _lonLat.getLatitudeRad();
	}

	public double getLongitudeDeg() {
		return _lonLat.getLongitudeDeg();
	}

	public double getLongitudeRad() {
		return _lonLat.getLongitudeRad();
	}

	public String toString() {
		float dist;
		return new StringBuilder().append("resource(").
		append((dist = getDistanceInMiles()) >= 0f ? "distanceInMiles: "+dist+", " : "").
		append("name: ").append(getName()).
		append(", description: ").append(getDescription()).
		append(", address: ").append(getAddressStr()).
		append(", phone: ").append(getPrettyPhoneNumber()).
		append(", (").append(_lonLat.toString()).
		append(")").
		toString();
	}
}
