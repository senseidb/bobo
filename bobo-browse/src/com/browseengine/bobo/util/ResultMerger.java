package com.browseengine.bobo.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

public class ResultMerger {
	
	public static <T> Iterator<T> mergeResults(final Iterator<T>[] results,final Comparator<T> comparator){
		
		return new Iterator<T>(){
			TreeMap<T,Iterator<T>> map=new TreeMap<T,Iterator<T>>(comparator);
			{
				for (Iterator<T> result : results)
				{
					if (result.hasNext())
					{
						map.put(result.next(),result);
					}
				}
			}
			
			public boolean hasNext() {
				return map.size()>0;
			}

			public T next() {
				T first=map.firstKey();
				Iterator<T> iter=map.remove(first);
				while (iter.hasNext())
				{
					T next=iter.next();
					if (!map.containsKey(next))
					{
						map.put(next,iter);
						break;
					}
				}
				return first;
			}

			public void remove() {
				T first=map.firstKey();
				Iterator<T> iter=map.remove(first);
				while (iter.hasNext())
				{
					T next=iter.next();
					if (!map.containsKey(next))
					{
						map.put(next,iter);
						break;
					}
				}
			}
		};
	}
	
	/*public static <F extends BrowseFacet> Iterator<F> mergeFacets(Iterator<F>[] facetList,final Comparator<F> comparator){
		return null;
	}
	*/
}
