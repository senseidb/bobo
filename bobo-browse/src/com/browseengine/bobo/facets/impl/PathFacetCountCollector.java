package com.browseengine.bobo.facets.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.util.BigSegmentedArray;
import com.browseengine.bobo.util.BoundedPriorityQueue;
import com.browseengine.bobo.util.ListMerger;

public class PathFacetCountCollector implements FacetCountCollector
{
	private final BrowseSelection _sel;
	private final FacetSpec _ospec;
	protected int[] _count;
	private final String _name;
	private final String _sep;
	private final BigSegmentedArray _orderArray;
	protected final FacetDataCache _dataCache;
	private final ComparatorFactory _comparatorFactory;
	private final int _minHitCount;
	private int _maxCount;
	
	
	PathFacetCountCollector(String name,String sep,BrowseSelection sel,FacetSpec ospec,FacetDataCache dataCache)
	{
		_sel = sel;
		_ospec=ospec;
		_name = name;
        _dataCache = dataCache;
        _sep = sep;
		_count=new int[_dataCache.freqs.length];
		_orderArray = _dataCache.orderArray;
		_minHitCount = ospec.getMinHitCount();
		_maxCount = ospec.getMaxCount();
		if (_maxCount<1){
			_maxCount = _count.length;
		}
		FacetSortSpec sortOption = ospec.getOrderBy();
		switch(sortOption){
		case OrderHitsDesc: _comparatorFactory=new FacetHitcountComparatorFactory(); break;
		case OrderValueAsc: _comparatorFactory=null; break;
		case OrderByCustom: _comparatorFactory=ospec.getCustomComparatorFactory(); break;
		default: throw new IllegalArgumentException("invalid sort option: "+sortOption);
		}
	}
	
	public int[] getCountDistribution()
	{
	  return _count;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public void collect(int docid) {
		_count[_orderArray.get(docid)]++;
	}
	
	public void collectAll()
	{
	    _count = _dataCache.freqs; 
	}
	
	public BrowseFacet getFacet(String value)
	{
	  return null;	
	}
	
	private List<BrowseFacet> getFacetsForPath(String selectedPath,int depth,boolean strict,int minCount,int maxCount)
	{
		LinkedList<BrowseFacet> list=new LinkedList<BrowseFacet>();

        BoundedPriorityQueue<BrowseFacet> pq=null;
        
        if (_comparatorFactory!=null){
        	final Comparator<BrowseFacet> comparator = _comparatorFactory.newComparator();
        	
        	pq=new BoundedPriorityQueue<BrowseFacet>(new Comparator<BrowseFacet>(){

				public int compare(BrowseFacet o1, BrowseFacet o2) {
					return -comparator.compare(o1,o2);				}
        		
        	},maxCount);
        }
        
		String[] startParts=null;
		int startDepth=0;
		
		if (selectedPath!=null && selectedPath.length()>0){					
			startParts=selectedPath.split(_sep);
			startDepth=startParts.length;		
			if (!selectedPath.endsWith(_sep)){
				selectedPath+=_sep;
			}
		}	
		
		String currentPath=null;
		int currentCount=0;
		
		int wantedDepth=startDepth+depth;
		
		int index=0;
		if (selectedPath!=null && selectedPath.length()>0){		
			index=_dataCache.valArray.indexOf(selectedPath);
			if (index<0)
			{
				index=-(index + 1);
			}
		}
		
		for (int i=index;i<_count.length;++i){
			if (_count[i] >= minCount){
				String path=_dataCache.valArray.get(i);
				//if (path==null || path.equals(selectedPath)) continue;						
				
				int subCount=_count[i];
			
				String[] pathParts=path.split(_sep);
				
				int pathDepth=pathParts.length;
							
				if ((startDepth==0) || (startDepth>0 && path.startsWith(selectedPath))){
						StringBuffer buf=new StringBuffer();
						int minDepth=Math.min(wantedDepth, pathDepth);
						for(int k=0;k<minDepth;++k){
							buf.append(pathParts[k]);
							if (!pathParts[k].endsWith(_sep)){
								if (pathDepth!=wantedDepth || k<(wantedDepth-1))
									buf.append(_sep);
							}
						}
						String wantedPath=buf.toString();
						if (currentPath==null){
							currentPath=wantedPath;
							currentCount=subCount;
						}
						else if (wantedPath.equals(currentPath)){
							if (!strict){
								currentCount+=subCount;
							}
						}
						else{	
							boolean directNode=false;
							
							if (wantedPath.endsWith(_sep)){
								if (currentPath.equals(wantedPath.substring(0, wantedPath.length()-1))){
									directNode=true;
								}
							}
							
							if (strict){
								if (directNode){
									currentCount+=subCount;
								}
								else{
									BrowseFacet ch=new BrowseFacet(currentPath,currentCount);
									if (pq!=null){
										pq.add(ch);
									}
									else{
										if (list.size()<maxCount){
									      list.add(ch);
										}
									}
									currentPath=wantedPath;
									currentCount=subCount;
								}
							}
							else{
								if (!directNode){
									BrowseFacet ch=new BrowseFacet(currentPath,currentCount);
									if (pq!=null){
										pq.add(ch);
									}
									else{
										if (list.size()<maxCount){
										  list.add(ch);
										}
									}
									currentPath=wantedPath;
									currentCount=subCount;
								}
								else{
									currentCount+=subCount;
								}
							}
						}
				}
				else{
					break;
				}
			}
		}
		
		if (currentPath!=null && currentCount>0){
			BrowseFacet ch=new BrowseFacet(currentPath,currentCount);
			if (pq!=null){
			  pq.add(ch);
			}
			else{
			  if (list.size()<maxCount){
				  list.add(ch);
			  }
			}
		}
		
		if (pq!=null){
			BrowseFacet val;
			while((val = pq.poll()) != null)
            {
              list.addFirst(val);
            }
		}
		
		return list;
	}

	public List<BrowseFacet> getFacets() {
		Properties props = _sel == null ? null : _sel.getSelectionProperties();
		int depth = PathFacetHandler.getDepth(props);
		boolean strict = PathFacetHandler.isStrict(props);
		
		String[] paths= _sel == null ? null : _sel.getValues();
		if (paths==null || paths.length == 0)
		{
			return getFacetsForPath(null, depth, strict, _minHitCount,_maxCount);
		}
		
		if (paths.length==1) return getFacetsForPath(paths[0],depth,strict,_minHitCount,_maxCount);

		LinkedList<BrowseFacet> finalList=new LinkedList<BrowseFacet>();
		ArrayList<Iterator<BrowseFacet>> iterList = new ArrayList<Iterator<BrowseFacet>>(paths.length);
		for (String path : paths)
		{
			List<BrowseFacet> subList=getFacetsForPath(path, depth, strict,  _minHitCount,_maxCount);
			if (subList.size() > 0)
			{
				iterList.add(subList.iterator());
			}
		}
		Iterator<BrowseFacet> finalIter = ListMerger.mergeLists(iterList.toArray((Iterator<BrowseFacet>[])new Iterator[iterList.size()]), _comparatorFactory==null ? new FacetValueComparatorFactory().newComparator(): _comparatorFactory.newComparator());
		while (finalIter.hasNext())
	    {
			BrowseFacet f = finalIter.next();
			finalList.addFirst(f);
	    }
		return finalList;
	}
	
}

