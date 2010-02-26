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
public class AddtlAddressRange implements Comparable<AddtlAddressRange>{
	private int TLID=-1;
	private int priority=-1;
	private String frAddL;
	private String toAddL;
	private String frAddR;
	private String toAddR;
	private int zip5left;
	private int zip5right;
	
	public int compareTo(AddtlAddressRange other) {
		Integer p = new Integer(getTLID());
		int ret;
		if ((ret = p.compareTo(other.getTLID())) != 0) {
			return ret;
		} else {
			return new Integer(priority).compareTo(other.getPriority());
		}
	}
	
	public AddtlAddressRange(
			int TLID,
			int priority,
			String frAddL,
			String toAddL,
			String frAddR,
			String toAddR,
			int zip5left,
			int zip5right
			) {
		this.TLID = TLID;
		this.priority = priority;
		this.frAddL = frAddL;
		this.toAddL = toAddL;
		this.frAddR = frAddR;
		this.toAddR = toAddR;
		this.zip5left = zip5left;
		this.zip5right = zip5right;
	}

	public String getFrAddL() {
		return frAddL;
	}

	public String getFrAddR() {
		return frAddR;
	}

	public int getPriority() {
		return priority;
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

	public int getZip5left() {
		return zip5left;
	}

	public int getZip5right() {
		return zip5right;
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("TLID: ").append(TLID).
		append(", priority: ").append(priority).
		append(", frAddL: ").append(frAddL).
		append(", toAddL: ").append(toAddL).
		append(", frAddR: ").append(frAddR).
		append(", toAddR: ").append(toAddR).
		append(", zip5left: ").append(zip5left).
		append(", zip5right: ").append(zip5right);
		return buf.toString();
	}
}
