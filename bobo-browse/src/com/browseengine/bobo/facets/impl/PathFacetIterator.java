/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.util.List;
import java.util.NoSuchElementException;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.FacetIterator;

/**
 * @author nnarkhed
 *
 */
public class PathFacetIterator implements FacetIterator {

	private BrowseFacet[] _facets;
	private int _index;
	private String _facet;
	private int _count;
	
	public PathFacetIterator(List<BrowseFacet> facets) {
		_facets = facets.toArray(new BrowseFacet[facets.size()]);
		_index = -1;
		_facet = null;
		_count = 0;
	}
	
	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetIterator#getFacet()
	 */
	public String getFacet() {
      return _facet;
	}

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetIterator#getFacetCount()
	 */
	public int getFacetCount() {
      return _count;
	}

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetIterator#next()
	 */
	public String next() {
		if((_index >= 0) && !hasNext())
			throw new NoSuchElementException("No more facets in this iteration");
		_index++;
		_facet = _facets[_index].getValue();
		_count = _facets[_index].getHitCount();
		return _facet;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext() {
		return ( (_index >= 0) && (_index < (_facets.length-1)) );
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
    while(++_index < _facets.length)
    {
      if(_facets[_index].getHitCount() >= minHits)
      {
        _facet = _facets[_index].getValue();
        _count = _facets[_index].getHitCount();
        return _facet;
      }
    }
    _facet = null;
    _count = 0;
    return _facet;  
  }
}
