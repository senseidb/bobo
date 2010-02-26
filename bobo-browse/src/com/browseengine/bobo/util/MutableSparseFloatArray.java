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

package com.browseengine.bobo.util;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * @author spackle
 *
 */
public class MutableSparseFloatArray extends SparseFloatArray {
	private Map<Integer,Float> _map;
	private boolean _isDirty;
	
	public MutableSparseFloatArray(float[] floats) {
		super(floats);
		_map = new Hashtable<Integer,Float>();
		_isDirty = false;
	}

	public boolean isDirty() {
		return _isDirty;
	}
	
	public synchronized float get(int idx) {
		float val = super.get(idx);
		if (val != 0f) {
			return val;
		}
		// else, check here!
		Float stored = _map.get(idx);
		if (stored != null) {
			return stored;
		}
		return 0f;
	}
	
	public synchronized void set(int idx, float val) {
		_isDirty = true;
		if (null == _bits && null != _floats) {
			_floats[idx] = val;
		} else {
			if (null != _bits && _bits.get(idx)) {
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
				_floats[count] = val;
			} else {
				if (val != 0f) {
					_map.put(idx, val);
				} else {
					Float stored = _map.get(idx);
					if (stored != null) {
						_map.remove(idx);
					}
				}
				int sz = _map.size();
				// keep something on the order of 32KB, or 0.4*compressed size, in _map
				// if _floats is null, then that's the same as it existing but being of length 0
				// if sz > 512, and _floats is null, that's the same as checking if sz > 0.4f*0 and that sz > 2f*0, which is true
				// in other words, if _floats is null, and sz > 512, then our expansion rule says to condense()
				if (sz > 512 && (null == _floats || (sz > 4096 ? sz > 0.4f*_floats.length : sz > 2f*_floats.length))) {
					condense();
				}
			}
		}
	}
	
	/**
	 * See super{@link #expand()}.  Future modifications to this may or may not affect values stored in the 
	 * returned array.  Modifications to the returned array, cause this object instance to become unusable, as 
	 * from this point on all operations are UNDEFINED.
	 * 
	 * @return the expanded primitive float array rep. of this.
	 */
	public synchronized float[] expand() {
		float[] all = super.expand();
		Iterator<Integer> keysIter = _map.keySet().iterator();
		while (keysIter.hasNext()) {
			int idx = keysIter.next();
			float val = _map.get(idx);
			all[idx] = val;
		}
		return all;
	}
	
	/**
	 * An expensive, but necessary, operation to internally conserve space as things grow.
	 * Might be useful to call outside of the automatic maintenance, 
	 * when you expect very few new non-zero values, just changes to existing values,
	 * in the future.
	 *
	 * Uses an expanded form of the float array as scratch space in memory, so be careful 
	 * that you have enough memory, and try doing this one at a time.
	 */
	public synchronized void condense() {
		super.condense(expand());
		_map = new Hashtable<Integer,Float>();
	}
}
