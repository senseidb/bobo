package com.browseengine.bobo.facets;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.util.PriorityQueue;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.impl.CombinedFacetIterator;

/**
 * @author nnarkhed
 *
 */
public class CombinedFacetAccessible implements FacetAccessible 
{

  private final List<FacetAccessible> _list;
  private final FacetSpec _fspec;
  
  public CombinedFacetAccessible(FacetSpec fspec,List<FacetAccessible> list)
  {
    _list = list;
    _fspec = fspec;
  }

  public String toString() 
  {
    return "_list:"+_list+" _fspec:"+_fspec;
  }

  public BrowseFacet getFacet(String value) 
  {
    int sum=-1;
    String foundValue=null;
    if (_list!=null)
    {
      for (FacetAccessible facetAccessor : _list)
      {
        BrowseFacet facet = facetAccessor.getFacet(value);
        if (facet!=null)
        {
          foundValue = facet.getValue();
          if (sum==-1) sum=facet.getHitCount();
          else sum+=facet.getHitCount();
        }
      }
    }
    if (sum==-1) return null;
    return new BrowseFacet(foundValue,sum);
  }

  public List<BrowseFacet> getFacets() 
  {
    int maxCnt = _fspec.getMaxCount();
    if(maxCnt <= 0)
      maxCnt = Integer.MAX_VALUE;
    int minHits = _fspec.getMinHitCount();
    LinkedList<BrowseFacet> list = new LinkedList<BrowseFacet>();

    int cnt = 0;
    String facet = null;
    CombinedFacetIterator iter = (CombinedFacetIterator)this.iterator();
    int count = 0;
    Comparator<BrowseFacet> comparator;
    if (FacetSortSpec.OrderValueAsc.equals(_fspec.getOrderBy()))
    {
      while((facet = iter.next(minHits)) != null) 
      {
        // find the next facet whose combined hit count obeys minHits
        count = iter.getFacetCount();
        list.add(new BrowseFacet(facet, count));
        if(++cnt >= maxCnt) break;                  
      }
    }
    else if(FacetSortSpec.OrderHitsDesc.equals(_fspec.getOrderBy()))
    {
      comparator = new Comparator<BrowseFacet>()
      {
        public int compare(BrowseFacet f1, BrowseFacet f2)
        {
          int val=f2.getHitCount() - f1.getHitCount();
          if (val==0)
          {
            val = (f1.getValue().compareTo(f2.getValue()));
          }
          return val;
        }
      };       
      if(maxCnt != Integer.MAX_VALUE)
      {
        // we will maintain a min heap of size maxCnt
        // Order by hits in descending order and max count is supplied
        PriorityQueue queue = createPQ(maxCnt, comparator);
        int qsize = 0;
        while( (qsize < maxCnt) && ((facet = iter.next(minHits)) != null) )
        {
          count = iter.getFacetCount();
          queue.add(new BrowseFacet(facet, count));
          qsize++;
        }
        if(facet != null)
        {
          BrowseFacet rootFacet = (BrowseFacet)queue.top();
          while(((facet = iter.next(minHits)) != null))
          {
            // check with the top of min heap
            count = iter.getFacetCount();
            // if facet count less than top of min heap, it should never be added 
            if(count > rootFacet.getHitCount())
            {
              rootFacet.setValue(facet);
              rootFacet.setHitCount(count);
              rootFacet = (BrowseFacet) queue.updateTop();
              minHits = rootFacet.getHitCount() + 1;
            }
          }
        }
        // at this point, queue contains top maxCnt facets that have hitcount >= minHits
        while(qsize-- > 0)
        {
          // append each entry to the beginning of the facet list to order facets by hits descending
          list.addFirst((BrowseFacet) queue.pop());
        }
      }
      else
      {
        // no maxCnt specified. So fetch all facets according to minHits and sort them later
        while((facet = iter.next(minHits)) != null)
          list.add(new BrowseFacet(facet, iter.getFacetCount()));
        Collections.sort(list, comparator);
      }
    }
    else // FacetSortSpec.OrderByCustom.equals(_fspec.getOrderBy()
    {
      comparator = _fspec.getCustomComparatorFactory().newComparator();
      if(maxCnt != Integer.MAX_VALUE)
      {
        PriorityQueue queue = createPQ(maxCnt, comparator);
        BrowseFacet browseFacet = new BrowseFacet();        
        int qsize = 0;
        while( (qsize < maxCnt) && ((facet = iter.next(minHits)) != null) )
        {
          count = iter.getFacetCount();
          queue.add(new BrowseFacet(facet, count));
          qsize++;
        }
        if(facet != null)
        {
          while((facet = iter.next(minHits)) != null)
          {
            // check with the top of min heap
            // if facet count less than top of min heap, it should never be added 
            browseFacet.setHitCount(count);
            browseFacet.setValue(facet);
            BrowseFacet ejectedFacet = (BrowseFacet)queue.insertWithOverflow(browseFacet);
            if(ejectedFacet != browseFacet)
              browseFacet = ejectedFacet;
          }
        }
        // remove from queue and add to the list
        while(qsize-- > 0)
          list.addFirst((BrowseFacet)queue.pop());
      }
      else 
      {
        // order by custom but no max count supplied
        while((facet = iter.next(minHits)) != null)
          list.add(new BrowseFacet(facet, iter.getFacetCount()));
        Collections.sort(list, comparator);
      }
    }
    return list;
  }

  private PriorityQueue createPQ(final int max, final Comparator<BrowseFacet> comparator)
  {
    PriorityQueue queue = new PriorityQueue()
    {
      {
        this.initialize(max);
      }
      @Override
      protected boolean lessThan(Object arg0, Object arg1)
      {
        BrowseFacet o1 = (BrowseFacet)arg0;
        BrowseFacet o2 = (BrowseFacet)arg1;
        return comparator.compare(o1, o2) > 0;
      }     
    };
    return queue;
  }

  public void close()
  {
    if (_list!=null)
    {
      for(FacetAccessible fa : _list)
      {
        fa.close();
      }
    }
  }

  public FacetIterator iterator() {

    ArrayList<FacetIterator> iterList = new ArrayList<FacetIterator>(_list.size());
    FacetIterator iter;
    for (FacetAccessible facetAccessor : _list)
    {
      iter = (FacetIterator) facetAccessor.iterator();
      if(iter != null)
        iterList.add(iter);
    }
    return new CombinedFacetIterator(iterList, _fspec.getMinHitCount());
  }
}
