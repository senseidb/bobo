package com.browseengine.bobo.facets;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.util.PriorityQueue;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.DoubleFacetIterator;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FloatFacetIterator;
import com.browseengine.bobo.api.IntFacetIterator;
import com.browseengine.bobo.api.LongFacetIterator;
import com.browseengine.bobo.api.ShortFacetIterator;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.impl.CombinedDoubleFacetIterator;
import com.browseengine.bobo.facets.impl.CombinedFacetIterator;
import com.browseengine.bobo.facets.impl.CombinedFloatFacetIterator;
import com.browseengine.bobo.facets.impl.CombinedIntFacetIterator;
import com.browseengine.bobo.facets.impl.CombinedLongFacetIterator;
import com.browseengine.bobo.facets.impl.CombinedShortFacetIterator;
import com.browseengine.bobo.facets.impl.DefaultIntFacetIterator;

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
    Comparable facet = null;
    FacetIterator iter = (FacetIterator)this.iterator();
    Comparator<BrowseFacet> comparator;
    if (FacetSortSpec.OrderValueAsc.equals(_fspec.getOrderBy()))
    {
      while((facet = iter.next(minHits)) != null) 
      {
        // find the next facet whose combined hit count obeys minHits
    	BrowseFacet bf = new BrowseFacet(String.valueOf(facet), iter.count);
        bf.setFacetValueScore(iter.score);
        list.add(bf);
        if(++cnt >= maxCnt) break;                  
      }
    }
    else if(FacetSortSpec.OrderHitsDesc.equals(_fspec.getOrderBy()))
    {
      comparator = new Comparator<BrowseFacet>()
      {
        public int compare(BrowseFacet f1, BrowseFacet f2)
        {
          int val=f2.getFacetValueHitCount() - f1.getFacetValueHitCount();
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
          BrowseFacet bf = new BrowseFacet(String.valueOf(facet), iter.count);
          bf.setFacetValueScore(iter.score);
          queue.add(bf);
          qsize++;
        }
        if(facet != null)
        {
          BrowseFacet rootFacet = (BrowseFacet)queue.top();
          minHits = rootFacet.getFacetValueHitCount() + 1;
          // facet count less than top of min heap, it will never be added 
          while(((facet = iter.next(minHits)) != null))
          {
            rootFacet.setValue(String.valueOf(facet));
            rootFacet.setFacetValueHitCount(iter.count);
            rootFacet.setFacetValueScore(iter.score);
            rootFacet = (BrowseFacet) queue.updateTop();
            minHits = rootFacet.getFacetValueHitCount() + 1;
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
        while((facet = iter.next(minHits)) != null){
          BrowseFacet bf = new BrowseFacet(String.valueOf(facet), iter.count);
          bf.setFacetValueScore(iter.score);
          list.add(bf);
        }
        Collections.sort(list, comparator);
      }
    }
    else if(FacetSortSpec.OrderScoreDesc.equals(_fspec.getOrderBy()))
    {
      comparator = new Comparator<BrowseFacet>()
      {
        public int compare(BrowseFacet f1, BrowseFacet f2)
        {
          int val=f2.getFacetValueScore() - f1.getFacetValueScore();
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
          BrowseFacet bf = new BrowseFacet(String.valueOf(facet), iter.count);
          bf.setFacetValueScore(iter.score);
          queue.add(bf);
          qsize++;
        }
        if(facet != null)
        {
          BrowseFacet rootFacet = (BrowseFacet)queue.top();
          minHits = rootFacet.getFacetValueHitCount() + 1;
          // facet count less than top of min heap, it will never be added 
          while(((facet = iter.next(minHits)) != null))
          {
            rootFacet.setValue(String.valueOf(facet));
            rootFacet.setFacetValueHitCount(iter.count);
            rootFacet.setFacetValueScore(iter.score);
            rootFacet = (BrowseFacet) queue.updateTop();
            minHits = rootFacet.getFacetValueHitCount() + 1;
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
        while((facet = iter.next(minHits)) != null){
          BrowseFacet bf = new BrowseFacet(String.valueOf(facet), iter.count);
          bf.setFacetValueScore(iter.score);
          list.add(bf);
        }
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
          BrowseFacet bf = new BrowseFacet(String.valueOf(facet), iter.count);
          bf.setFacetValueScore(iter.score);
          queue.add(bf);
          qsize++;
        }
        if(facet != null)
        {
          while((facet = iter.next(minHits)) != null)
          {
            // check with the top of min heap
            browseFacet.setFacetValueHitCount(iter.count);
            browseFacet.setValue(String.valueOf(facet));
            browseFacet.setFacetValueScore(iter.score);
            browseFacet = (BrowseFacet)queue.insertWithOverflow(browseFacet);
          }
        }
        // remove from queue and add to the list
        while(qsize-- > 0)
          list.addFirst((BrowseFacet)queue.pop());
      }
      else 
      {
        // order by custom but no max count supplied
        while((facet = iter.next(minHits)) != null){
          BrowseFacet bf = new BrowseFacet(String.valueOf(facet), iter.count);
          bf.setFacetValueScore(iter.score);
          list.add(bf);
        }
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
    if (iterList.get(0) instanceof IntFacetIterator)
    {
      ArrayList<IntFacetIterator> il = new ArrayList<IntFacetIterator>();
      for (FacetAccessible facetAccessor : _list)
      {
        iter = (FacetIterator) facetAccessor.iterator();
        if(iter != null)
          il.add((IntFacetIterator) iter);
      }
      return new CombinedIntFacetIterator(il, _fspec.getMinHitCount());
    }
    if (iterList.get(0) instanceof LongFacetIterator)
    {
      ArrayList<LongFacetIterator> il = new ArrayList<LongFacetIterator>();
      for (FacetAccessible facetAccessor : _list)
      {
        iter = (FacetIterator) facetAccessor.iterator();
        if(iter != null)
          il.add((LongFacetIterator) iter);
      }
      return new CombinedLongFacetIterator(il, _fspec.getMinHitCount());
    }
    if (iterList.get(0) instanceof ShortFacetIterator)
    {
      ArrayList<ShortFacetIterator> il = new ArrayList<ShortFacetIterator>();
      for (FacetAccessible facetAccessor : _list)
      {
        iter = (FacetIterator) facetAccessor.iterator();
        if(iter != null)
          il.add((ShortFacetIterator) iter);
      }
      return new CombinedShortFacetIterator(il, _fspec.getMinHitCount());
    }
    if (iterList.get(0) instanceof FloatFacetIterator)
    {
      ArrayList<FloatFacetIterator> il = new ArrayList<FloatFacetIterator>();
      for (FacetAccessible facetAccessor : _list)
      {
        iter = (FacetIterator) facetAccessor.iterator();
        if(iter != null)
          il.add((FloatFacetIterator) iter);
      }
      return new CombinedFloatFacetIterator(il, _fspec.getMinHitCount());
    }
    if (iterList.get(0) instanceof DoubleFacetIterator)
    {
      ArrayList<DoubleFacetIterator> il = new ArrayList<DoubleFacetIterator>();
      for (FacetAccessible facetAccessor : _list)
      {
        iter = (FacetIterator) facetAccessor.iterator();
        if(iter != null)
          il.add((DoubleFacetIterator) iter);
      }
      return new CombinedDoubleFacetIterator(il, _fspec.getMinHitCount());
    }
    return new CombinedFacetIterator(iterList);
  }
}
