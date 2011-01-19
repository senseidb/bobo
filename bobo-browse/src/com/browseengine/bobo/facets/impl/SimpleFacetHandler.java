package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Explanation;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.AdaptiveFacetFilter;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.FacetFilter;
import com.browseengine.bobo.facets.filter.FacetOrFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.facets.filter.AdaptiveFacetFilter.FacetDataCacheBuilder;
import com.browseengine.bobo.query.scoring.BoboDocScorer;
import com.browseengine.bobo.query.scoring.FacetScoreable;
import com.browseengine.bobo.query.scoring.FacetTermScoringFunctionFactory;
import com.browseengine.bobo.sort.DocComparatorSource;

public class SimpleFacetHandler extends FacetHandler<FacetDataCache> implements FacetScoreable
{
	private static Logger logger = Logger.getLogger(SimpleFacetHandler.class);
	
	private final TermListFactory _termListFactory;
	private final String _indexFieldName;
	
	public SimpleFacetHandler(String name,String indexFieldName,TermListFactory termListFactory,Set<String> dependsOn)
	{
	   super(name,dependsOn);
	   _indexFieldName=indexFieldName;
	   _termListFactory=termListFactory;
	}
	
	public SimpleFacetHandler(String name,TermListFactory termListFactory,Set<String> dependsOn)
	{
	   this(name,name,termListFactory,dependsOn);
	}
	
	public SimpleFacetHandler(String name,String indexFieldName,TermListFactory termListFactory)
	{
	   this(name,indexFieldName,termListFactory,null);
	}
	
	public SimpleFacetHandler(String name,TermListFactory termListFactory)
    {
        this(name,name,termListFactory);
    }
	
	public SimpleFacetHandler(String name)
    {
        this(name,name,null);
    }
	
	public SimpleFacetHandler(String name,String indexFieldName)
	{
		this(name,indexFieldName,null);
	}
	
	@Override
	public DocComparatorSource getDocComparatorSource() {
		return new FacetDataCache.FacetDocComparatorSource(this);
	}

	@Override
	public String[] getFieldValues(BoboIndexReader reader,int id) {
		FacetDataCache dataCache = getFacetData(reader);
		return new String[]{dataCache.valArray.get(dataCache.orderArray.get(id))};
	}

	@Override
	public Object[] getRawFieldValues(BoboIndexReader reader,int id){
		FacetDataCache dataCache = getFacetData(reader);
		return new Object[]{dataCache.valArray.getRawValue(dataCache.orderArray.get(id))};
	}
	
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop) throws IOException
  {
    FacetFilter f = new FacetFilter(this, value);
    AdaptiveFacetFilter af = new AdaptiveFacetFilter(new FacetDataCacheBuilder(){

		@Override
		public FacetDataCache build(BoboIndexReader reader) {
			return  getFacetData(reader);
		}

		@Override
		public String getName() {
			return _name;
		}
    	
    }, f, new String[]{value});
    return af;
  }

  @Override
  public RandomAccessFilter buildRandomAccessAndFilter(String[] vals,Properties prop) throws IOException
  {
    if (vals.length > 1)
    {
      return EmptyFilter.getInstance();
    }
    else
    {
      return buildRandomAccessFilter(vals[0],prop);
    }
  }

  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,Properties prop,boolean isNot) throws IOException
  {
    RandomAccessFilter filter = null;
    
    if(vals.length > 1)
    {
      FacetOrFilter f = new FacetOrFilter(this,vals,isNot);
      AdaptiveFacetFilter af = new AdaptiveFacetFilter(new FacetDataCacheBuilder(){

  		@Override
  		public FacetDataCache build(BoboIndexReader reader) {
  			return  getFacetData(reader);
  		}

  		@Override
  		public String getName() {
  			return _name;
  		}
      	
      }, f, vals);
      return af;
    }
    else if(vals.length == 1)
    {
      filter = buildRandomAccessFilter(vals[0], prop);
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

  @Override
	public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel,final FacetSpec ospec) {
	  return new FacetCountCollectorSource(){

		@Override
		public FacetCountCollector getFacetCountCollector(
				BoboIndexReader reader, int docBase) {
			FacetDataCache dataCache = SimpleFacetHandler.this.getFacetData(reader);
			SimpleFacetCountCollector collector = new SimpleFacetCountCollector(_name,dataCache,docBase,sel,ospec);
			collector.setFacetScoringParams(SimpleFacetHandler.this.getFacetScoringParamMap());
			return collector;
		}  
	  };
	}

	@Override
	public FacetDataCache load(BoboIndexReader reader) throws IOException {
		FacetDataCache dataCache = new FacetDataCache();
		dataCache.load(_indexFieldName, reader, _termListFactory);
		return dataCache;
	}
	
	public BoboDocScorer getDocScorer(BoboIndexReader reader,FacetTermScoringFunctionFactory scoringFunctionFactory,Map<String,Float> boostMap){
		FacetDataCache dataCache = getFacetData(reader);
		float[] boostList = BoboDocScorer.buildBoostList(dataCache.valArray, boostMap);
		return new SimpleBoboDocScorer(dataCache,scoringFunctionFactory,boostList);
	}
	
	public static final class SimpleFacetCountCollector extends DefaultFacetCountCollector
	{
		public SimpleFacetCountCollector(String name,FacetDataCache dataCache,int docBase,BrowseSelection sel,FacetSpec ospec)
		{
		    super(name,dataCache,docBase,sel,ospec);
		}
		
		public final void collect(int docid) {
			_count[_array.get(docid)]++;
		}
		
		public final void collectAll() {
		  _count = _dataCache.freqs;
		}
	}
	
	public static final class SimpleBoboDocScorer extends BoboDocScorer{
		private final FacetDataCache _dataCache;
		
		public SimpleBoboDocScorer(FacetDataCache dataCache,FacetTermScoringFunctionFactory scoreFunctionFactory,float[] boostList){
			super(scoreFunctionFactory.getFacetTermScoringFunction(dataCache.valArray.size(), dataCache.orderArray.size()),boostList);
			_dataCache = dataCache;
		}
		
		@Override
		public Explanation explain(int doc){
			int idx = _dataCache.orderArray.get(doc);
			return _function.explain(_dataCache.freqs[idx],_boostList[idx]);
		}

		@Override
		public final float score(int docid) {
			int idx = _dataCache.orderArray.get(docid);
			return _function.score(_dataCache.freqs[idx],_boostList[idx]);
		}
	}
}
