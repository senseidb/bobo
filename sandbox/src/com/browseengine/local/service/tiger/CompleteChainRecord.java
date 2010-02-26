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
 * from the *.RT1 file.
 * 
 * @author spackle
 *
 */
public class CompleteChainRecord {
	/**
	 * uniquely identifies this segment
	 */
	private int TLID=-1;

	/**
	 * identifies this type of road/highway/boundary
	 */
	private String CFCC = null;

	// a name for this segment
	private String prefix = null;
	private String name = null;
	private String type = null;
	private String suffix = null;

	// a left entry numeric
	private String frAddL = null;
	private String toAddL = null;
	private int zip5left = -1;

	// a right entry numeric
	private String frAddR = null;
	private String toAddR = null;
	private int zip5right = -1;
	
	// city/place code
	private int placeL = -1;
	private int placeR = -1;
	
	// state code
	private int stateL = -1;
	private int stateR = -1;
	
	// lat/lon for origin point and end point of segment
	private double startLon = -1D;
	private double startLat = -1D;
	private double endLon = -1D;
	private double endLat = -1D;
	
	// possibly not needed
	private int countyL = -1;
	private int countyR = -1;
	
	public CompleteChainRecord(	 int TLID,
	 String prefix ,
	 String name ,
	 String type ,
	 String suffix ,
	 String CFCC ,
	// from/to address
	 String frAddL ,
	 String toAddL ,
	 String frAddR ,
	 String toAddR ,
	 int zip5left ,
	 int zip5right ,
	// state code
	 int stateL ,
	 int stateR ,
	 int countyL ,
	 int countyR ,
	// city/place code
	 int placeL ,
	 int placeR ,
	 double startLon ,
	 double startLat ,
	 double endLon ,
	 double endLat 
	) {
		this.TLID=TLID;
		this.prefix=prefix;
		this.name=name;
		this.type=type;
		 this.suffix=suffix;
		 this.CFCC=CFCC;
		 // from/to address
		 this.frAddL=frAddL;
		 this.toAddL=toAddL;
		 this.frAddR=frAddR;
		 this.toAddR=toAddR;
		 this.zip5left=zip5left;
		 this.zip5right=zip5right;
		// state code
		 this.stateL=stateL;
		 this.stateR=stateR;
		 this.countyL=countyL;
		 this.countyR=countyR;
		// city/place code
		 this.placeL=placeL;
		 this.placeR=placeR;
		 this.startLon=startLon;
		 this.startLat=startLat;
		 this.endLon=endLon;
		 this.endLat=endLat;
	}
	
	public String getCFCC() {
		return CFCC;
	}
	public int getCountyL() {
		return countyL;
	}
	public int getCountyR() {
		return countyR;
	}
	public double getEndLat() {
		return endLat;
	}
	public double getEndLon() {
		return endLon;
	}
	public String getFrAddL() {
		return frAddL;
	}
	public String getFrAddR() {
		return frAddR;
	}
	public String getName() {
		return name;
	}
	public int getPlaceL() {
		return placeL;
	}
	public int getPlaceR() {
		return placeR;
	}
	public String getPrefix() {
		return prefix;
	}
	public double getStartLat() {
		return startLat;
	}
	public double getStartLon() {
		return startLon;
	}
	public int getStateL() {
		return stateL;
	}
	public int getStateR() {
		return stateR;
	}
	public String getSuffix() {
		return suffix;
	}
	public int getTLID() {
		return TLID;
	}
	public String getToAddL() {
		return toAddL;
	}
	public String getToAddR() {
		return toAddR;
	}
	public String getType() {
		return type;
	}
	public int getZip5left() {
		return zip5left;
	}
	public int getZip5right() {
		return zip5right;
	}

	public String toString() {
		// TODO: complete this method
		StringBuilder buf = new StringBuilder();
		buf.append("TLID: ").append(TLID).
		append(", CFCC: ").append(CFCC).
		append(", prefix: ").append(prefix).
		append(", name: ").append(name).
		append(", type: ").append(type).
		append(", suffix: ").append(suffix).
		append(", stateL: ").append(stateL).
		append(", stateR: ").append(stateR).
		append(", placeL: ").append(placeL).
		append(", placeR: ").append(placeR);
		return buf.toString();
	}
}
