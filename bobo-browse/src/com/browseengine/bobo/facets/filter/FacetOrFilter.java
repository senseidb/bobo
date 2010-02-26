package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitVector;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.util.BigSegmentedArray;

public class FacetOrFilter extends RandomAccessFilter
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  protected final FacetHandler<FacetDataCache> _facetHandler;
  protected final String[] _vals;
  private final boolean _takeCompliment;
  private final FacetValueConverter _valueConverter;
  
  public FacetOrFilter(FacetHandler<FacetDataCache> facetHandler, String[] vals)
  {
    this(facetHandler,vals,false);
  }
  
  public FacetOrFilter(FacetHandler<FacetDataCache> facetHandler, String[] vals,boolean takeCompliment){
	this(facetHandler,vals,takeCompliment,FacetValueConverter.DEFAULT);  
  }
  
  public FacetOrFilter(FacetHandler<FacetDataCache> facetHandler, String[] vals,boolean takeCompliment,FacetValueConverter valueConverter)
  {
    _facetHandler = facetHandler;
    _vals = vals;
    _takeCompliment = takeCompliment;
    _valueConverter = valueConverter;
  }
  
  @Override
  public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboIndexReader reader) throws IOException
  {
    if (_vals.length == 0)
    {
      final DocIdSet empty = EmptyDocIdSet.getInstance();
        return new RandomAccessDocIdSet()
        {
		    @Override
		    public boolean get(int docId)
		    {
		      return false;
		    }
		
		    @Override
		    public DocIdSetIterator iterator() throws IOException
		    {
		      return empty.iterator();
		    }         
        };
    }
    else
    {
    	return new FacetOrRandomAccessDocIdSet(_facetHandler, reader, _vals, _valueConverter,_takeCompliment);
    }
  }
  
  public static class FacetOrRandomAccessDocIdSet extends RandomAccessDocIdSet{

	private BitVector _bitset;
	private final BigSegmentedArray _orderArray;
	private final FacetDataCache _dataCache;
	private final int[] _index;
	
	FacetOrRandomAccessDocIdSet(FacetHandler<FacetDataCache> facetHandler,BoboIndexReader reader,
								String[] vals,FacetValueConverter valConverter,boolean takeCompliment){
		_dataCache = facetHandler.getFacetData(reader);
		_orderArray = _dataCache.orderArray;
	    _index = valConverter.convert(_dataCache, vals);
	    
	    _bitset = new BitVector(_dataCache.valArray.size());
	    for (int i : _index)
	    {
	      _bitset.set(i);
	    }
	    if (takeCompliment)
	    {
	      // flip the bits
	      for (int i=0;i<_bitset.size();++i){
	    	  if (_bitset.get(i)){
	    		  _bitset.clear(i);
	    	  }
	    	  else{
	    		  _bitset.set(i);
	    	  }
	      }
	    }
	}
	
	@Override
	public boolean get(int docId) {
		return _bitset.get(_orderArray.get(docId));
	}

	@Override
	public DocIdSetIterator iterator() throws IOException {
        return new FacetOrDocIdSetIterator(_dataCache,_index,_bitset);
	}
	  
  }
  
  public static class FacetOrDocIdSetIterator extends DocIdSetIterator
  {
      protected int _doc;
      protected final FacetDataCache _dataCache;
      protected final int[] _index;
      protected int _maxID;
      protected final BitVector _bitset;
      protected final BigSegmentedArray _orderArray;
      
      public FacetOrDocIdSetIterator(FacetDataCache dataCache,int[] index,BitVector bitset)
      {
          _dataCache=dataCache;
          _index=index;
          _orderArray = dataCache.orderArray;
          _bitset=bitset;
              
          _doc = Integer.MAX_VALUE;
          _maxID = -1;
          int size = bitset.size();
          for (int i=0;i<size;++i){
	    	if (!bitset.get(i)){
	    	  continue;
	    	}
            if (_doc > _dataCache.minIDs[i]){
              _doc = _dataCache.minIDs[i];
            }
            if (_maxID < _dataCache.maxIDs[i])
            {
              _maxID = _dataCache.maxIDs[i];
            }
          }
          _doc--;
          if (_doc<0) _doc=-1;
      }
      
      @Override
      final public int docID() {
          return _doc;
      }
      /*
      protected boolean validate(int docid){
          return _dataCache.orderArray.get(docid) == _index;
      }
*/
      @Override
      public int nextDoc() throws IOException {
          while(_doc < _maxID) // not yet reached end
          {
              if (_bitset.get(_orderArray.get(++_doc))){
                  return _doc;
              }
          }
          return DocIdSetIterator.NO_MORE_DOCS;
      }

      @Override
      public int advance(int id) throws IOException {
        if (_doc < id)
        {
          _doc=id-1;
        }
        
        while(_doc < _maxID) // not yet reached end
        {
          if (_bitset.get(_orderArray.get(++_doc))){
            return _doc;
          }
        }
        return DocIdSetIterator.NO_MORE_DOCS;
      }

  }

}
