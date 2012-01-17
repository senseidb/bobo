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
package com.browseengine.bobo.util;

import java.util.BitSet;

/**
 * @author spackle
 *
 */
public class SparseFloatArray {
	float[] _floats;
	BitSet _bits;
	/**
	 * the number of bits set BEFORE the given reference point index*REFERENCE_POINT_EVERY.
	 */
	int[] _referencePoints;
	private int _capacity;
	private static final float ON_RATIO_CUTOFF = 0.75f;
	/**
	 * 32 is 32 bits per 256 floats, which is the same as the 32 bits per 32 floats that are needed
	 * in _bits.  
	 */
	static final int REFERENCE_POINT_EVERY = 32;//256;
	
	/**
	 * Good for saving memory with sparse float arrays, when those arrays no longer need to be mutable.
	 * 
	 * requires: floats never changes after this method is called returns.
	 * in fact, you should lose all references to it, since this object 
	 * might save you a lot of memory.
	 * 
	 * @param floats
	 */
	public SparseFloatArray(float[] floats) {
		_capacity = floats.length;
		condense(floats);
	}

	/**
	 * Short-cut to quickly create a sparse float array representing 
	 * <code>this(new float[capacity]);</code>, but without reading through said array.
	 * The advantage here is that the constructor is lightning-fast in the case that 
	 * all values in the float array are known to 
	 * <pre>
	 * == 0f
	 * <pre>
	 * .
	 * 
	 * @param capacity
	 */
	public SparseFloatArray(int capacity) {
		_capacity = capacity;
		_floats = null;
		_bits = null;
		_referencePoints = null;
	}
	
	void condense(float[] floats) {
		if (floats.length != _capacity) {
			throw new IllegalArgumentException("bad input float array of length "+floats.length+" for capacity: "+_capacity);
		}
		BitSet bits = new BitSet(floats.length);
		int on = 0;
		for (int i = 0; i < floats.length; i++) {
			if (floats[i] != 0f) {
				bits.set(i);
				on++;
			}
		}
		if (((float)on)/((float)floats.length) < ON_RATIO_CUTOFF) {
			// it's worth compressing
			if (0 == on) {
				// it's worth super-compressing
				_floats = null;
				_bits = null;
				_referencePoints = null;
				// capacity is good.
			} else {
			_bits = bits;
			_floats = new float[_bits.cardinality()];
			_referencePoints = new int[floats.length/REFERENCE_POINT_EVERY];
			int i = 0;
			int floatsIdx = 0;
			int refIdx = 0;
			while (i < floats.length && (i = _bits.nextSetBit(i)) >= 0) {
				_floats[floatsIdx] = floats[i];
				while (refIdx < i/REFERENCE_POINT_EVERY) {
					_referencePoints[refIdx++] = floatsIdx;
				}
				floatsIdx++;
				i++;
			}
			while (refIdx < _referencePoints.length) {
				_referencePoints[refIdx++] = floatsIdx;
			}
			}
		} else {
			// it's not worth compressing
			_floats = floats;
			_bits = null;
		}
	}
	
	/**
	 * warning: DO NOT modify the return value at all.
	 * the assumption is that these arrays are QUITE LARGE and that we would not want 
	 * to unnecessarily copy them.  this method in many cases returns an array from its
	 * internal representation.  doing anything other than READING these values 
	 * results in UNDEFINED operations on this, from that point on.
	 * 
	 * @return
	 */
	public float[] expand() {
		if (null == _bits) {
			if (null == _floats) {
				// super-compressed, all zeros
				return new float[_capacity];
			} else {
				return _floats;
			}
		}
		float[] all = new float[_capacity];
		int floatsidx = 0;
		for (int idx = _bits.nextSetBit(0); idx >= 0 && idx < _capacity; idx = _bits.nextSetBit(idx+1)) {
			all[idx] = _floats[floatsidx++];
		}
		return all;
	}
	
	public float get(int idx) {
		if (null == _bits) {
			if (null == _floats) {
				// super-compressed, all zeros
				if (idx < 0 || idx >= _capacity) {
					throw new ArrayIndexOutOfBoundsException("bad index: "+idx+" for SparseFloatArray representing array of length "+_capacity);
				}
				return 0f;
			} else {
				return _floats[idx];
			}
		} else {
			if (_bits.get(idx)) {
				// count the number of bits that are on BEFORE this idx
				int count;
				int ref = idx/REFERENCE_POINT_EVERY-1;
				if (ref >= 0) {
					count = _referencePoints[ref];
				} else {
					count = 0;
				}
				int i = idx - idx%REFERENCE_POINT_EVERY;
				while ((i = _bits.nextSetBit(i)) >= 0 && i < idx) {
					count++;
					i++;
				}
				return _floats[count];
			} else {
				return 0f;
			}
		}
	}

}
