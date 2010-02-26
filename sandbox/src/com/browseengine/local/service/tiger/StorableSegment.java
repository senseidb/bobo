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
 * @author spackle
 *
 */
class StorableSegment {
	/**
	 * uniquely identifies this segment
	 */
	int TLID=-1;

	Name[] names;
	
	// zero or more left address ranges
	NumberAndZip[] lefts;
	
	// zero or more right address ranges
	NumberAndZip[] rights;
	
	// city/place code
	String placeL;
	String placeR;
	
	// state code
	String stateL;
	String stateR;
	
	// lat/lon for origin point and end point of segment
	double startLon = -1D;
	double startLat = -1D;
	double endLon = -1D;
	double endLat = -1D;

	static class Name {
		// a name for this segment
		String prefix = null;
		String name = null;
		String type = null;
		String suffix = null;
		public String getName() {
			return name;
		}
		public String getPrefix() {
			return prefix;
		}
		public String getSuffix() {
			return suffix;
		}
		public String getType() {
			return type;
		}
	}
	
	static class NumberAndZip {
		String frAdd = null;
		String toAdd = null;
		int zip5 = -1;
		
		/**
		 * check if frAdd, toAdd are proper ints >= 0, and zip5 > 0
		 * @return
		 */
		boolean isNumeric() {
			try {
				int fr = Integer.parseInt(frAdd);
				int to = Integer.parseInt(toAdd);
				if (fr >= 0 && to >= 0 && zip5 > 0) {
					return true;
				} else {
					return false;
				}
			} catch (NumberFormatException nfe) {
				return false;
			}
		}

		public String getFrAdd() {
			return frAdd;
		}

		public String getToAdd() {
			return toAdd;
		}

		public int getZip5() {
			return zip5;
		}
	}

	public double getEndLat() {
		return endLat;
	}

	public double getEndLon() {
		return endLon;
	}

	public NumberAndZip[] getLefts() {
		return lefts;
	}

	public Name[] getNames() {
		return names;
	}

	public String getPlaceL() {
		return placeL;
	}

	public String getPlaceR() {
		return placeR;
	}

	public NumberAndZip[] getRights() {
		return rights;
	}

	public double getStartLat() {
		return startLat;
	}

	public double getStartLon() {
		return startLon;
	}

	public String getStateL() {
		return stateL;
	}

	public String getStateR() {
		return stateR;
	}

	public int getTLID() {
		return TLID;
	}
}
