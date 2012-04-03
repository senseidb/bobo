package com.browseengine.bobo.facets.impl;

import java.util.NoSuchElementException;

import com.browseengine.bobo.api.IntFacetIterator;
import com.browseengine.bobo.facets.data.TermIntList;

public class DefaultIntFacetIterator extends IntFacetIterator
{

  public TermIntList _valList;
  private int[] _count;
  private int _countlength;
  private int _countLengthMinusOne;
  private int _index;

  public DefaultIntFacetIterator(TermIntList valList, int[] countarray, int countlength, boolean zeroBased)
  {
    _valList = valList;
    _count = countarray;
    _countlength = countlength;
    _countLengthMinusOne = countlength-1;
    _index = -1;
    if(!zeroBased)
      _index++;
    facet = TermIntList.VALUE_MISSING;
    count = 0;
  }

  /* (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#getFacet()
   */
  public String getFacet() {
    if (facet == -1) return null;
    return _valList.format(facet);
  }
  public String format(int val)
  {
    return _valList.format(val);
  }
  public String format(Object val)
  {
    return _valList.format(val);
  }
  /* (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#getFacetCount()
   */
  public int getFacetCount() {
    return count;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return (_index < _countLengthMinusOne);
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public String next() {
    if((_index >= 0) && (_index >= _countLengthMinusOne))
      throw new NoSuchElementException("No more facets in this iteration");
    _index++;
    facet = _valList.getPrimitiveValue(_index);
    count = _count[_index];
    return _valList.get(_index);
  }

  /* (non-Javadoc)
   * @see com.browseengine.bobo.api.IntFacetIterator#nextInt()
   */
  public int nextInt()
  {
    if(_index >= _countLengthMinusOne)
      throw new NoSuchElementException("No more facets in this iteration");
    _index++;
    facet = _valList.getPrimitiveValue(_index);
    count = _count[_index];
    return facet;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new UnsupportedOperationException("remove() method not supported for Facet Iterators");
  }

  /* (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#next(int)
   */
  public String next(int minHits)
  {
    while(++_index < _countlength)
    {
      if(_count[_index] >= minHits)
      {
        facet = _valList.getPrimitiveValue(_index);
        count = _count[_index];
        return _valList.format(facet);
      }
    }
    facet = TermIntList.VALUE_MISSING;
    count = 0;
    return null;    
  }
  /* (non-Javadoc)
   * @see com.browseengine.bobo.api.IntFacetIterator#nextInt(int)
   */
  public int nextInt(int minHits)
  {
    while(++_index < _countlength)
    {
      if(_count[_index] >= minHits)
      {
        facet = _valList.getPrimitiveValue(_index);
        count = _count[_index];
        return facet;
      }
    }
    facet = TermIntList.VALUE_MISSING;
    count = 0;
    return facet;    
  }
}
