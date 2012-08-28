/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  Spackle
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
 * send mail to owner@browseengine.com.
 */
package com.browseengine.bobo.util.test;

import java.util.Random;

import junit.framework.TestCase;

import com.browseengine.bobo.util.SparseFloatArray;

/**
 * @author spackle
 *
 */
public class SparseFloatArrayTest extends TestCase {
	private static final long SEED = -1587797429870936371L;

	public void testSpeed() throws Throwable {
		try {
			float[] orig = new float[32*1024*1024];
			float density = 0.4f;
			Random rand = new Random(SEED);
			int idx = 0;
			while (rand.nextFloat() > density) {
				idx++;
			}
			int count = 0;
			while (idx < orig.length) {
				orig[idx] = rand.nextFloat();
				count++;
				idx += 1;
				while (rand.nextDouble() > density) {
					idx++;
				}
			}
			assertTrue("count was bad: "+count, count > 100 && count < orig.length/2);
			System.out.println("float array with "+count+" out of "+orig.length+" non-zero values");
			
			SparseFloatArray sparse = new SparseFloatArray(orig);
			
			for (int i = 0; i < orig.length; i++) {
				float o = orig[i];
				float s = sparse.get(i);
				assertTrue("orig "+o+" wasn't the same as sparse: "+s+" for i = "+i, o == s);
			}
			// things came out correct
			
			long markTime = System.currentTimeMillis();
			for (int i = 0; i < orig.length; i++) {
				float f = orig[i];
			}
			long elapsedTimeOrig = System.currentTimeMillis()-markTime;
			
			markTime = System.currentTimeMillis();
			for (int i = 0;i < orig.length; i++) {
				float f = sparse.get(i);
			}
			long elapsedTimeSparse = System.currentTimeMillis()-markTime;
			
			double ratio = (double)elapsedTimeSparse/(double)elapsedTimeOrig;
			System.out.println("fyi on speed, direct array access took "+elapsedTimeOrig+
					" millis, while sparse float access took "+elapsedTimeSparse+
					"; that's a "+ratio+
					" X slowdown by using the condensed memory model (smaller number is better)");
			
			System.out.println(getName()+" success!");
		} catch (Throwable t) {
			t.printStackTrace();
			System.err.println("fail: "+t);
			throw t;
		}
	}
}
