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

import java.util.BitSet;

/**
 * @author spackle
 *
 */
public class TernarySet {
	private BitSet _bits;
	private short _numValues;
	private int _size;
	// width of each entry
	private short _width;
	
	/**
	 * Uses a BitSet (efficient packing of bits into a long[]), 
	 * with number of bits equal to 
	 * size*ceiling(log_base_2(numValues)).
	 * 
	 * <p>
	 * In general, you can't use values exceeding <code>numValues</code>, 
	 * and you can't use indexes outside of 
	 * <pre>
	 * [0, size-1]
	 * </pre>
	 * 
	 * @param ternaryValues
	 * @param size
	 * @throws IllegalArgumentException if either (ternaryValues < 2), or (size < 1).
	 */
	public TernarySet(short ternaryValues, int size) {
		_numValues = ternaryValues;
		_size = size;
	    int val = (int)ternaryValues;
	    if (val < 2) {
	      throw new IllegalArgumentException("numValues "+ternaryValues+" renders TernarySet useless");
	    }
	    if (size < 1) {
	    	throw new IllegalArgumentException("size "+size+" renders TernarySet useless");
	    }
	    // find the width of a single entry, in number of bits
	    val--;
	    _width = 0;
	    while (val > 0) {
	      _width++;
	      val >>>= 1;
	    }
	    int nbits = _width*size;
	    _bits = new BitSet(nbits);

	}
	
	public int size() {
		return _size;
	}
	
	public short get(int idx) {
		if (idx >= _size || idx < 0) {
			throw new ArrayIndexOutOfBoundsException("get index: "+idx+" was out-of-bounds for size "+_size);
		}
		short mask = 0;
		int bitsIdx = idx*_width;
		for (short i = 0; i < _width; i++) {
			if (_bits.get(bitsIdx+i)) {
				mask += (1 << i);
			}
		}
		return mask;
	}
	
	public void set(int idx, short val) {
		if (idx >= _size || idx < 0) {
			throw new ArrayIndexOutOfBoundsException("set index: "+idx+" was out-of-bounds for size "+_size);
		}
		if (val < 0 || val >= _numValues) {
			throw new IllegalArgumentException("set value: "+val+" was invalid, this TernarySet only understands values on the range ["+0+", "+(_numValues-1)+"]");
		}
		int bitsIdx = idx*_width;
		for (short i = 0; val > 0 && i < _width; i++) {
			if ((val & 1) == 0) {
				_bits.clear(bitsIdx+i);
			} else {
				_bits.set(bitsIdx+i);
			}
			val >>>= 1;
		}
		// mask should be zero
	}
}
