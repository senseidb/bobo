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

package com.browseenginge.local.service.geosearch.test;

import junit.framework.TestCase;

import com.browseengine.local.service.LonLat;
import com.browseengine.local.service.geosearch.HaversineFormula;

/**
 * @author spackle
 *
 */
public class HaversineFormulaTest extends TestCase {
	public void testBoundaries() throws Throwable {
		try {
			LonLat point = LonLat.getLonLatDeg(-122, 45);
			float range = 5f;
			double dlat = HaversineFormula.computeLatBoundary(point.getLongitudeRad(), point.getLatitudeRad(), range);
			LonLat north = point.moveRad(0, dlat);
			LonLat south = point.moveRad(0, -dlat);
			double dlon = HaversineFormula.computeLonBoundary(point.getLongitudeRad(), point.getLatitudeRad(), range);
			LonLat east = point.moveRad(dlon, 0);
			LonLat west = point.moveRad(-dlon, 0);
			
			float dnorth = HaversineFormula.computeHaversineDistanceMiles(point.getLongitudeRad(), point.getLatitudeRad(), north.getLongitudeRad(), north.getLatitudeRad());
			assertTrue("north at "+north+" from point at "+point+" not "+range+" miles away, it was "+dnorth+" miles", Math.abs(dnorth-range) <= 0.0001);
			float dsouth = HaversineFormula.computeHaversineDistanceMiles(point.getLongitudeRad(), point.getLatitudeRad(), south.getLongitudeRad(), south.getLatitudeRad());
			assertTrue("south at "+south+" from point at "+point+" not "+range+" miles away, it was "+dsouth+" miles", Math.abs(dsouth-range) <= 0.0001);
			float deast = HaversineFormula.computeHaversineDistanceMiles(point.getLongitudeRad(), point.getLatitudeRad(), east.getLongitudeRad(), east.getLatitudeRad());
			assertTrue("deast at "+east+" from point at "+point+" not "+range+" miles away, it was "+deast+" miles", Math.abs(deast-range) <= 0.0001);
			float dwest = HaversineFormula.computeHaversineDistanceMiles(point.getLongitudeRad(), point.getLatitudeRad(), west.getLongitudeRad(), west.getLatitudeRad());
			assertTrue("west at "+west+" from point at "+point+" not "+range+" miles away, it was "+west+" miles", Math.abs(dwest-range) <= 0.0001);
			float ten1 = HaversineFormula.computeHaversineDistanceMiles(south.getLongitudeRad(), south.getLatitudeRad(), north.getLongitudeRad(), north.getLatitudeRad());
			float ten2 = HaversineFormula.computeHaversineDistanceMiles(east.getLongitudeRad(), east.getLatitudeRad(), west.getLongitudeRad(), west.getLatitudeRad());
			float s2e = HaversineFormula.computeHaversineDistanceMiles(south.getLongitudeRad(), south.getLatitudeRad(), east.getLongitudeRad(), east.getLatitudeRad());
			float e2n = HaversineFormula.computeHaversineDistanceMiles(east.getLongitudeRad(), east.getLatitudeRad(), north.getLongitudeRad(), north.getLatitudeRad());
			float n2w = HaversineFormula.computeHaversineDistanceMiles(north.getLongitudeRad(), north.getLatitudeRad(), west.getLongitudeRad(), west.getLatitudeRad());
			float w2s = HaversineFormula.computeHaversineDistanceMiles(west.getLongitudeRad(), west.getLatitudeRad(), south.getLongitudeRad(), south.getLatitudeRad());
			// according to the curvature of the earth, walking s2e and w2s should be about the same distance, 
			// and walking e2n and n2w should be about the same distance, but walking s2e and e2n should be a bit 
			// different, precisely b/c the earth is curved, and east and west are not walking directly away from 
			// each other, they are like walking along a U-shape starting from the middle.
			float rootfifty = (float)Math.sqrt(50);
			float error = 0.01f;
			assertTrue("a value wasn't right, one of tens: "+ten1+", "+ten2+"; or one of (almost) rootfifties "+rootfifty+" s2e: "+s2e+", e2n: "+e2n+", n2w: "+n2w+", w2s: "+w2s, 
					approxEqual(ten1, 10) && approxEqual(ten2, 10) && 
					approxEqual(s2e, w2s) && 
					approxEqual(e2n, n2w) && 
					approxEqual(s2e, rootfifty, error) && 
					approxEqual(e2n, rootfifty, error) &&
					!approxEqual(s2e, e2n));
		} catch (Throwable t) {
			t.printStackTrace();
			System.err.println("fail: "+t);
			throw t;
		}
	}

	public static boolean approxEqual(double a, double b, double error) {
		return Math.abs(a-b) < error;
	}
	
	public static boolean approxEqual(double a, double b) {
		return Math.abs(a-b) < 0.0001;
	}
}
