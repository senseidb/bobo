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

package com.browseengine.local.service.geocode;

import java.util.Hashtable;
import java.util.Map;

class PlaceMatch {
	Map<Integer,Short> tlidSideMap = new Hashtable<Integer,Short>();
	
	static final short LEFT = 0;
	static final short RIGHT = 1;
	static final short BOTH = 2;
	
	static final short BLANK = 0;
	static final short N = 1;
	static final short S = 2;
	static final short E = 3;
	static final short W = 4;
	static final short NE = 5;
	static final short NW = 6;
	static final short SE = 7;
	static final short SW = 8;
	static final short NO = 9;
	static final short SO = 10;
	static final short O = 11;
	static final short EX = 12;

	static short getLRBoth(short in) {
		return (short)(in & 0x03);
	}
	static short setLRBoth(short prev, short lrboth) {
		return (short)(prev | lrboth);
	}
	static short getDirPre(short in) {
		return (short)((in & 0x3C) >>> 2);
	}
	static short setDirPre(short prev, short dir) {
		return (short)(prev | (dir << 2));
	}
	static short getDirSuf(short in) {
		return (short)((in & 0x3C0) >>> 6);
	}
	static short setDirSuf(short prev, short dir) {
		return (short)(prev | (dir << 6));
	}
}