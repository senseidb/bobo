package com.browseengine.bobo.facets.impl;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BoboIndexReader.WorkArea;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.AdaptiveFacetFilter;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.MultiValueFacetFilter;
import com.browseengine.bobo.facets.filter.MultiValueORFacetFilter;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.facets.range.MultiDataCacheBuilder;
import com.browseengine.bobo.facets.range.SimpleDataCacheBuilder;
import com.browseengine.bobo.query.scoring.BoboDocScorer;
import com.browseengine.bobo.query.scoring.FacetScoreable;
import com.browseengine.bobo.query.scoring.FacetTermScoringFunctionFactory;
import com.browseengine.bobo.sort.DocComparatorSource;
import com.browseengine.bobo.util.BigNestedIntArray;

public class MultiValueFacetHandler extends FacetHandler<MultiValueFacetDataCache> implements FacetScoreable 
{
  private static Logger logger = Logger.getLogger(MultiValueFacetHandler.class);

 

  protected final TermListFactory _termListFactory;
  protected final String _indexFieldName;

  protected int _maxItems = BigNestedIntArray.MAX_ITEMS;
  protected Term _sizePayloadTerm;
  protected Set<String> _depends;
  
  public MultiValueFacetHandler(String name, 
                                String indexFieldName, 
                                TermListFactory termListFactory, 
                                Term sizePayloadTerm,
                                Set<String> depends) 
  {
    super(name, depends);
    _depends = depends;
    _indexFieldName = (indexFieldName != null ? indexFieldName : name);
    _termListFactory = termListFactory;
    _sizePayloadTerm = sizePayloadTerm;
  }
  
  @Override
	public int getNumItems(BoboIndexReader reader, int id) {
	  MultiValueFacetDataCache data = getFacetData(reader);
	  if (data==null) return 0;
	  return data.getNumItems(id);
	}
  
  public MultiValueFacetHandler(String name, String indexFieldName, TermListFactory termListFactory, Term sizePayloadTerm)
  {
    this(name, indexFieldName, termListFactory, sizePayloadTerm, null);
  }

  public MultiValueFacetHandler(String name, TermListFactory termListFactory, Term sizePayloadTerm) 
  {
    this(name, name, termListFactory, sizePayloadTerm, null);
  }

  public MultiValueFacetHandler(String name, String indexFieldName, TermListFactory termListFactory) 
  {
    this(name, indexFieldName, termListFactory, null, null);
  }

  public MultiValueFacetHandler(String name, TermListFactory termListFactory)
  {
    this(name, name, termListFactory);
  }

  public MultiValueFacetHandler(String name, String indexFieldName)
  {
    this(name, indexFieldName, null);
  }

  public MultiValueFacetHandler(String name)
  {
    this(name, name, null);
  }
  
  public MultiValueFacetHandler(String name, Set<String> depends)
  {
    this(name, name, null, null, depends);
  }
  @Override
  public DocComparatorSource getDocComparatorSource() 
  {
    return new MultiValueFacetDataCache.MultiFacetDocComparatorSource(new MultiDataCacheBuilder(getName(), _indexFieldName));
  }
  
  
  
  public void setMaxItems(int maxItems)
  {
    _maxItems = Math.min(maxItems,BigNestedIntArray.MAX_ITEMS);
  }

  @Override
  public String[] getFieldValues(BoboIndexReader reader,int id) 
  {
	  MultiValueFacetDataCache dataCache = getFacetData(reader);
	  if (dataCache!=null){
      return dataCache._nestedArray.getTranslatedData(id, dataCache.valArray);
	  }
	  return new String[0];
  }
  
  @Override
  public Object[] getRawFieldValues(BoboIndexReader reader,int id){

	MultiValueFacetDataCache dataCache = getFacetData(reader);
	  if (dataCache!=null){
      return dataCache._nestedArray.getRawData(id, dataCache.valArray);
	  }
	  return new String[0];
  }


  @Override
  public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel, final FacetSpec ospec){
	return new FacetCountCollectorSource(){

		@Override
		public FacetCountCollector getFacetCountCollector(
				BoboIndexReader reader, int docBase) {
			MultiValueFacetDataCache dataCache = MultiValueFacetHandler.this.getFacetData(reader);
			return new MultiValueFacetCountCollector(_name,dataCache,docBase,sel, ospec);
		}
	};
    
  }

  @Override
  public MultiValueFacetDataCache load(BoboIndexReader reader) throws IOException
  {
    return load(reader, new WorkArea());
  }

  @Override
  public MultiValueFacetDataCache load(BoboIndexReader reader, WorkArea workArea) throws IOException
  {
	MultiValueFacetDataCache dataCache = new MultiValueFacetDataCache();
    
	dataCache.setMaxItems(_maxItems);

    if(_sizePayloadTerm == null)
    {
    	dataCache.load(_indexFieldName, reader, _termListFactory, workArea);
    }
    else
    {
    	dataCache.load(_indexFieldName, reader, _termListFactory, _sizePayloadTerm);
    }
    return dataCache;
  }

  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop) throws IOException
  {
	MultiValueFacetFilter f= new MultiValueFacetFilter(new MultiDataCacheBuilder(getName(), _indexFieldName), value);
    AdaptiveFacetFilter af = new AdaptiveFacetFilter(new SimpleDataCacheBuilder(getName(), _indexFieldName), f, new String[]{value}, false);
    return af;
  }

  @Override
  public RandomAccessFilter buildRandomAccessAndFilter(String[] vals,Properties prop) throws IOException
  {

    ArrayList<RandomAccessFilter> filterList = new ArrayList<RandomAccessFilter>(vals.length);

    for (String val : vals)
    {
      RandomAccessFilter f = buildRandomAccessFilter(val, prop);
      if(f != null) 
      {
        filterList.add(f); 
      }
      else
      {
        return EmptyFilter.getInstance();
      }
    }
    if (filterList.size() == 1) return filterList.get(0);
    return new RandomAccessAndFilter(filterList);
  }

  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,Properties prop,boolean isNot) throws IOException
  {
    RandomAccessFilter filter = null;
    if (vals.length > 1)
    {
      MultiValueORFacetFilter f = new MultiValueORFacetFilter(this,vals,false);			// catch the "not" case later
      if (!isNot) {
	      AdaptiveFacetFilter af = new AdaptiveFacetFilter(new SimpleDataCacheBuilder(getName(), _indexFieldName), f, vals, false);
	      return af;
      }
      else{
    	  filter = f;
      }
    }
    else if(vals.length == 1)
    {
      filter = buildRandomAccessFilter(vals[0],prop);
    }
    else
    {
      filter = EmptyFilter.getInstance();
    }
    
    if (isNot)
    {
      filter = new RandomAccessNotFilter(filter);
    }
    return filter;
  }
  
  public BoboDocScorer getDocScorer(BoboIndexReader reader,FacetTermScoringFunctionFactory scoringFunctionFactory,Map<String,Float> boostMap){
	    MultiValueFacetDataCache dataCache = getFacetData(reader);
		float[] boostList = BoboDocScorer.buildBoostList(dataCache.valArray, boostMap);
		return new MultiValueDocScorer(dataCache,scoringFunctionFactory,boostList);
  }

  public static final class MultiValueDocScorer extends BoboDocScorer{
		private final MultiValueFacetDataCache _dataCache;
		private final BigNestedIntArray _array;
		
		public MultiValueDocScorer(MultiValueFacetDataCache dataCache,FacetTermScoringFunctionFactory scoreFunctionFactory,float[] boostList){
			super(scoreFunctionFactory.getFacetTermScoringFunction(dataCache.valArray.size(), dataCache._nestedArray.size()),boostList);
			_dataCache = dataCache;
			_array = _dataCache._nestedArray;
		}
		
		@Override
		public Explanation explain(int doc){
			String[] vals = _array.getTranslatedData(doc, _dataCache.valArray);
			
			FloatList scoreList = new FloatArrayList(_dataCache.valArray.size());
			ArrayList<Explanation> explList = new ArrayList<Explanation>(scoreList.size());
			for (String val : vals)
			{
				int idx = _dataCache.valArray.indexOf(val);
				if (idx>=0){
				  scoreList.add(_function.score(_dataCache.freqs[idx], _boostList[idx]));
				  explList.add(_function.explain(_dataCache.freqs[idx], _boostList[idx]));
				}
			}
			Explanation topLevel = _function.explain(scoreList.toFloatArray());
			for (Explanation sub : explList){
				topLevel.addDetail(sub);
			}
			return topLevel;
		}
		
		@Override
		public final float score(int docid) {
			return _array.getScores(docid, _dataCache.freqs, _boostList, _function);
		}
		
	}

  public static final class MultiValueFacetCountCollector extends DefaultFacetCountCollector
  {
    public final BigNestedIntArray _array;
    MultiValueFacetCountCollector(String name,
    							  MultiValueFacetDataCache dataCache,
    							  int docBase,
    							  BrowseSelection sel,
                                  FacetSpec ospec)
                                  {
      super(name,dataCache,docBase,sel,ospec);
      _array = dataCache._nestedArray;
    }

    @Override
    public final void collect(int docid) 
    {
      _array.countNoReturn(docid, _count);
    }

    @Override
    public final void collectAll()
    {
      _count = _dataCache.freqs;
    }
  }
}
