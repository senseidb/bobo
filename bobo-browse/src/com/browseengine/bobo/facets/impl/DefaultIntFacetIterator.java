package com.browseengine.bobo.facets.impl;

import java.util.NoSuchElementException;

import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.facets.data.TermIntList;

public class DefaultIntFacetIterator implements FacetIterator
{

  public TermIntList _valList;
  private int[] _count;
  private int _countLengthMinusOne;
  private int _index;
//  private String _facet;
  public int _facet;
  public int _cnt;

  public DefaultIntFacetIterator(TermIntList valList, int[] count, boolean zeroBased) {
    _valList = valList;
    _count = count;
    _countLengthMinusOne = _count.length-1;
    _index = -1;
    if(!zeroBased)
      _index++;
    _facet = -1;
    _cnt = 0;
  }

  /* (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#getFacet()
   */
  public String getFacet() {
    return _valList.format(_facet);
  }

  /* (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#getFacetCount()
   */
  public int getFacetCount() {
    return _cnt;
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
    _facet = _valList.getPrimitiveValue(_index);
    _cnt = _count[_index];
    return _valList.get(_index);
  }

  public int nextInt() {
    if(_index >= _countLengthMinusOne)
      throw new NoSuchElementException("No more facets in this iteration");
    _index++;
    _facet = _valList.getPrimitiveValue(_index);
    _cnt = _count[_index];
    return _facet;
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
    while(++_index < _count.length)
    {
      if(_count[_index] >= minHits)
      {
        _facet = _valList.getPrimitiveValue(_index);
        _cnt = _count[_index];
        return _valList.format(_facet);
      }
    }
    _facet = -1;
    _cnt = 0;
    return _valList.format(_facet);    
  }
  public int nextInt(int minHits)
  {
    while(++_index < _count.length)
    {
      if(_count[_index] >= minHits)
      {
        _facet = _valList.getPrimitiveValue(_index);
        _cnt = _count[_index];
        return _facet;
      }
    }
    _facet = -1;
    _cnt = 0;
    return _facet;    
  }
}
