package com.browseengine.bobo.facets.impl;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FieldValueAccessor;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.data.TermStringList;
import com.browseengine.bobo.util.IntBoundedPriorityQueue;
import com.browseengine.bobo.util.IntBoundedPriorityQueue.IntComparator;

public class BucketFacetCountCollector implements FacetCountCollector
{
  private final String _name;
  private final FacetCountCollector _dependsOnCollector;
  private final FacetSpec _ospec;
  private final TermStringList _predefinedBuckets;
  private final int _predefinedBucketsSize;
  
  protected BucketFacetCountCollector(String name,  FacetCountCollector dependsOnCollector, FacetSpec ospec, List<String> predefinedBuckets)
  {
    _name = name;
    _dependsOnCollector = dependsOnCollector;
    _ospec=ospec;
    
    if(predefinedBuckets != null)
    {
      _predefinedBuckets = new TermStringList();
      _predefinedBuckets.addAll(predefinedBuckets);
      _predefinedBucketsSize = _predefinedBuckets.size();
    }
    else
    {
      _predefinedBuckets = null;
      _predefinedBucketsSize = 0;
    }
  }
  
 // get the total count of all possible elements 
  public int[] getCountDistribution()
  {
    int dist[];
    int distSize = 0;
   
    if(_predefinedBuckets != null)
    {
      dist = new int[_predefinedBuckets.size()];
      for(String bucketString : _predefinedBuckets)
      {
        BrowseFacet facet = getFacet(bucketString);
        if(facet != null)
        {
          dist[distSize++] = facet.getFacetValueHitCount();
        }
      }
    }
    else
    {
      dist = _dependsOnCollector.getCountDistribution();
    }
    
    return dist;
  }
  
  public String getName()
  {
      return _name;
  }
  
  // get the facet of one particular bucket
  public BrowseFacet getFacet(String bucketValue)
  {
      BrowseFacet facet = null;
      int sum=0;
      
      String[] elems = BucketFacetHandler.convertBucketStringsToNonOverlapElementStrings(new String[]{bucketValue});
      for(String elem : elems)
      {
        BrowseFacet curFacet = _dependsOnCollector.getFacet(elem);
        sum += curFacet.getFacetValueHitCount();
      }
    
      facet = new BrowseFacet(bucketValue, sum);
      return facet;
  }
  
  public final void collect(int docid) {
    _dependsOnCollector.collect(docid);
  }
  
  public final void collectAll()
  {
    _dependsOnCollector.collectAll();
  }
  
  // get facets for all predefined buckets
  public List<BrowseFacet> getFacets() 
  {
    if (_ospec!=null)
    {
      if (_predefinedBuckets!=null)
      {
        int minCount=_ospec.getMinHitCount();
        List<BrowseFacet> list = new ArrayList<BrowseFacet>(_predefinedBucketsSize);

        int maxNumOfFacets = _ospec.getMaxCount();
        if (maxNumOfFacets <= 0 || maxNumOfFacets > _predefinedBucketsSize) maxNumOfFacets = _predefinedBucketsSize;
        
        // elements in bucketCount must match those in _predefinedBuckets 
        int[] bucketCount = new int[_predefinedBucketsSize];
        int k=0;
        for(String bucketString : _predefinedBuckets)
        {
          bucketCount[k++] = getFacet(bucketString).getFacetValueHitCount();
        }
        List<BrowseFacet> facetColl;
        FacetSortSpec sortspec = _ospec.getOrderBy();
        if (sortspec == FacetSortSpec.OrderValueAsc)
        {
          facetColl = new ArrayList<BrowseFacet>(maxNumOfFacets);
          for (k=0;k<_predefinedBucketsSize;++k)
          {
            if(bucketCount[k] >= minCount)
            {
              BrowseFacet choice=new BrowseFacet(_predefinedBuckets.get(k), bucketCount[k]);
              facetColl.add(choice);
            }
            if(facetColl.size() >= maxNumOfFacets) break;
          }
        }
        else //if (sortspec == FacetSortSpec.OrderHitsDesc)
        {
          ComparatorFactory comparatorFactory;
          if (sortspec == FacetSortSpec.OrderHitsDesc)
          {
            comparatorFactory = new FacetHitcountComparatorFactory();
          }
          else
          {
            comparatorFactory = _ospec.getCustomComparatorFactory();
          }

          if (comparatorFactory == null){
            throw new IllegalArgumentException("facet comparator factory not specified");
          }

          final IntComparator comparator = comparatorFactory.newComparator(new FieldValueAccessor(){
              public String getFormatedValue(int index)
              {
                return _predefinedBuckets.get(index);
              }

              public Object getRawValue(int index) {
                return _predefinedBuckets.getRawValue(index);
              }
            }, bucketCount);
          
          final int forbidden = -1;
          // pq's size is  maxCount
          IntBoundedPriorityQueue pq=new IntBoundedPriorityQueue(comparator, maxNumOfFacets, forbidden);
          for (int i=0; i<_predefinedBucketsSize; ++i)
          {
            if (bucketCount[i]>=minCount)  pq.offer(i);
          }

          int val;
          facetColl=new LinkedList<BrowseFacet>();
          while((val = pq.pollInt()) != forbidden)
          {
            BrowseFacet facet=new BrowseFacet(_predefinedBuckets.get(val),bucketCount[val]);
            ((LinkedList<BrowseFacet>)facetColl).addFirst(facet);
          }
        }
        return facetColl;
      }
      else
      {
        return FacetCountCollector.EMPTY_FACET_LIST;
      }
    }
    else
    {
      return FacetCountCollector.EMPTY_FACET_LIST;
    }

  }
  
  
  public void close()
  {
    // TODO Auto-generated method stub
  }    

  public FacetIterator iterator() 
  {
    if(_predefinedBuckets != null) 
    {
      int[] bucketCounts = new int[_predefinedBuckets.size()];
      for(int i=0; i<_predefinedBuckets.size(); ++i)
      {
        BrowseFacet facet = getFacet(_predefinedBuckets.get(i));
        bucketCounts[i] = facet.getFacetValueHitCount();
      }
      
      return new DefaultFacetIterator(_predefinedBuckets, bucketCounts, bucketCounts.length, true);
    }
    return null;
  }  
}

