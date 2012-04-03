/**
 * 
 */
package com.browseengine.bobo.util;

import java.util.ArrayList;

/**
 * @author ymatsuda
 *
 */
public class BigIntBuffer
{
  private static final int PAGESIZE = 1024;
  private static final int MASK = 0x3FF;
  private static final int SHIFT = 10;

  private ArrayList<int[]> _buffer;
  private int _allocSize;
  private int _mark;

  public BigIntBuffer()
  {
    _buffer = new ArrayList<int[]>();
    _allocSize = 0;
    _mark = 0;
  }
  
  public int alloc(int size)
  {
    if(size > PAGESIZE) throw new IllegalArgumentException("size too big");
    
    if((_mark + size) > _allocSize)
    {
      int[] page = new int[PAGESIZE];
      _buffer.add(page);
      _allocSize += PAGESIZE;
    }
    int ptr = _mark;
    _mark += size;

    return ptr;
  }
  
  public void reset()
  {
    _mark = 0;
  }
  
  public void set(int ptr, int val)
  {
    int[] page = _buffer.get(ptr >> SHIFT);
    page[ptr & MASK] = val;
  }
  
  public int get(int ptr)
  {
    int[] page = _buffer.get(ptr >> SHIFT);
    return page[ptr & MASK];
  }
}
