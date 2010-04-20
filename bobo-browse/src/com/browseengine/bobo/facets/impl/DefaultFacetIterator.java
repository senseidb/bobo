/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.util.NoSuchElementException;

import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.facets.data.TermValueList;

/**
 * @author nnarkhed
 *
 */
public class DefaultFacetIterator extends FacetIterator {

  private TermValueList _valList;
  private int[] _count;
  private int _index;

  public DefaultFacetIterator(TermValueList valList, int[] counts, boolean zeroBased) {
    _valList = valList;
    _count = counts;
    _index = -1;
    if(!zeroBased)
      _index++;
    facet = null;
    count = 0;
 }


  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return (_index < (_count.length-1));
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public Comparable next() {
    if((_index >= 0) && (_index >= (_count.length-1)))
      throw new NoSuchElementException("No more facets in this iteration");
    _index++;
    facet = _valList.get(_index);
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
  public Comparable next(int minHits)
  {
    while(++_index < _count.length)
    {
      if(_count[_index] >= minHits)
      {
    	facet = (Comparable)_valList.getRawValue(_index);
        count = _count[_index];
        return facet;
      }
    }
    facet = null;
    count = 0;
    return facet;    
  }
}
