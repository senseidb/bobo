package com.browseengine.bobo.facets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;

public class CombinedFacetAccessible implements FacetAccessible {

	private final List<FacetAccessible> _list;
	private final FacetSpec _fspec;
	public CombinedFacetAccessible(FacetSpec fspec,List<FacetAccessible> list)
	{
		_list = list;
		_fspec = fspec;
	}
	
	public String toString() {
		return "_list:"+_list+" _fspec:"+_fspec;
	}
	
	public BrowseFacet getFacet(String value) {
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

	public List<BrowseFacet> getFacets() {
		Map<String,BrowseFacet> facetMap;
		if (FacetSortSpec.OrderValueAsc.equals(_fspec.getOrderBy()))
		{
			facetMap= new TreeMap<String,BrowseFacet>();
		}
		else
		{
			facetMap = new HashMap<String,BrowseFacet>();
		}
		
		for (FacetAccessible facetAccessor : _list)
		{
			Iterator<BrowseFacet> iter = facetAccessor.getFacets().iterator();
			if (facetMap.size() == 0)
			{
				while(iter.hasNext())
				{
					BrowseFacet facet = iter.next();
					facetMap.put(facet.getValue(),facet);
				}
			}
			else
			{
				while(iter.hasNext())
				{
					BrowseFacet facet = iter.next();
					BrowseFacet existing = facetMap.get(facet.getValue());
					if (existing == null)
					{
						facetMap.put(facet.getValue(), facet);
					}
					else
					{
						existing.setHitCount(existing.getHitCount() + facet.getHitCount());
					}
				}
			}
		}
		
        int cnt = 0;
        int maxCnt = _fspec.getMaxCount();
        if(maxCnt <= 0) maxCnt = Integer.MAX_VALUE;
        int minHits = _fspec.getMinHitCount();
        List<BrowseFacet> list = new LinkedList<BrowseFacet>();
		
		if (FacetSortSpec.OrderValueAsc.equals(_fspec.getOrderBy()))
		{
		  for(BrowseFacet facet : facetMap.values())
		  {
		    if(facet.getHitCount() >= minHits)
		    {
		      list.add(facet);
		      if(++cnt >= maxCnt) break;			      
		    }
		  }
		}
		else
		{
		  Comparator<BrowseFacet> comparator;
		  if (FacetSortSpec.OrderHitsDesc.equals(_fspec.getOrderBy()))
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
		  }
		  else // FacetSortSpec.OrderByCustom.equals(_fspec.getOrderBy()
		  {
		    comparator = _fspec.getCustomComparatorFactory().newComparator();
		  }
		  ArrayList<BrowseFacet> facets = new ArrayList<BrowseFacet>(facetMap.values());
		  Collections.sort(facets, comparator);
		  for(BrowseFacet facet : facets)
		  {
		    if(facet.getHitCount() >= minHits)
		    {
		      list.add(facet);
		      if(++cnt >= maxCnt) break;                  
		    }
		  }
		}
		return list;
	}

}
