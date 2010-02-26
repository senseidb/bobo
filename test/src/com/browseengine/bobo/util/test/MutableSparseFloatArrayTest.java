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

package com.browseengine.bobo.util.test;

import java.util.Random;

import junit.framework.TestCase;

import com.browseengine.bobo.util.MutableSparseFloatArray;

/**
 * @author spackle
 *
 */
public class MutableSparseFloatArrayTest extends TestCase {
	private static final long SEED = -7862018348108294439L;
	
	public void testMute() throws Throwable {
		try {
			Random rand = new Random(SEED);
			
			float[] orig = new float[1024];
			MutableSparseFloatArray fromEmpty = new MutableSparseFloatArray(new float[1024]);
			float density = 0.2f;
			int idx = 0;
			while (rand.nextFloat() > density) {
				idx++;
			}
			int count = 0;
			while (idx < orig.length) {
				float val = rand.nextFloat();
				orig[idx] = val;
				fromEmpty.set(idx, val);
				count++;
				idx += 1;
				while (rand.nextDouble() > density) {
					idx++;
				}
			}
			float[] copy =new float[orig.length]; 
			System.arraycopy(orig, 0, copy, 0, orig.length);
			MutableSparseFloatArray fromPartial = new MutableSparseFloatArray(copy);
			
			// do 128 modifications
			int mods = 128;
			for (int i = 0; i < mods; i++) {
				float val = rand.nextFloat();
				idx = rand.nextInt(orig.length);
				orig[idx] = val;
				fromEmpty.set(idx, val);
				fromPartial.set(idx, val);				
			}
			
			for (int i = 0; i < orig.length; i++) {
				assertTrue("orig "+orig[i]+" wasn't the same as fromEmpty "+fromEmpty.get(i)+" at i="+i, orig[i] == fromEmpty.get(i));
				assertTrue("orig "+orig[i]+" wasn't the same as fromPartial "+fromPartial.get(i)+" at i="+i, orig[i] == fromPartial.get(i));
			}
			
			System.out.println(getName()+" success!");
		} catch (Throwable t) {
			System.err.println("fail: "+t);
			t.printStackTrace();
			throw t;
		}
	}
	
	public void testSpeed() throws Throwable {
		try {
			Random r = new Random(SEED);
			
			float[] orig = new float[16*1024*1024];
			MutableSparseFloatArray arr = new MutableSparseFloatArray(new float[orig.length]);
			
			for (int i = 0; i < 32*1024; i++) {
				int idx = r.nextInt(orig.length);
				if (r.nextBoolean()) {
					assertTrue("orig "+orig[idx]+" not the same as arr "+arr.get(idx)+" at idx="+idx, orig[idx] == arr.get(idx));
				} else {
					float val = r.nextFloat();
					orig[idx] = val;
					arr.set(idx, val);
				}
			}
			
			// repeat it, but timed
			orig = new float[orig.length];
			arr = new MutableSparseFloatArray(new float[orig.length]);
			int[] idxs = new int[1024*1024];
			float[] vals = new float[idxs.length];
			for (int i = 0; i < idxs.length; i++) {
				idxs[i] = r.nextInt(orig.length);
				vals[i] = r.nextFloat();
			}
			
			long markTime = System.currentTimeMillis();
			for (int i = 0; i < idxs.length; i++) {
				orig[i] = vals[i];
			}
			long elapsedTimePrim = System.currentTimeMillis()-markTime;
			
			markTime = System.currentTimeMillis();
			for (int i = 0; i < idxs.length; i++) {
				arr.set(idxs[i], vals[i]);
			}
			long elapsedTimeMutable = System.currentTimeMillis()-markTime;
			
			System.out.println("elapsed time on the primitive array: "+elapsedTimePrim+"; elapsed time on the mutable condensed arr: "+elapsedTimeMutable);
			System.out.println("ratio of time to do it on the mutable condensed arr, to time on primitive array: "+(double)elapsedTimeMutable/elapsedTimePrim);
		} catch (Throwable t) {
			System.err.println("fail: "+t);
			t.printStackTrace();
			throw t;
		}
	}
}
