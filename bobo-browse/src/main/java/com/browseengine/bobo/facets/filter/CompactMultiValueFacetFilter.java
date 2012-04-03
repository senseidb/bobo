package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.util.BigSegmentedArray;

public class CompactMultiValueFacetFilter extends RandomAccessFilter {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;	
	private FacetHandler<FacetDataCache> _facetHandler;
	
	private final String[] _vals;
	
	public CompactMultiValueFacetFilter(FacetHandler<FacetDataCache> facetHandler,String val)
    {
      this(facetHandler,new String[]{val});
    }
	
	public CompactMultiValueFacetFilter(FacetHandler<FacetDataCache> facetHandler,String[] vals)
	{
		_facetHandler = facetHandler;
		_vals = vals;
	}
	
	public double getFacetSelectivity(BoboIndexReader reader)
  {
    double selectivity = 0;
    FacetDataCache dataCache = _facetHandler.getFacetData(reader);
    int[] idxes = FacetDataCache.convert(dataCache,_vals);
    if(idxes == null)
    {
      return 0.0;
    }
    int accumFreq=0;
    for(int idx : idxes)
    {
      accumFreq +=dataCache.freqs[idx];
    }
    int total = reader.maxDoc();
    selectivity = (double)accumFreq/(double)total;
    if(selectivity > 0.999) 
    {
      selectivity = 1.0;
    }
    return selectivity;
  }
	
	private final static class CompactMultiValueFacetDocIdSetIterator extends DocIdSetIterator
	{
	    private final int _bits;
	    private int _doc;
	    private int _maxID;
	    private final BigSegmentedArray _orderArray;
	    
		public CompactMultiValueFacetDocIdSetIterator(FacetDataCache dataCache,int[] index,int bits) {
			_bits = bits;
			_doc = Integer.MAX_VALUE;
	        _maxID = -1;
	        _orderArray = dataCache.orderArray;
	        for (int i : index)
	        {
	          if (_doc > dataCache.minIDs[i]){
	            _doc = dataCache.minIDs[i];
	          }
	          if (_maxID < dataCache.maxIDs[i])
	          {
	            _maxID = dataCache.maxIDs[i];
	          }
	        }
	        _doc--;
	        if (_doc<0) _doc=-1;
		}
		
		@Override
		public final int docID()
		{
		  return _doc;
		}

		@Override
        public final int nextDoc() throws IOException
        {
          return (_doc = (_doc < _maxID ? _orderArray.findBits(_bits, (_doc + 1), _maxID) : NO_MORE_DOCS));
        }

        @Override
        public final int advance(int id) throws IOException
        {
          if (_doc < id)
          {
            return (_doc = (id <= _maxID ? _orderArray.findBits(_bits, id, _maxID) : NO_MORE_DOCS));
          }
          return nextDoc();
        }
	}
	
	@Override
	public RandomAccessDocIdSet getRandomAccessDocIdSet(final BoboIndexReader reader) throws IOException 
	{
		final FacetDataCache dataCache = _facetHandler.getFacetData(reader);
		final int[] indexes = FacetDataCache.convert(dataCache,_vals);
		
		int bits;
		bits = 0x0;
		for (int i : indexes)
		{
			bits |= 0x00000001 << (i-1);  
		}
		
		final int finalBits = bits;
		
		final BigSegmentedArray orderArray = dataCache.orderArray;
		
		if (indexes.length == 0)
		{
			return EmptyDocIdSet.getInstance();
		}
		else
		{
			return new RandomAccessDocIdSet()
			{
				@Override
				public DocIdSetIterator iterator() 
				{
					return new CompactMultiValueFacetDocIdSetIterator(dataCache,indexes,finalBits);
				}

		        @Override
		        final public boolean get(int docId)
		        {
		          return (orderArray.get(docId) & finalBits) != 0x0;
		        }
			};
		}
	}

}
