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
	
	public DefaultFacetIterator(List<String> valList, int[] count) {
		_valList = valList;
		_count = count;
		_index = 0;
		// some facet handlers need to start from 1 instead of 0
		if(getFacet().equals(""))
			next();
	}
	
	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetIterator#getFacet()
	 */
	public String getFacet() {
		if(!hasNext())
			throw new NoSuchElementException("No more facets in this iteration");
		return _valList.get(_index);
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
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return (_index < _count.length);
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#next()
	 */
	public Object next() {
		if(!hasNext())
			throw new NoSuchElementException("No more facets in this iteration");
		_index++;
		return null;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {
		throw new UnsupportedOperationException("remove() method not supported for Facet Iterators");
	}

}
