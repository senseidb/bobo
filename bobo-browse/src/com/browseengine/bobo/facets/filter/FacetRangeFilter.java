package com.browseengine.bobo.facets.filter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.impl.RangeFacetHandler;
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
	
	private final static class FacetRangeDocIdSetIterator extends DocIdSetIterator
	{
		private int _doc = -1;
		private int _totalFreq;
		private int _minID = Integer.MAX_VALUE;
		private int _maxID = -1;
		private final int _start;
		private final int _end;
		private final BigSegmentedArray _orderArray;
		
		
		FacetRangeDocIdSetIterator(int start,int end,FacetDataCache dataCache)
		{
			_totalFreq = 0;
			_start=start;
			_end=end;
			for (int i=start;i<=end;++i)
			{
				_totalFreq +=dataCache.freqs[i];
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
		final public int nextDoc() throws IOException {
			int index;
            while(_doc < _maxID) // not yet reached end
			{
				index=_orderArray.get(++_doc);
				if (index>=_start && index<=_end) return _doc;
			}
			return DocIdSetIterator.NO_MORE_DOCS;
		}

		@Override
		final public int advance(int id) throws IOException {
		  if (_doc < id)
		  {
		    _doc=id-1;
		  }
		  
		  int index;
		  while(_doc < _maxID) // not yet reached end
		  {
		    index=_orderArray.get(++_doc);
		    if (index>=_start && index<=_end) return _doc;
		  }
		  return DocIdSetIterator.NO_MORE_DOCS;
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
		String[] ranges = RangeFacetHandler.getRangeStrings(rangeString);
	    String lower=ranges[0];
	    String upper=ranges[1];
	    
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
	    }
	    
	    return new int[]{start,end};
	}
	
  @Override
  public RandomAccessDocIdSet getRandomAccessDocIdSet(final BoboIndexReader reader) throws IOException
  {      
	final FacetDataCache dataCache = _facetHandler.getFacetData(reader);

    final int[] range = parse(dataCache,_rangeString);
    if (range == null) return null;
    
    return new RandomAccessDocIdSet()
    {
      int _start = range[0];
      int _end = range[1];
      
      @Override
      final public boolean get(int docId)
      {
        int index = dataCache.orderArray.get(docId);
        return index >= _start && index <= _end;
      }

      @Override
      public DocIdSetIterator iterator()
      {
        return new FacetRangeDocIdSetIterator(_start,_end,dataCache);
      }
      
    };
  }

	
}
