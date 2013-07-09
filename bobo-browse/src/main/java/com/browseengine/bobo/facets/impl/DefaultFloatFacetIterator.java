/**
 *
 */
package com.browseengine.bobo.facets.impl;

import java.util.NoSuchElementException;

import com.browseengine.bobo.api.FloatFacetIterator;
import com.browseengine.bobo.facets.data.TermFloatList;
import com.browseengine.bobo.util.BigSegmentedArray;

public class DefaultFloatFacetIterator extends FloatFacetIterator {

  public TermFloatList _valList;
  private final BigSegmentedArray _count;
  private final int _countlength;
  private final int _countLengthMinusOne;
  private int _index;

  public DefaultFloatFacetIterator(TermFloatList valList, BigSegmentedArray countarray,
      int countlength, boolean zeroBased) {
    _valList = valList;
    _countlength = countlength;
    _count = countarray;
    _countLengthMinusOne = _countlength - 1;
    _index = -1;
    if (!zeroBased) _index++;
    facet = TermFloatList.VALUE_MISSING;
    count = 0;
  }

  /*
   * (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#getFacet()
   */
  public String getFacet() {
    if (facet == TermFloatList.VALUE_MISSING) return null;
    return _valList.format(facet);
  }

  @Override
  public String format(float val) {
    return _valList.format(val);
  }

  @Override
  public String format(Object val) {
    return _valList.format(val);
  }

  /*
   * (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#getFacetCount()
   */
  public int getFacetCount() {
    return count;
  }

  /*
   * (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  @Override
  public boolean hasNext() {
    return (_index < _countLengthMinusOne);
  }

  /*
   * (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  @Override
  public String next() {
    if ((_index >= 0) && (_index >= _countLengthMinusOne)) throw new NoSuchElementException(
        "No more facets in this iteration");
    _index++;
    facet = _valList.getPrimitiveValue(_index);
    count = _count.get(_index);
    return _valList.get(_index);
  }

  /*
   * (non-Javadoc)
   * @see com.browseengine.bobo.api.FloatFacetIterator#nextFloat()
   */
  @Override
  public float nextFloat() {
    if (_index >= _countLengthMinusOne) throw new NoSuchElementException(
        "No more facets in this iteration");
    _index++;
    facet = _valList.getPrimitiveValue(_index);
    count = _count.get(_index);
    return facet;
  }

  /*
   * (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("remove() method not supported for Facet Iterators");
  }

  /*
   * (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#next(int)
   */
  @Override
  public String next(int minHits) {
    while (++_index < _countlength) {
      if (_count.get(_index) >= minHits) {
        facet = _valList.getPrimitiveValue(_index);
        count = _count.get(_index);
        return _valList.format(facet);
      }
    }
    facet = TermFloatList.VALUE_MISSING;
    count = 0;
    return null;
  }

  /*
   * (non-Javadoc)
   * @see com.browseengine.bobo.api.FloatFacetIterator#nextFloat(int)
   */
  @Override
  public float nextFloat(int minHits) {
    while (++_index < _countlength) {
      if (_count.get(_index) >= minHits) {
        facet = _valList.getPrimitiveValue(_index);
        count = _count.get(_index);
        return facet;
      }
    }
    facet = TermFloatList.VALUE_MISSING;
    count = 0;
    return facet;
  }
}
