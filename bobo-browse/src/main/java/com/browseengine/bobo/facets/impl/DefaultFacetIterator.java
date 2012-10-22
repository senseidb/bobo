/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.facets.data.TermValueList;
import com.browseengine.bobo.util.BigSegmentedArray;

/**
 * @author nnarkhed
 *
 */
public class DefaultFacetIterator extends FacetIterator {

  private TermValueList _valList;
  private BigSegmentedArray _count;
  private int _countlength;
  private int _index;
  private int _lastIndex;

  public DefaultFacetIterator(TermValueList valList, BigSegmentedArray counts, int countlength, boolean zeroBased)
  {
    _valList = valList;
    _count = counts;
    _countlength = countlength;
    _index = -1;
    _lastIndex = _countlength - 1;
    if(!zeroBased)
      _index++;
    facet = null;
    count = 0;
  }


  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return (_index < _lastIndex);
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public Comparable next() {
    _index++;
    facet = (Comparable)_valList.getRawValue(_index);
    count = _count.get(_index);
    return format(facet);
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
    while(++_index < _countlength)
    {
      if(_count.get(_index) >= minHits)
      {
    	facet = (Comparable)_valList.getRawValue(_index);
        count = _count.get(_index);
        return format(facet);
      }
    }
    facet = null;
    count = 0;
    return null;    
  }


  @Override
  public String format(Object val)
  {
    return _valList.format(val);
  }
  
}
