package com.browseengine.bobo.facets.impl;

import java.util.ArrayList;
import java.util.List;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.filter.FacetRangeFilter;
import com.browseengine.bobo.util.BigSegmentedArray;

public class RangeFacetCountCollector implements FacetCountCollector
{
  private final FacetSpec _ospec;
  private int[] _count;
  private final BigSegmentedArray _array;
  private FacetDataCache _dataCache;
  private final String _name;
  private final List<String> _predefinedRanges;
  private int[][] _predefinedRangeIndexes;
  private int _docBase;
  
  protected RangeFacetCountCollector(String name,FacetDataCache dataCache,int docBase,FacetSpec ospec,List<String> predefinedRanges)
  {
      _name = name;
      _dataCache = dataCache;
	  _count=new int[_dataCache.freqs.length];
      _array = _dataCache.orderArray;
      _docBase = docBase;
      _ospec=ospec;
      _predefinedRanges = predefinedRanges;
      
      if (_predefinedRanges!=null)
      {
          _predefinedRangeIndexes = new int[_predefinedRanges.size()][];
          int i=0;
          for (String range : _predefinedRanges)
          {
              _predefinedRangeIndexes[i++]=FacetRangeFilter.parse(_dataCache,range);
          }
      }
  }
  
  /**
   * gets distribution of the value arrays. This is only valid when predefined ranges are available.
   */
  public int[] getCountDistribution()
  {
    
    int[] dist = null;
    if (_predefinedRangeIndexes!=null)
    {
      dist = new int[_predefinedRangeIndexes.length];
      int n=0;
      for (int[] range : _predefinedRangeIndexes)
      {
        int start = range[0];
        int end = range[1];
        
        int sum = 0;
        for (int i=start;i<end;++i)
        {
          sum += _count[i];
        }
        dist[n++]=sum;
      }
    }
    
    return dist;
  }
  
  public String getName()
  {
      return _name;
  }
  
  public BrowseFacet getFacet(String value)
  {
      BrowseFacet facet = null;
      int[] range = FacetRangeFilter.parse(_dataCache,value);
      if (range!=null)
      {
          int sum=0;
          for (int i=range[0];i<=range[1];++i)
          {
              sum+=_count[i];
          }
          facet = new BrowseFacet(value,sum);
      }
      return facet; 
  }
  
  public final void collect(int docid) {
      _count[_array.get(docid)]++;
  }
  
  public final void collectAll()
  {
    _count = _dataCache.freqs;
  }
  
  void convertFacets(BrowseFacet[] facets){
      int i=0;
      for (BrowseFacet facet : facets){
          int hit=facet.getHitCount();
          String val=facet.getValue();            
          RangeFacet rangeFacet=new RangeFacet();
          rangeFacet.setValues(val,val);
          rangeFacet.setHitCount(hit);
          facets[i++]=rangeFacet;
      }
  }
  
  // this is really crappy, need to fix it
  private BrowseFacet[] foldChoices(BrowseFacet[] choices,int max){
      if (max==0 || choices.length<=max) return choices;      
      ArrayList<RangeFacet> list = new ArrayList<RangeFacet>();

      for (int i=0;i<choices.length;i+=2){    
          RangeFacet rangeChoice=new RangeFacet();
          if ((i+1)<choices.length){
              if (choices instanceof RangeFacet[]){
                  RangeFacet[] rChoices=(RangeFacet[])choices; 
                  String val1=rChoices[i]._lower;
                  String val2=rChoices[i+1]._upper;
                  rangeChoice.setValues(val1,val2);
                  rangeChoice.setHitCount(choices[i].getHitCount()+choices[i+1].getHitCount());
              }
              else{                   
                  rangeChoice.setValues(choices[i].getValue(),choices[i+1].getValue());
                  rangeChoice.setHitCount(choices[i].getHitCount()+choices[i+1].getHitCount());                   
              }
              
          }
          else{               
              if (choices instanceof RangeFacet[]){
                  RangeFacet[] rChoices=(RangeFacet[])choices;
                  rangeChoice.setValues(rChoices[i]._lower,rChoices[i]._upper);
              }
              else{
                  rangeChoice.setValues(choices[i].getValue(),choices[i].getValue());                 
              }                           
              rangeChoice.setHitCount(choices[i].getHitCount());
          }
          list.add(rangeChoice);
      }
      
      RangeFacet[] result=new RangeFacet[list.size()];
      result=(RangeFacet[])list.toArray(result);
      return foldChoices(result,max);
  }
  /*
  private List<BrowseFacet> buildDynamicRanges()
  {
      final TreeSet<BrowseFacet> facetSet=new TreeSet<BrowseFacet>(new Comparator<BrowseFacet>(){
          public int compare(BrowseFacet ch1, BrowseFacet ch2) {
            return ch1.getValue().compareTo(ch2.getValue());
          }
          
      });
      int minCount=_ospec.getMinHitCount();
      for (int i=0;i<_count.length;++i){
          if (_count[i] >= minCount){
              String val=_dataCache.valArray.get(i);
              facetSet.add(new BrowseFacet(val,_count[i]));
          }
      }
      
      if (_ospec.getMaxCount()<=0){
          _ospec.setMaxCount(5);
      }
      int maxCount=_ospec.getMaxCount();
      
      
      BrowseFacet[] facets=new BrowseFacet[facetSet.size()];
      facets=(BrowseFacet[])facetSet.toArray(facets);
      
      if (facetSet.size() < maxCount){        
          convertFacets(facets);
      }
      else{
          facets=foldChoices(facets,maxCount);
      }
      
      return Arrays.asList(facets);
  }*/

  public List<BrowseFacet> getFacets() {
	  if (_ospec!=null){
		  if (_predefinedRangeIndexes!=null)
		  {
			  int minCount=_ospec.getMinHitCount();
			  int[] rangeCounts = new int[_predefinedRangeIndexes.length];
			  for (int i=0;i<_count.length;++i){
				  if (_count[i] >0 ){
					  for (int k=0;k<_predefinedRangeIndexes.length;++k)
					  {
						  if (i>=_predefinedRangeIndexes[k][0] && i<=_predefinedRangeIndexes[k][1])
						  {
							  rangeCounts[k]+=_count[i];
						  }
					  }
				  }
			  }
			  List<BrowseFacet> list=new ArrayList<BrowseFacet>(rangeCounts.length);
			  for (int i=0;i<rangeCounts.length;++i)
			  {
				  if (rangeCounts[i]>=minCount)
				  {
					  BrowseFacet choice=new BrowseFacet();
					  choice.setHitCount(rangeCounts[i]);
					  choice.setValue(_predefinedRanges.get(i));
					  list.add(choice);
				  }
			  }
			  return list;
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
  
  private static class RangeFacet extends BrowseFacet{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    String _lower;
    String _upper;
    
    RangeFacet(){           
    }
    
    void setValues(String lower, String upper) {
        _lower=lower;
        _upper=upper;
        setValue(new StringBuilder("[").append(_lower).append(" TO ").append(_upper).append(']').toString());
    }
  }
}

