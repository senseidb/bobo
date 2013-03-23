package com.browseengine.bobo.util;

import org.apache.lucene.util.BitVector;
import org.apache.lucene.util.OpenBitSet;

/**
 * Breaks up a regular java array by splitting it into a 2 dimensional array with
 * a predefined block size. Attempts to induce more efficient GC.  
 */

public abstract class BigSegmentedArray {

	protected final int _size;
	protected final int _blockSize;
	protected final int _shiftSize;

	protected int _numrows;
	  
	public BigSegmentedArray(int size)
	{
	  _size = size;
	  _blockSize = getBlockSize();
	  _shiftSize = getShiftSize();
	  _numrows = (size >> _shiftSize) + 1;
	}
	
	public int size(){
	  return _size;
	}
	
	abstract int getBlockSize();
	
	// TODO: maybe this should be automatically calculated
	abstract int getShiftSize();
	
	abstract public int get(int id);
	
	public int capacity(){
	  return _numrows * _blockSize;
	}
	
	abstract public void add(int id, int val);
	
	abstract public void fill(int val);
	  
	abstract public void ensureCapacity(int size);
	
	abstract public int maxValue();
	
	abstract public int findValue(int val, int id, int maxId);
	
	abstract public int findValues(OpenBitSet bitset, int id, int maxId);

	abstract public int findValues(BitVector bitset, int id, int maxId);
    
	abstract public int findValueRange(int minVal, int maxVal, int id, int maxId);
	  
	abstract public int findBits(int bits, int id, int maxId);
}
