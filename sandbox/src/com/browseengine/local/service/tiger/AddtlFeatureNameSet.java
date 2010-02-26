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
 * Records from *.RT4 files.
 * These are a reference between an *.RT1 entry, and one or more 
 * alternate names listed as the *.RT5 entries.
 * 
 * @author spackle
 *
 */
public class AddtlFeatureNameSet implements Comparable<AddtlFeatureNameSet> {
	private int TLID=-1;
	private int priority = -1;
	/**
	 * the alternate name index is called a FEAT
	 */
	private int[] FEATs = null;
	
	public int compareTo(AddtlFeatureNameSet other) {
		Integer p = new Integer(getTLID());
		int ret;
		if ((ret = p.compareTo(other.getTLID())) != 0) {
			return ret;
		} else {
			return new Integer(priority).compareTo(other.getPriority());
		}
	}

	public AddtlFeatureNameSet(int TLID, int priority, int[] idxs) {
		this.TLID = TLID;
		this.priority = priority;
		this.FEATs = idxs;
	}

	public int getPriority() {
		return priority;
	}	
	public int[] getFEATs() {
		return FEATs;
	}

	public int hashCode() {
		return new Integer(TLID).hashCode();
	}
	
	public boolean equals(AddtlFeatureNameSet other) {
		return other.getTLID() == getTLID();
	}
	
	public int getTLID() {
		return TLID;
	}
	
	public String toString() {
		StringBuilder buf = new StringBuilder().append("TLID: ").append(TLID).append(", FEATS: (");
		boolean first = true;
		for (int feat : FEATs) {
			if (!first) {
				buf.append(", ");
			}
			buf.append(feat);
			first = false;
		}
		buf.append(")");
		return buf.toString();
	}
}
