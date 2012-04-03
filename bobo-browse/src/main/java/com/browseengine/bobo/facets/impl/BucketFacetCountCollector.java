package com.browseengine.bobo.facets.impl;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.lucene.util.BitVector;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermStringList;
import com.browseengine.bobo.facets.data.TermValueList;

public class BucketFacetCountCollector implements FacetCountCollector
{
  private final String _name;
  private final DefaultFacetCountCollector _subCollector;
  private final FacetSpec _ospec;
  private final Map<String,String[]> _predefinedBuckets;
  private int[] _collapsedCounts;
  private TermStringList _bucketValues;
  private final int _numdocs;
  
  protected BucketFacetCountCollector(String name,  DefaultFacetCountCollector subCollector, FacetSpec ospec,Map<String,String[]> predefinedBuckets,int numdocs)
  {
    _name = name;
    _subCollector = subCollector;
    _ospec=ospec;
    _numdocs = numdocs;
    
    _predefinedBuckets = predefinedBuckets;
    _collapsedCounts = null;
    
    _bucketValues = new TermStringList();
    _bucketValues.add("");
    
    String[] bucketArray = _predefinedBuckets.keySet().toArray(new String[0]);
    Arrays.sort(bucketArray);
    for (String bucket : bucketArray){
    	_bucketValues.add(bucket);
    }
    _bucketValues.seal();
  }
  
  private int[] getCollapsedCounts(){
	if (_collapsedCounts==null){
		_collapsedCounts = new int[_bucketValues.size()];
		FacetDataCache dataCache = _subCollector._dataCache;
		TermValueList<?> subList = dataCache.valArray; 
		int[] subcounts = _subCollector._count;
		BitVector indexSet = new BitVector(subcounts.length);
		int c = 0;
		int i = 0;
		for (String val : _bucketValues){
			if (val.length()>0){
				String[] subVals = _predefinedBuckets.get(val);
				int count = 0;
				for (String subVal : subVals){
					int index = subList.indexOf(subVal);
					if (index>0){
						int subcount = subcounts[index];
						count+=subcount;
						if (!indexSet.get(index)){
							indexSet.set(index);
							c+=dataCache.freqs[index];
						}
					}
				}
				_collapsedCounts[i] = count;
			}
			i++;
		}
		_collapsedCounts[0] = (_numdocs-c);
	}
	return _collapsedCounts;
  }
  
 // get the total count of all possible elements 
  public int[] getCountDistribution()
  {
    return getCollapsedCounts();
  }
  
  public String getName()
  {
      return _name;
  }
  
  // get the facet of one particular bucket
  public BrowseFacet getFacet(String bucketValue)
  {
      int index = _bucketValues.indexOf(bucketValue);
      if (index<0){
    	  return new BrowseFacet(bucketValue,0);
      }
      
      int[] counts = getCollapsedCounts();
    
      return new BrowseFacet(bucketValue,counts[index]);
  }
  
  public int getFacetHitsCount(Object value) 
  {
    int index = _bucketValues.indexOf(value);
    if (index<0){
      return 0;
    }
    
    int[] counts = getCollapsedCounts();
  
    return counts[index];
  }

  public final void collect(int docid) {
	  _subCollector.collect(docid);
  }
  
  public final void collectAll()
  {
	  _subCollector.collectAll();
  }
  
  // get facets for all predefined buckets
  public List<BrowseFacet> getFacets() 
  {

	int[] counts = getCollapsedCounts();
    return DefaultFacetCountCollector.getFacets(_ospec, counts, counts.length, _bucketValues);

  }
  
  
  public void close()
  {
	  _subCollector.close();
  }    

  public FacetIterator iterator() 
  {
	int[] counts = getCollapsedCounts();
	return new DefaultFacetIterator(_bucketValues, counts, counts.length, true);
  }  
}

