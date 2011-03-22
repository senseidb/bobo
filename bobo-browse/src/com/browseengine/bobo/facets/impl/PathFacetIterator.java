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
public class PathFacetIterator extends FacetIterator {

	private BrowseFacet[] _facets;
	private int _index;
	
	/**
	 * @param facets a value ascending sorted list of BrowseFacets
	 */
	public PathFacetIterator(List<BrowseFacet> facets) {
		_facets = facets.toArray(new BrowseFacet[facets.size()]);
		_index = -1;
		facet = null;
		count = 0;
	}
	
	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetIterator#next()
	 */
	public Comparable next() {
		if((_index >= 0) && !hasNext())
			throw new NoSuchElementException("No more facets in this iteration");
		_index++;
		facet = _facets[_index].getValue();
		count = _facets[_index].getFacetValueHitCount();
		return facet;
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
  public Comparable next(int minHits)
  {
    while(++_index < _facets.length)
    {
      if(_facets[_index].getFacetValueHitCount() >= minHits)
      {
        facet = _facets[_index].getValue();
        count = _facets[_index].getFacetValueHitCount();
        return facet;
      }
    }
    facet = null;
    count = 0;
    return facet;  
  }

  /**
   * The string from here should be already formatted. No need to reformat.
   * @see com.browseengine.bobo.api.FacetIterator#format(java.lang.Object)
   */
  @Override
  public String format(Object val)
  {
    return (String)val;
  }
}
