package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.util.BigSegmentedArray;

public class FacetFilter extends RandomAccessFilter 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	

  private final FacetHandler<FacetDataCache> _facetHandler;
	protected final String _value;
	
	public FacetFilter(FacetHandler<FacetDataCache> facetHandler,String value)
	{
		_facetHandler = facetHandler;
		_value=value;
	}
	
  public double getFacetSelectivity(BoboIndexReader reader)
  {
    double selectivity = 0;
    FacetDataCache dataCache = _facetHandler.getFacetData(reader);
    int idx = dataCache.valArray.indexOf(_value);
    if(idx<0)
    {
      return 0.0;
    }
    int freq =dataCache.freqs[idx];
    int total = reader.maxDoc();
    selectivity = (double)freq/(double)total;
    return selectivity;
  }
  
	public static class FacetDocIdSetIterator extends DocIdSetIterator
	{
		protected int _doc;
		protected final int _index;
		protected final int _maxID;
        protected final BigSegmentedArray _orderArray;
		
		public FacetDocIdSetIterator(FacetDataCache dataCache,int index)
		{
			_index=index;
			_doc=Math.max(-1,dataCache.minIDs[_index]-1);
			_maxID = dataCache.maxIDs[_index];
			_orderArray = dataCache.orderArray;
		}
		
		@Override
		final public int docID() {
			return _doc;
		}

		@Override
		public int nextDoc() throws IOException
		{
          return (_doc = (_doc < _maxID ? _orderArray.findValue(_index, (_doc + 1), _maxID) : NO_MORE_DOCS));
		}

		@Override
		public int advance(int id) throws IOException
		{
          if (_doc < id)
          {
            return (_doc = (id <= _maxID ? _orderArray.findValue(_index, id, _maxID) : NO_MORE_DOCS));
          }
          return nextDoc();
        }

	}

	@Override
	public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboIndexReader reader) throws IOException 
	{
		FacetDataCache dataCache = _facetHandler.getFacetData(reader);
		int index = dataCache.valArray.indexOf(_value);
		if (index < 0)
		{
		  return EmptyDocIdSet.getInstance();
		}
		else
		{
			return new FacetDataRandomAccessDocIdSet(dataCache, index);
		}
	}

	public static class FacetDataRandomAccessDocIdSet extends RandomAccessDocIdSet{

		private final FacetDataCache _dataCache;
	    private final BigSegmentedArray _orderArray;
	    private final int _index;
	    
	    FacetDataRandomAccessDocIdSet(FacetDataCache dataCache,int index){
	    	_dataCache = dataCache;
	    	_orderArray = _dataCache.orderArray;
	    	_index = index;
	    }
		@Override
		public boolean get(int docId) {
			return _orderArray.get(docId) == _index;
		}

		@Override
		public DocIdSetIterator iterator() throws IOException {
			return new FacetDocIdSetIterator(_dataCache,_index);
		}
		
	}
}
