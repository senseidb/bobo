package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandlerFactory;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.FacetOrFilter;
import com.browseengine.bobo.facets.filter.FacetRangeFilter;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.facets.filter.FacetRangeFilter.FacetRangeValueConverter;
import com.browseengine.bobo.sort.DocComparatorSource;

public class RangeFacetHandler extends FacetHandler<FacetDataCache> implements FacetHandlerFactory<RangeFacetHandler>{
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
	
	
	public RangeFacetHandler newInstance()
    {
	  return new RangeFacetHandler(getName(),_indexFieldName,_termListFactory,_predefinedRanges);
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
      return new FacetOrFilter(this,vals,isNot,FacetRangeValueConverter.instance);
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

	@Override
	public FacetDataCache load(BoboIndexReader reader) throws IOException {
	    FacetDataCache dataCache = new FacetDataCache();
		dataCache.load(_indexFieldName, reader, _termListFactory);
		return dataCache;
	}	
}
