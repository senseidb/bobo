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

package com.browseengine.local.service.index;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.browseengine.local.service.Conversions;

/**
 * @author spackle
 *
 */
public enum GeoSearchFields {
	LON("lon"),
	LAT("lat"),
	ADDRESS("address"),
	// traditionally searchable fields
	NAME("name"),
	DESCRIPTION("description"),
	// 10-digit U.S. phone numbers, for now
	PHONE("phone");

	private String _field;
	
	private GeoSearchFields(String field) {
		_field = field;
	}
	
	public String getField() {
		return _field;
	}

	private static Pattern INDEXED_PHONE_PATTERN = Pattern.compile("\\A\\d{1,10}\\z");
	public static boolean validIndexedPhoneNumber(String str) {
		if (null != str) {
			Matcher m = INDEXED_PHONE_PATTERN.matcher(str);
			return m.matches();
		} 
		return false;
	}
	
	
	private static final int NUM_DECIMALS = 7;
	private static final int DUB_TO_INT = (int)Math.pow(10, NUM_DECIMALS);
	public static String dubToStr(double dub) {
		// sign, 3 digits, decimal, NUM_DECIMALS decimals
		char[] res = new char[1+3+1+NUM_DECIMALS];
		String str = Integer.toString(dubToInt(dub));
		boolean negative = str.charAt(0) == '-';
		if (negative) {
			str = str.substring(1);
		}
		// write out the decimals
		for (int i = res.length-1; i > 4; i--) {
			res[i] = str.length() >= res.length-i ? str.charAt(i-res.length+str.length()) : '0';
		}
		res[4] = '.';
		for (int i = 3; i > 0; i--) {
			res[i] = str.length() > NUM_DECIMALS+3-i ? str.charAt(i-4+str.length()) : '0';
		}
		res[0] = negative ? '-' : '0';
		return new String(res);
	}
	public static double strToDub(String str) {
		return Double.parseDouble(str);
	}
	public static int dubToInt(double dub) {
		return (int)Math.round(dub*DUB_TO_INT);
	}
	public static double intToDub(int asint) {
		return ((double)asint)/DUB_TO_INT;
	}
	public static int strToInt(String str) {
		return dubToInt(strToDub(str));
	}
	public static double intToRad(int asint) {
		return Conversions.d2r(intToDub(asint));
	}
	public static int radToInt(double radians) {
		return dubToInt(Conversions.r2d(radians));
	}
}
