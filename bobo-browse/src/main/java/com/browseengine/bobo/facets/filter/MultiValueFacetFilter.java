package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.facets.filter.FacetFilter.FacetDocIdSetIterator;
import com.browseengine.bobo.facets.range.MultiDataCacheBuilder;
import com.browseengine.bobo.util.BigNestedIntArray;

public class MultiValueFacetFilter extends RandomAccessFilter 
{
    private static final long serialVersionUID = 1L;
    
   
    private final String _val;


    private final MultiDataCacheBuilder multiDataCacheBuilder;
    
    @SuppressWarnings("rawtypes")
    public MultiValueFacetFilter(MultiDataCacheBuilder multiDataCacheBuilder, String val)  {
        this.multiDataCacheBuilder = multiDataCacheBuilder;
        _val = val;
    }
    
    public double getFacetSelectivity(BoboIndexReader reader)
    {
      double selectivity = 0;
      MultiValueFacetDataCache dataCache = multiDataCacheBuilder.build(reader);
      int idx = dataCache.valArray.indexOf(_val);
      if(idx<0)
      {
        return 0.0;
      }
      int freq =dataCache.freqs[idx];
      int total = reader.maxDoc();
      selectivity = (double)freq/(double)total;
      return selectivity;
    }
    
    
    public final static class MultiValueFacetDocIdSetIterator extends FacetDocIdSetIterator
    {
        private final BigNestedIntArray _nestedArray;

        public MultiValueFacetDocIdSetIterator(MultiValueFacetDataCache dataCache, int index) 
        {
            super(dataCache, index);
            _nestedArray = dataCache._nestedArray;
        }
        
        @Override
        final public int nextDoc() throws IOException
        {
          return (_doc = (_doc < _maxID ? _nestedArray.findValue(_index, (_doc + 1), _maxID) : NO_MORE_DOCS));
        }

        @Override
        final public int advance(int id) throws IOException
        {
          if(_doc < id)
          {
            return (_doc = (id <= _maxID ? _nestedArray.findValue(_index, id, _maxID) : NO_MORE_DOCS));
          }
          return nextDoc();
        }
    }

    @Override
    public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboIndexReader reader) throws IOException {    	
      final MultiValueFacetDataCache dataCache = multiDataCacheBuilder.build(reader);  
      final int index = dataCache.valArray.indexOf(_val);
        final BigNestedIntArray nestedArray = dataCache._nestedArray; 
        if(index < 0)
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
                    return new MultiValueFacetDocIdSetIterator(dataCache, index);
                }
		        @Override
		        final public boolean get(int docId)
		        {
		          return nestedArray.contains(docId, index);
		        }
                
            };
        }
    }

}
