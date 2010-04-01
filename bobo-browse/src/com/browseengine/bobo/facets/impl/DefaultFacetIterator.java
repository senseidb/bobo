/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.util.List;
import java.util.NoSuchElementException;

import com.browseengine.bobo.api.FacetIterator;

/**
 * @author nnarkhed
 *
 */
public class DefaultFacetIterator implements FacetIterator {

  private List<String> _valList;
  private int[] _count;
  private int _index;

  public DefaultFacetIterator(List<String> valList, int[] count, boolean zeroBased) {
    _valList = valList;
    _count = count;
    _index = -1;
    if(!zeroBased)
      _index++;
  }

  /* (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#getFacet()
   */
  public String getFacet() {
    return _valList.get(_index);
  }

  /* (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#getFacetCount()
   */
  public int getFacetCount() {
    return _count[_index];
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
  public String next() {
    if((_index >= 0) && (_index >= (_count.length-1)))
      throw new NoSuchElementException("No more facets in this iteration");
    _index++;
    return _valList.get(_index);
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new UnsupportedOperationException("remove() method not supported for Facet Iterators");
  }

}
