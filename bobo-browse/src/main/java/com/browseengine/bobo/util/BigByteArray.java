package com.browseengine.bobo.util;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitVector;
import org.apache.lucene.util.OpenBitSet;

public class BigByteArray extends BigSegmentedArray implements Serializable
{
	  
	  private static final long serialVersionUID = 1L;
		
	  private byte[][] _array;
	  
	  /* Remember that 2^SHIFT_SIZE = BLOCK_SIZE */
	  final private static int BLOCK_SIZE = 4096;
	  final private static int SHIFT_SIZE = 12; 
	  final private static int MASK = BLOCK_SIZE -1;
	  
	  public BigByteArray(int size)
	  {
	    super(size);
	    _array = new byte[_numrows][];
	    for (int i = 0; i < _numrows; i++)
	    {
	      _array[i]=new byte[BLOCK_SIZE];
	    }
	  }

	  @Override
	  public final void add(int docId, int val)
	  {
	    _array[docId >> SHIFT_SIZE][docId & MASK] = (byte)val;
	  }

	  @Override
	  public final int get(int docId)
	  {
	    return _array[docId >> SHIFT_SIZE][docId & MASK];
	  }
	  
	  @Override
	  public final int findValue(int val, int docId, int maxId)
	  {
	    while(true)
	    {
	      if(_array[docId >> SHIFT_SIZE][docId & MASK] == val) return docId;
	      if(docId++ >= maxId) break;
	    }
	    return DocIdSetIterator.NO_MORE_DOCS;
	  }
	  
	  @Override
	  public final int findValues(OpenBitSet bitset, int docId, int maxId)
	  {
	    while(true)
	    {
	      if(bitset.fastGet(_array[docId >> SHIFT_SIZE][docId & MASK])) return docId;
	      if(docId++ >= maxId) break;
	    }
	    return DocIdSetIterator.NO_MORE_DOCS;
	  }
	  
	  @Override
	  public final int findValues(BitVector bitset, int docId, int maxId)
	  {
	    while(true)
	    {
	      if(bitset.get(_array[docId >> SHIFT_SIZE][docId & MASK])) return docId;
	      if(docId++ >= maxId) break;
	    }
	    return DocIdSetIterator.NO_MORE_DOCS;
	  }
	  
	  @Override
	  public final int findValueRange(int minVal, int maxVal, int docId, int maxId)
	  {
	    while(true)
	    {
	      int val = _array[docId >> SHIFT_SIZE][docId & MASK];
	      if(val >= minVal && val <= maxVal) return docId;
	      if(docId++ >= maxId) break;
	    }
	    return DocIdSetIterator.NO_MORE_DOCS;
	  }
	  
	  @Override
	  public final int findBits(int bits, int docId, int maxId)
	  {
	    while(true)
	    {
	      if((_array[docId >> SHIFT_SIZE][docId & MASK] & bits) != 0) return docId;
	      if(docId++ >= maxId) break;
	    }
	    return DocIdSetIterator.NO_MORE_DOCS;
	  }

	  @Override
	  public final void fill(int val)
	  {
		byte byteVal = (byte)val;
	    for(byte[] block : _array)
	    {
	      Arrays.fill(block, byteVal);
	    }
	  }

	  @Override
	  public void ensureCapacity(int size)
	  {
	    int newNumrows = (size >> SHIFT_SIZE) + 1;
	    if (newNumrows > _array.length)
	    {
	      byte[][] newArray = new byte[newNumrows][];           // grow
	      System.arraycopy(_array, 0, newArray, 0, _array.length);
	      for (int i = _array.length; i < newNumrows; ++i)
	      {
	        newArray[i] = new byte[BLOCK_SIZE];
	      }
	      _array = newArray;
	    }
	    _numrows = newNumrows;
	  }
	  
	  @Override
	  final int getBlockSize() {
		return BLOCK_SIZE;
	  }
		
	  @Override
	  final int getShiftSize() {
		return SHIFT_SIZE;
	  }

	  @Override
	  public int maxValue() {
		return Byte.MAX_VALUE;
	  }
}
