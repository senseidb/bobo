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
public class CombinedFacetIterator extends FacetIterator {
  
  private FacetIterator[] heap;
  private int size;
  List<FacetIterator> _iterators;
  
  public CombinedFacetIterator(final List<FacetIterator> iterators) {
    _iterators = iterators;
    heap = new FacetIterator[iterators.size() + 1];
    size = 0;
    for(FacetIterator iterator : iterators) {
      if(iterator.next(0) != null)
        add(iterator);
    }
    facet = null;
    count = 0;
  }
  
  private final void add(FacetIterator element) {
    size++;
    heap[size] = element;
    upHeap();
  }

  private final void upHeap() {
    int i = size;
    FacetIterator node = heap[i];            // save bottom node
    Comparable val = node.facet;
    int j = i >>> 1;
    while (j > 0 && val.compareTo(heap[j].facet) < 0) {
      heap[i] = heap[j];              // shift parents down
      i = j;
      j = j >>> 1;
    }
    heap[i] = node;               // install saved node
  }

  private final void downHeap() {
    int i = 1;
    FacetIterator node = heap[i];            // save top node
    Comparable val = node.facet;
    int j = i << 1;               // find smaller child
    int k = j + 1;
    if (k <= size && heap[k].facet.compareTo(heap[j].facet) < 0) {
      j = k;
    }
    while (j <= size && heap[j].facet.compareTo(val) < 0) {
      heap[i] = heap[j];              // shift up child
      i = j;
      j = i << 1;
      k = j + 1;
      if (k <= size && heap[k].facet.compareTo(heap[j].facet) < 0) {
        j = k;
      }
    }
    heap[i] = node;               // install saved node
  }
  
  private final void pop() {
    if (size > 0) {
      heap[1] = heap[size];           // move last to first
      heap[size] = null;              // permit GC of objects
      if(--size > 0) downHeap();                 // adjust heap
    }
  }

  /* (non-Javadoc)
   * @see com.browseengine.bobo.api.FacetIterator#next()
   */
  public Comparable next() {
    if(!hasNext())
      throw new NoSuchElementException("No more facets in this iteration");

    return next(1);
  }

  /**
   * This version of the next() method applies the minHits from the facet spec before returning the facet and its hitcount
   * @param minHits the minHits from the facet spec for CombinedFacetAccessible
   * @return        The next facet that obeys the minHits 
   */
  public Comparable next(int minHits) {
    if(size == 0)
    {
      facet = null;
      count = 0;
      return null;
    }

    FacetIterator node = heap[1];    
    facet = node.facet;
    count = node.count;
    int min = (minHits > 0 ? 1 : 0);
    while(true)
    {
      if(node.next(min) != null)
      {
        downHeap();
        node = heap[1];
      }
      else
      {
        pop();
        if(size > 0)
        {
          node = heap[1];
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
      Comparable next = node.facet;
      if (next==null) throw new RuntimeException();
      if(!next.equals(facet))
      {
        // check if this facet obeys the minHits
        if(count >= minHits)
          break;
        // else, continue iterating to the next facet
        facet = next;
        count = node.count;
      }
      else
      {
        count += node.count;
      }
    }
    return format(facet);
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return (size > 0);
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new UnsupportedOperationException("remove() method not supported for Facet Iterators");
  }

  @Override
  public String format(Object val)
  {
    return _iterators.get(0).format(val);
  }
}
