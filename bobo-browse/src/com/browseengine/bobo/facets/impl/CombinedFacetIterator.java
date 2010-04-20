/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.lucene.util.PriorityQueue;

import com.browseengine.bobo.api.FacetIterator;

/**
 * @author nnarkhed
 *
 */
public class CombinedFacetIterator extends FacetIterator {
	
  private class IteratorNode
  {
    public FacetIterator _iterator;
    public Comparable _curFacet;
    public int _curFacetCount;

    public IteratorNode(FacetIterator iterator)
    {
      _iterator = iterator;
      _curFacet = null;
      _curFacetCount = 0;
    }

    public boolean fetch(int minHits)
    {
      if(minHits > 0)
        minHits = 1;
      if( (_curFacet = _iterator.next(minHits)) != null)
      {
        _curFacetCount = _iterator.count;
        return true;
      }
      _curFacet = null;
      _curFacetCount = 0;
      return false;
    }

    public Comparable peek()
    {
      if(_iterator.hasNext()) 
      {
        return _iterator.facet;
      }
      return null;
    }
  }

  private final PriorityQueue _queue;

  //private List<FacetIterator> _iterators;

  private CombinedFacetIterator(final int length) {
    _queue = new PriorityQueue() {
      {
        this.initialize(length);
      }
      @Override
      protected boolean lessThan(Object o1, Object o2) {
    	Comparable v1 = ((IteratorNode)o1)._curFacet;
    	Comparable v2 = ((IteratorNode)o2)._curFacet;

        return v1.compareTo(v2) < 0;
      }
    };		
  }

  public CombinedFacetIterator(final List<FacetIterator> iterators) {
    this(iterators.size());
  //  _iterators = iterators;
    for(FacetIterator iterator : iterators) {
      IteratorNode node = new IteratorNode(iterator);
      if(node.fetch(1))
        _queue.add(node);
    }
    facet = null;
    count = 0;
  }

  public CombinedFacetIterator(final List<FacetIterator> iterators, int minHits) {
    this(iterators.size());
 //   _iterators = iterators;
    for(FacetIterator iterator : iterators) {
      IteratorNode node = new IteratorNode(iterator);
      if(node.fetch(minHits))
        _queue.add(node);
    }
    facet = null;
    count = 0;
  }

  /* (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#next()
   */
  public Comparable next() {
    if(!hasNext())
      throw new NoSuchElementException("No more facets in this iteration");

    IteratorNode node = (IteratorNode) _queue.top();

    facet = node._curFacet;
    Comparable next = null;
    count = 0;
    while(hasNext())
    {
      node = (IteratorNode) _queue.top();
      next = node._curFacet;
      if( (next != null) && (!next.equals(facet)) )
        break;
      count += node._curFacetCount;
      if(node.fetch(1))
        _queue.updateTop();
      else
        _queue.pop();
    }
    return facet;
  }

  /**
   * This version of the next() method applies the minHits from the facet spec before returning the facet and its hitcount
   * @param minHits the minHits from the facet spec for CombinedFacetAccessible
   * @return        The next facet that obeys the minHits 
   */
  public Comparable next(int minHits) {
    int qsize = _queue.size();
    if(qsize == 0)
    {
      facet = null;
      count = 0;
      return null;
    }

    IteratorNode node = (IteratorNode) _queue.top();    
    facet = node._curFacet;
    count = node._curFacetCount;
    while(true)
    {
      if(node.fetch(minHits))
      {
        node = (IteratorNode)_queue.updateTop();
      }
      else
      {
        _queue.pop();
        if(--qsize > 0)
        {
          node = (IteratorNode)_queue.top();
        }
        else
        {
          // we reached the end. check if this facet obeys the minHits
          if(count < minHits)
          {
            facet = null;
            count = 0;
          }
          break;
        }
      }
      Comparable next = node._curFacet;
      if(!next.equals(facet))
      {
        // check if this facet obeys the minHits
        if(count >= minHits)
          break;
        // else, continue iterating to the next facet
        facet = next;
        count = node._curFacetCount;
      }
      else
      {
        count += node._curFacetCount;
      }
    }
    return facet;
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
