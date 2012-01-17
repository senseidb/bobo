package com.browseengine.bobo.util;

import java.util.Comparator;
import java.util.PriorityQueue;

public class BoundedPriorityQueue<E> extends PriorityQueue<E>
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  private final int _maxSize;
  public BoundedPriorityQueue(int maxSize)
  {
    super();
    _maxSize=maxSize;
  }

  public BoundedPriorityQueue(Comparator<? super E> comparator,int maxSize)
  {
    super(maxSize, comparator);
    _maxSize=maxSize;
  }

  @Override
  public boolean offer(E o)
  {
    int size=size();
    if (size<_maxSize)
    {
      return super.offer(o);
    }
    else
    {
      E smallest=super.peek();
      Comparator<? super E> comparator = super.comparator();
      boolean madeIt=false;
      if (comparator == null)
      {
        if (((Comparable<E>)smallest).compareTo(o) < 0)
        {
          madeIt=true;
        }
      }
      else
      {
        if (comparator.compare(smallest, o) < 0)
        {
          madeIt=true;
        }
      }
      
      if (madeIt)
      {
        super.poll();
        return super.offer(o);
      }
      else
      {
        return false;
      }
    }
  }
}
