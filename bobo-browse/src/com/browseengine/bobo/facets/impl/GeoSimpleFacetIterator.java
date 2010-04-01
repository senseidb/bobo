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
public class GeoSimpleFacetIterator implements FacetIterator {

	private List<String> _ranges;
	private int[] _count;
	private int _index;
	
	public GeoSimpleFacetIterator(List<String> ranges, int[] rangeCounts) {
		_ranges = ranges;
		_count = rangeCounts;
		_index = -1;
	}
	
	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetIterator#getFacet()
	 */
	public String getFacet() {
		if(!hasNext())
			throw new NoSuchElementException("No more facets in this iteration");
		return _ranges.get(_index);
	}

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetIterator#getFacetCount()
	 */
	public int getFacetCount() {
		if(!hasNext())
			throw new NoSuchElementException("No more facets in this iteration");
		return _count[_index];
	}

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetIterator#next()
	 */
	public String next() {
		if(!hasNext())
			throw new NoSuchElementException("No more facets in this iteration");
		_index++;
		return _ranges.get(_index);
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext() {
		return (_index < _count.length);
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {
		throw new UnsupportedOperationException("remove() method not supported for Facet Iterators");
	}

}
