package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.FacetOrFilter;
import com.browseengine.bobo.facets.filter.FacetRangeFilter;
import com.browseengine.bobo.facets.filter.FacetRangeOrFilter;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.facets.filter.FacetRangeFilter.FacetRangeValueConverter;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler.SimpleBoboDocScorer;
import com.browseengine.bobo.query.scoring.BoboDocScorer;
import com.browseengine.bobo.query.scoring.FacetScoreable;
import com.browseengine.bobo.query.scoring.FacetTermScoringFunctionFactory;
import com.browseengine.bobo.sort.DocComparatorSource;

public class RangeFacetHandler extends FacetHandler<FacetDataCache> implements FacetScoreable{
	private static Logger logger = Logger.getLogger(RangeFacetHandler.class);
	private final String _indexFieldName;
	private final TermListFactory _termListFactory;
	private final List<String> _predefinedRanges;
	
	public RangeFacetHandler(String name,String indexFieldName,TermListFactory termListFactory,List<String> predefinedRanges)
	{
		super(name);
		_indexFieldName = indexFieldName;
		_termListFactory = termListFactory;
		_predefinedRanges = predefinedRanges;
	}
	
	public RangeFacetHandler(String name,TermListFactory termListFactory,List<String> predefinedRanges)
    {
	   this(name,name,termListFactory,predefinedRanges);
    }
	
	public RangeFacetHandler(String name,List<String> predefinedRanges)
    {
        this(name,name,null,predefinedRanges);
    }
	
	public RangeFacetHandler(String name,String indexFieldName,List<String> predefinedRanges)
    {
        this(name,indexFieldName,null,predefinedRanges);
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

	public static String[] getRangeStrings(String rangeString)
	{
	  int index=rangeString.indexOf('[');
      int index2=rangeString.indexOf(" TO ");
      int index3=rangeString.indexOf(']');
      
      String lower,upper;
      try{
        lower=rangeString.substring(index+1,index2).trim();
        upper=rangeString.substring(index2+4,index3).trim();
      
        return new String[]{lower,upper};
      }
      catch(RuntimeException re){
        logger.error("problem parsing range string: "+rangeString+":"+re.getMessage(),re);
        throw re;
      }
	}
	
	
	
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop) throws IOException
  {
      return new FacetRangeFilter(this,value);
  }
  
	
  @Override
  public RandomAccessFilter buildRandomAccessAndFilter(String[] vals,Properties prop) throws IOException
  {
    ArrayList<RandomAccessFilter> filterList = new ArrayList<RandomAccessFilter>(vals.length);
    
    for (String val : vals){
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
    if (vals.length > 1)
    {
      return new FacetRangeOrFilter(this,vals,isNot,FacetRangeValueConverter.instance);
    }
    else
    {
      RandomAccessFilter filter = buildRandomAccessFilter(vals[0],prop);
      if (filter == null) return filter;
      if (isNot)
      {
        filter = new RandomAccessNotFilter(filter);
      }
      return filter;
    }
  }

  @Override
  public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel,final FacetSpec ospec) {
	  return new FacetCountCollectorSource() {
		
		@Override
		public FacetCountCollector getFacetCountCollector(BoboIndexReader reader,
				int docBase) {
			FacetDataCache dataCache = getFacetData(reader);
			return new RangeFacetCountCollector(_name,dataCache,docBase,ospec,_predefinedRanges);
		}
	};
    
  }
  
  public boolean hasPredefinedRanges()
  {
    return (_predefinedRanges != null);
  }

	@Override
	public FacetDataCache load(BoboIndexReader reader) throws IOException {
	    FacetDataCache dataCache = new FacetDataCache();
		dataCache.load(_indexFieldName, reader, _termListFactory);
		return dataCache;
	}

	@Override
	public BoboDocScorer getDocScorer(BoboIndexReader reader,
			FacetTermScoringFunctionFactory scoringFunctionFactory,
			Map<String, Float> boostMap) {
		FacetDataCache dataCache = getFacetData(reader);
		float[] boostList = BoboDocScorer.buildBoostList(dataCache.valArray, boostMap);
		return new RangeBoboDocScorer(dataCache,scoringFunctionFactory,boostList);
	}	
	
	public static final class RangeBoboDocScorer extends BoboDocScorer{
		private final FacetDataCache _dataCache;
		
		public RangeBoboDocScorer(FacetDataCache dataCache,FacetTermScoringFunctionFactory scoreFunctionFactory,float[] boostList){
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
