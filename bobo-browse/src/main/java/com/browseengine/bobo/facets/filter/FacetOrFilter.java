package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.OpenBitSet;

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
  
  public double getFacetSelectivity(BoboIndexReader reader)
  {
    double selectivity = 0;
    FacetDataCache dataCache = _facetHandler.getFacetData(reader);
    int accumFreq=0;
    for(String value : _vals)
    {
      int idx = dataCache.valArray.indexOf(value);
      if(idx < 0)
      {
        continue;
      }
      accumFreq +=dataCache.freqs[idx];
    }
    int total = reader.maxDoc();
    selectivity = (double)accumFreq/(double)total;
    if(selectivity > 0.999)
    {
      selectivity = 1.0;
    }
    if(_takeCompliment)
    {
      selectivity = 1.0 - selectivity;
    }
    return selectivity;
  }
  
  @Override
  public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboIndexReader reader) throws IOException
  {
    if (_vals.length == 0)
    {
      return EmptyDocIdSet.getInstance();
    }
    else
    {
    	return new FacetOrRandomAccessDocIdSet(_facetHandler, reader, _vals, _valueConverter,_takeCompliment);
    }
  }
  
  public static class FacetOrRandomAccessDocIdSet extends RandomAccessDocIdSet{

	private OpenBitSet _bitset;
	private final BigSegmentedArray _orderArray;
	private final FacetDataCache _dataCache;
	private final int[] _index;
	
	FacetOrRandomAccessDocIdSet(FacetHandler<FacetDataCache> facetHandler,BoboIndexReader reader,
								String[] vals,FacetValueConverter valConverter,boolean takeCompliment){
		_dataCache = facetHandler.getFacetData(reader);
		_orderArray = _dataCache.orderArray;
	    _index = valConverter.convert(_dataCache, vals);
	    
	    _bitset = new OpenBitSet(_dataCache.valArray.size());
	    for (int i : _index)
	    {
	      _bitset.fastSet(i);
	    }
      
      if (takeCompliment)
      {
        // flip the bits
        for (int i=0; i < _dataCache.valArray.size();++i){
          _bitset.fastFlip(i);
        }
      }
	}
	
	@Override
	public boolean get(int docId) {
		return _bitset.fastGet(_orderArray.get(docId));
	}

	@Override
	public DocIdSetIterator iterator() throws IOException {
        return new FacetOrDocIdSetIterator(_dataCache,_bitset);
	}
	  
  }
  
  public static class FacetOrDocIdSetIterator extends DocIdSetIterator
  {
      protected int _doc;
      protected final FacetDataCache _dataCache;
      protected int _maxID;
      protected final OpenBitSet _bitset;
      protected final BigSegmentedArray _orderArray;
      
      public FacetOrDocIdSetIterator(FacetDataCache dataCache, OpenBitSet bitset)
      {
          _dataCache=dataCache;
          _orderArray = dataCache.orderArray;
          _bitset=bitset;
              
          _doc = Integer.MAX_VALUE;
          _maxID = -1;
          int size = _dataCache.valArray.size();
          for (int i=0;i<size;++i) {
            if (!bitset.fastGet(i)){
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
      
      @Override
      public int nextDoc() throws IOException
      {
        return (_doc = (_doc < _maxID ? _orderArray.findValues(_bitset, (_doc + 1), _maxID) : NO_MORE_DOCS));
      }

      @Override
      public int advance(int id) throws IOException
      {
        if (_doc < id)
        {
          return (_doc = (id <= _maxID ? _orderArray.findValues(_bitset, id, _maxID) : NO_MORE_DOCS));
        }
        return nextDoc();
      }
  }

}
