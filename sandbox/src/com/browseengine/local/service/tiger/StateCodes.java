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

package com.browseengine.local.service.tiger;

/**
 * FIPS code FIPS 5-2, the state code for U.S. states/territories.
 * 
 * @author spackle
 *
 */
public class StateCodes {
	private static final String[] STATE_NAMES = {
		"Alabama",
		"Alaska",
		"Arizona",
		"Arkansas",
		"California",
		"Colorado",
		"Connecticut",
		"Delaware",
		"District of Columbia",
		"Florida",
		"Georgia",
		"Hawaii",
		"Idaho",
		"Illinois",
		"Indiana",
		"Iowa",
		"Kansas",
		"Kentucky",
		"Louisiana",
		"Maine",
		"Maryland",
		"Massachusetts",
		"Michigan",
		"Minnesota",
		"Mississippi",
		"Missouri",
		"Montana",
		"Nebraska",
		"Nevada",
		"New Hampshire",
		"New Jersey",
		"New Mexico",
		"New York",
		"North Carolina",
		"North Dakota",
		"Ohio",
		"Oklahoma",
		"Oregon",
		"Pennsylvania",
		"Rhode Island",
		"South Carolina",
		"South Dakota",
		"Tennessee",
		"Texas",
		"Utah",
		"Vermont",
		"Virginia",
		"Washington",
		"West Virginia",
		"Wisconsin",
		"Wyoming",
		"American Samoa",
		"Federated States of Micronesia",
		"Guam",
		"Marshall Islands",
		"Northern Mariana Islands",
		"Palau",
		"Puerto Rico",
		"U.S. Minor Outlying Islands",
		"Virgin Islands of the U.S.",
	};

	private static final String[] STATE_CODES = {
		"01",
		"02",
		"04",
		"05",
		"06",
		"08",
		"09",
		"10",
		"11",
		"12",
		"13",
		"15",
		"16",
		"17",
		"18",
		"19",
		"20",
		"21",
		"22",
		"23",
		"24",
		"25",
		"26",
		"27",
		"28",
		"29",
		"30",
		"31",
		"32",
		"33",
		"34",
		"35",
		"36",
		"37",
		"38",
		"39",
		"40",
		"41",
		"42",
		"44",
		"45",
		"46",
		"47",
		"48",
		"49",
		"50",
		"51",
		"53",
		"54",
		"55",
		"56",
		"60",
		"64",
		"66",
		"68",
		"69",
		"70",
		"72",
		"74",
		"78",
	};

	private static final String[] STATE_ABBREVS = {
		"AL",
		"AK",
		"AZ",
		"AR",
		"CA",
		"CO",
		"CT",
		"DE",
		"DC",
		"FL",
		"GA",
		"HI",
		"ID",
		"IL",
		"IN",
		"IA",
		"KS",
		"KY",
		"LA",
		"ME",
		"MD",
		"MA",
		"MI",
		"MN",
		"MS",
		"MO",
		"MT",
		"NE",
		"NV",
		"NH",
		"NJ",
		"NM",
		"NY",
		"NC",
		"ND",
		"OH",
		"OK",
		"OR",
		"PA",
		"RI",
		"SC",
		"SD",
		"TN",
		"TX",
		"UT",
		"VT",
		"VA",
		"WA",
		"WV",
		"WI",
		"WY",
		"AS",
		"FM",
		"GU",
		"MH",
		"MP",
		"PW",
		"PR",
		"UM",
		"VI",
	};
	
	private static final String[] CODES_TO_ABBREVS;
	private static final String[] CODES_TO_NAMES;
	
	static {
		int[] codes = new int[STATE_CODES.length];
		int max = 0;
		for (int i = 0; i < codes.length; i++) {
			codes[i] = Integer.parseInt(STATE_CODES[i]);
			max = (codes[i] > max ? codes[i] : max);
		}
		
		CODES_TO_ABBREVS = new String[max];
		CODES_TO_NAMES = new String[max];
		for (int i = 0; i < codes.length; i++) {
			CODES_TO_ABBREVS[codes[i]-1] = STATE_ABBREVS[i];
			CODES_TO_NAMES[codes[i]-1] = STATE_NAMES[i];
		}
	}
	
	public static String getStateAbbrev(int code) throws TigerDataException {
		return get(code, true);
	}

	public static String getStateName(int code) throws TigerDataException {
		return get(code, false);
	}

	private static String get(int code, boolean abbrev) throws TigerDataException {
		code--;
		if (code < 0 || code >= CODES_TO_ABBREVS.length || CODES_TO_ABBREVS[code] == null) {
			throw new TigerDataException("no state "+(abbrev ? "abbreviation" : "name")+" for state code "+code);
		} else {
			if (abbrev) {
				return CODES_TO_ABBREVS[code];
			} else {
				return CODES_TO_NAMES[code];
			}
		}
	}
}
