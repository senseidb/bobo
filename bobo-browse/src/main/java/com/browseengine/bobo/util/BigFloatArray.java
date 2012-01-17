package com.browseengine.bobo.util;

public class BigFloatArray {

	  private static final long serialVersionUID = 1L;
		
	  private float[][] _array;
	  private int _numrows;
	  /* Remember that 2^SHIFT_SIZE = BLOCK_SIZE */
	  final private static int BLOCK_SIZE = 1024;
	  final private static int SHIFT_SIZE = 10; 
	  final private static int MASK = BLOCK_SIZE -1;
	  
	  public BigFloatArray(int size)
	  {
	    _numrows = size >> SHIFT_SIZE;
	    _array = new float[_numrows+1][];
	    for (int i = 0; i <= _numrows; i++)
	    {
	      _array[i]=new float[BLOCK_SIZE];
	    }
	  }
	  
	  public void add(int docId, float val)
	  {
	    _array[docId >> SHIFT_SIZE][docId & MASK] = val;
	  }
	  
	  public float get(int docId)
	  {
	    return _array[docId >> SHIFT_SIZE][docId & MASK];
	  }
	  
	  public int capacity()
	  {
	    return _numrows * BLOCK_SIZE;
	  }
	  
	  public void ensureCapacity(int size)
	  {
	    int newNumrows = (size >> SHIFT_SIZE) + 1;
	    if (newNumrows > _array.length)
	    {
	      float[][] newArray = new float[newNumrows][];           // grow
	      System.arraycopy(_array, 0, newArray, 0, _array.length);
	      for (int i = _array.length; i < newNumrows; ++i)
	      {
	        newArray[i] = new float[BLOCK_SIZE];
	      }
	      _array = newArray;
	    }
	    _numrows = newNumrows;
	  }
}
