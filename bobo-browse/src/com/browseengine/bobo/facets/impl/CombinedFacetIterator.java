/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.lucene.util.PriorityQueue;

import com.browseengine.bobo.api.FacetIterator;

/**
 * @author nnarkhed
 *
 */
public class CombinedFacetIterator implements FacetIterator {

    private class IteratorNode
    {
      public FacetIterator _iterator;
      public String _curFacet;
      public int _curFacetCount;
      
      public IteratorNode(FacetIterator iterator)
      {
        _iterator = iterator;
        _curFacet = null;
        _curFacetCount = 0;
      }
        
      public boolean fetch()
      {
        if(_iterator.hasNext())
        {
          _curFacet = _iterator.getFacet();
          _curFacetCount = _iterator.getFacetCount();
          _iterator.next();
          return true;
        }
        _curFacet = null;
        _curFacetCount = 0;
        return false;
      }
    }

    private final PriorityQueue _queue;

	private List<FacetIterator> _iterators;
	
	private CombinedFacetIterator(final int length) {
		_queue = new PriorityQueue() {
			{
				this.initialize(length);
			}
			@Override
			protected boolean lessThan(Object o1, Object o2) {
		          String v1 = ((IteratorNode)o1)._curFacet;
		          String v2 = ((IteratorNode)o2)._curFacet;
		          
		          return v1.compareTo(v2) < 0;
			}
		};		
	}
	
	public CombinedFacetIterator(final List<FacetIterator> iterators) {
		this(iterators.size());
		_iterators = iterators;
		for(FacetIterator iterator : iterators) {
			IteratorNode node = new IteratorNode(iterator);
			if(node.fetch())
				_queue.add(node);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetIterator#getFacet()
	 */
	public String getFacet() {
		if(!hasNext())
			throw new NoSuchElementException("No more facets in this iteration");
		IteratorNode node = (IteratorNode) _queue.top();
		return node._curFacet;
	}

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetIterator#getFacetCount()
	 */
	public int getFacetCount() {
		if(!hasNext())
			throw new NoSuchElementException("No more facets in this iteration");
		IteratorNode node = (IteratorNode) _queue.top();
		return node._curFacetCount;
	}

	/* (non-Javadoc)
	 * @see com.browseengine.bobo.api.FacetIterator#next()
	 */
	public Object next() {
		if(!hasNext())
			throw new NoSuchElementException("No more facets in this iteration");

		IteratorNode ctx = (IteratorNode)_queue.top();
	    if (ctx.fetch())
	    	_queue.updateTop();
	    else
	    	_queue.pop();
	    return null;
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#hasNext()
	 */
	public boolean hasNext() {
		return (_queue.size() > 0);
	}

	/* (non-Javadoc)
	 * @see java.util.Iterator#remove()
	 */
	public void remove() {
		throw new UnsupportedOperationException("remove() method not supported for Facet Iterators");
	}

}
