package com.browseengine.bobo.facets.filter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.util.BigNestedIntArray;
import com.browseengine.bobo.util.BigSegmentedArray;

public final class FacetRangeFilter extends RandomAccessFilter 
{

	private static final long serialVersionUID = 1L;
	private final FacetHandler<FacetDataCache> _facetHandler;
	private final String _rangeString;
	
	public FacetRangeFilter(FacetHandler<FacetDataCache> facetHandler, String rangeString)
	{
		_facetHandler = facetHandler;
		_rangeString = rangeString;
	}
	 
  public double getFacetSelectivity(BoboIndexReader reader)
  {
    double selectivity = 0;
    FacetDataCache dataCache = _facetHandler.getFacetData(reader);
    int[] range = parse(dataCache,_rangeString);
    if (range != null) 
    {
      int accumFreq=0;
      for(int idx=range[0]; idx<=range[1]; ++idx)
      {
        accumFreq +=dataCache.freqs[idx];
      }
      int total = reader.maxDoc();
      selectivity = (double)accumFreq/(double)total;
    }
    if(selectivity > 0.999)
    {
      selectivity = 1.0;
    }
    return selectivity;
  }
  
	private final static class FacetRangeDocIdSetIterator extends DocIdSetIterator
	{
		private int _doc = -1;
	
		private int _minID = Integer.MAX_VALUE;
		private int _maxID = -1;
		private final int _start;
		private final int _end;
		private final BigSegmentedArray _orderArray;
		
		
		FacetRangeDocIdSetIterator(int start,int end,FacetDataCache dataCache)
		{			
			_start=start;
			_end=end;
			for (int i=start;i<=end;++i)
			{			
				_minID = Math.min(_minID, dataCache.minIDs[i]);
				_maxID = Math.max(_maxID, dataCache.maxIDs[i]);
			}
			_doc=Math.max(-1,_minID-1);
			_orderArray = dataCache.orderArray;
		}
		
		@Override
		final public int docID() {
			return _doc;
		}

		@Override
		final public int nextDoc() throws IOException
		{
          return (_doc = (_doc < _maxID ? _orderArray.findValueRange(_start, _end, (_doc + 1), _maxID) : NO_MORE_DOCS));
		}

		@Override
		final public int advance(int id) throws IOException
		{
		  if (_doc < id)
		  {
		    return (_doc = (id <= _maxID ? _orderArray.findValueRange(_start, _end, id, _maxID) : NO_MORE_DOCS));
		  }
          return nextDoc();
		}
	}
	private final static class MultiFacetRangeDocIdSetIterator extends DocIdSetIterator
  {
    private int _doc = -1;
  
    private int _minID = Integer.MAX_VALUE;
    private int _maxID = -1;
    private final int _start;
    private final int _end;
    private final BigNestedIntArray nestedArray;
    
    
    MultiFacetRangeDocIdSetIterator(int start,int end, MultiValueFacetDataCache dataCache)
    {     
      _start=start;
      _end=end;
      for (int i=start;i<=end;++i)
      {     
        _minID = Math.min(_minID, dataCache.minIDs[i]);
        _maxID = Math.max(_maxID, dataCache.maxIDs[i]);
      }
      _doc=Math.max(-1,_minID-1);
      nestedArray = dataCache._nestedArray;
    }
    
    @Override
    final public int docID() {
      return _doc;
    }

    @Override
    final public int nextDoc() throws IOException
    {
          return (_doc = (_doc < _maxID ? nestedArray.findValuesInRange(_start, _end, (_doc + 1), _maxID) : NO_MORE_DOCS));
    }

    @Override
    final public int advance(int id) throws IOException
    {
      if (_doc < id)
      {
        return (_doc = (id <= _maxID ? nestedArray.findValuesInRange(_start, _end, id, _maxID) : NO_MORE_DOCS));
      }
          return nextDoc();
    }
  }
	public static class FacetRangeValueConverter implements FacetValueConverter{
		public static FacetRangeValueConverter instance = new FacetRangeValueConverter();
		private FacetRangeValueConverter(){
			
		}
		public int[] convert(FacetDataCache dataCache, String[] vals) {
			return convertIndexes(dataCache,vals);
		}
		
	}

	public static int[] convertIndexes(FacetDataCache dataCache,String[] vals)
	  {
	    IntList list = new IntArrayList();
	    for (String val : vals)
	    {
	      int[] range = parse(dataCache,val);
	      if ( range!=null)
	      {
	        for (int i=range[0];i<=range[1];++i)
	        {
	          list.add(i);
	        }
	      }
	    }
	    return list.toIntArray();
	  }
	
	public static int[] parse(FacetDataCache dataCache,String rangeString)
	{
		String[] ranges = getRangeStrings(rangeString);
	    String lower=ranges[0];
	    String upper=ranges[1];
	    String includeLower = ranges[2];
	    String includeUpper = ranges[3];
	    
	    boolean incLower = true, incUpper = true;
	    
	    if("false".equals(includeLower))
	      incLower = false;
	    if("false".equals(includeUpper))
	      incUpper = false;
	    
	    if ("*".equals(lower))
	    {
	      lower=null;
	    }
	    
	    if ("*".equals(upper))
	    {
	      upper=null;
	    }
	    
	    int start,end;
	    if (lower==null)
	    {
	    	start=1;
	    }
	    else
	    {
	    	start=dataCache.valArray.indexOf(lower);
	    	if (start<0)
	    	{
	    		start=-(start + 1);
	    	}
	    	else
	    	{
	    	  //when the lower value is in the list, we need to consider if we want this lower value included or not;
	    	  if(incLower == false)
	    	  {
	    	    start++;
	    	  }
	    	  
	    	}
	    }
	    
	    if (upper==null)
	    {
	    	end=dataCache.valArray.size()-1;
	    }
	    else
	    {
	    	end=dataCache.valArray.indexOf(upper);
	    	if (end<0)
	    	{
	    		end=-(end + 1);
	    		end=Math.max(0,end-1);
	    	}
	    	else
	    	{
	    	//when the lower value is in the list, we need to consider if we want this lower value included or not;
	    	  if(incUpper == false)
	    	  {
	    	    end--;
	    	  }
	    	}
	    }
	    
	    return new int[]{start,end};
	}
	public static String[] getRangeStrings(String rangeString)
  {

      
      int index2 = rangeString.indexOf(" TO ");
      boolean incLower = true, incUpper = true;
      
      if(rangeString.trim().startsWith("("))
        incLower = false;
      
      if(rangeString.trim().endsWith(")"))
        incUpper = false;
      
      int index = -1, index3 = -1;
      
      if(incLower == true)
        index=rangeString.indexOf('[');
      else if(incLower == false)
        index=rangeString.indexOf('(');
      
      if(incUpper == true)
        index3=rangeString.indexOf(']');
      else if(incUpper == false)
        index3=rangeString.indexOf(')');
      
      String lower,upper;
      try{
        lower=rangeString.substring(index+1,index2).trim();
        upper=rangeString.substring(index2+4,index3).trim();
      
        return new String[]{lower,upper, String.valueOf(incLower), String.valueOf(incUpper)};
      }
      catch(RuntimeException re){        
        throw re;
      }
  }
	
  @Override
  public RandomAccessDocIdSet getRandomAccessDocIdSet(final BoboIndexReader reader) throws IOException
  {      
    final FacetDataCache dataCache = _facetHandler.getFacetData(reader);

    final boolean multi = dataCache instanceof MultiValueFacetDataCache;    
    final BigNestedIntArray nestedArray = multi ? ((MultiValueFacetDataCache) dataCache)._nestedArray : null;
    final int[] range = parse(dataCache,_rangeString);
    
    if (range == null) return null;
    
    if (range[0]>range[1]){
      return EmptyDocIdSet.getInstance();
    }
    
    if (range[0] == range[1] && range[0]<0){
	  return EmptyDocIdSet.getInstance();
    }
    
    return new RandomAccessDocIdSet()
    {
      int _start = range[0];
      int _end = range[1];
      
      @Override
      final public boolean get(int docId)
      {
        if (multi) {
          nestedArray.containsValueInRange(docId, _start, _end);
        }
        int index = dataCache.orderArray.get(docId);        
        return index >= _start && index <= _end;
      }

      @Override
      public DocIdSetIterator iterator()
      {
        if (multi) {
          return new MultiFacetRangeDocIdSetIterator(_start,_end, (MultiValueFacetDataCache)dataCache);
        } else {
          return new FacetRangeDocIdSetIterator(_start,_end,dataCache);
        }
      }      
    };
  }

	
}
