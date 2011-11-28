package com.browseengine.bobo.facets.impl;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.FacetOrFilter;
import com.browseengine.bobo.facets.filter.FacetValueConverter;
import com.browseengine.bobo.facets.filter.MultiValueORFacetFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.sort.DocComparatorSource;

public class PathFacetHandler extends FacetHandler<FacetDataCache> 
{
	private static final String DEFAULT_SEP = "/";
	
	public static final String SEL_PROP_NAME_STRICT="strict";
    public static final String SEL_PROP_NAME_DEPTH="depth";
    
    private final boolean _multiValue;
	
	private final TermListFactory _termListFactory;
	private String _separator;
	private final String _indexedName;
	
	public PathFacetHandler(String name){
		this(name,false);
	}
	
	public PathFacetHandler(String name,boolean multiValue){
		this(name,name,multiValue);
	}
	
	public PathFacetHandler(String name,String indexedName,boolean multiValue)
	{
		super(name);
		_indexedName = indexedName;
		_multiValue = multiValue;
		_termListFactory=TermListFactory.StringListFactory;
		_separator=DEFAULT_SEP;
	}
	
	/**
     * Sets is strict applied for counting. Used if the field is of type <b><i>path</i></b>.
     * @param strict is strict applied
     */
    public static void setStrict(Properties props,boolean strict) {
      props.setProperty(PathFacetHandler.SEL_PROP_NAME_STRICT, String.valueOf(strict));
    }
    

    /**
     * Sets the depth.  Used if the field is of type <b><i>path</i></b>.
     * @param depth depth
     */
    public static void setDepth(Properties props,int depth) {
      props.setProperty(PathFacetHandler.SEL_PROP_NAME_DEPTH, String.valueOf(depth));
    }
    

	/**
     * Gets if strict applied for counting. Used if the field is of type <b><i>path</i></b>.
     * @return is strict applied
     */
    public static boolean isStrict(Properties selectionProp) {
    	try
    	{
          return Boolean.valueOf(selectionProp.getProperty(PathFacetHandler.SEL_PROP_NAME_STRICT));
    	}
    	catch(Exception e)
    	{
    		return false;
    	}
    }
    

	@Override
	public int getNumItems(BoboIndexReader reader, int id) {
		FacetDataCache data = getFacetData(reader);
		if (data==null) return 0;
		return data.getNumItems(id);
	}
	
    
    /**
     * Gets the depth.  Used if the field is of type <b><i>path</i></b>.
     * @return depth
     */
    public static int getDepth(Properties selectionProp) {
      try
      {
        return Integer.parseInt(selectionProp.getProperty(PathFacetHandler.SEL_PROP_NAME_DEPTH));
      }
      catch(Exception e)
      {
        return 1;
      }
    }
    
	@Override
	public DocComparatorSource getDocComparatorSource()  
	{
		return new FacetDataCache.FacetDocComparatorSource(this);
	}
	
	@Override
	public String[] getFieldValues(BoboIndexReader reader,int id) 
	{
		FacetDataCache dataCache = getFacetData(reader);
		if (dataCache==null) return new String[0];
		if (_multiValue){
		  return ((MultiValueFacetDataCache)dataCache)._nestedArray.getTranslatedData(id, dataCache.valArray);	
		}
		else{
		  	
		  return new String[]{dataCache.valArray.get(dataCache.orderArray.get(id))};
		}

	}
	 
	@Override
	public Object[] getRawFieldValues(BoboIndexReader reader,int id){
		return getFieldValues(reader,id);
	}

	
	public void setSeparator(String separator)
	{
		_separator = separator;
	}
	
	public String getSeparator()
	{
		return _separator;
	}
	
	private static int getPathDepth(String path,String separator)
	{
		return path.split(String.valueOf(separator)).length;
	}
	
	
	
	private static class PathValueConverter implements FacetValueConverter{
		private final boolean _strict;
		private final String _sep;
		private final int _depth;
		PathValueConverter(int depth,boolean strict,String sep){
			_strict = strict;
			_sep = sep;
			_depth = depth;
		}
		
		private void getFilters(FacetDataCache dataCache,IntSet intSet,String[] vals, int depth, boolean strict)
	    {
		 for (String val : vals)
		 {
		   getFilters(dataCache,intSet,val,depth,strict);
		 }
	    }
		
		private void getFilters(FacetDataCache dataCache,IntSet intSet,String val, int depth, boolean strict)
		{
		    List<String> termList = dataCache.valArray;
			int index = termList.indexOf(val);

			int startDepth = getPathDepth(val,_sep);
			
			if (index < 0)
			{
				int nextIndex = -(index + 1);
				if (nextIndex == termList.size())
				{
					return;
				}	
				index = nextIndex;
			}
			

			for (int i=index; i<termList.size(); ++i)
			{
				String path = termList.get(i);
				if (path.startsWith(val))
				{
					if (!strict || getPathDepth(path,_sep) - startDepth == depth)
					{
					  intSet.add(i);
					}
				}
				else
				{
					break;
				}	
			}
		}
		
		public int[] convert(FacetDataCache dataCache, String[] vals) {
			IntSet intSet = new IntOpenHashSet();
		    getFilters(dataCache,intSet,vals, _depth, _strict);
		    return intSet.toIntArray();
		}
		
	}
	
	
	
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value,Properties props) throws IOException
  {
	  int depth = getDepth(props);
	  boolean strict = isStrict(props);
	  PathValueConverter valConverter = new PathValueConverter(depth,strict,_separator);
	  String[] vals = new String[]{value};
	  
	  return _multiValue ? new MultiValueORFacetFilter(this,vals,valConverter,false) : new FacetOrFilter(this,vals,false,valConverter);
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
      RandomAccessFilter f = buildRandomAccessFilter(vals[0], prop);
      if (f!=null)
      {
        return f;
      }
      else
      {
        return EmptyFilter.getInstance();
      }
    }
  }

  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,Properties prop,boolean isNot) throws IOException
  {
    if (vals.length > 1)
    {
      
      
      if (vals.length>0)
      {
    	int depth = getDepth(prop);
        boolean strict = isStrict(prop);
    	PathValueConverter valConverter = new PathValueConverter(depth,strict,_separator);
		return _multiValue ? new MultiValueORFacetFilter(this,vals,valConverter,isNot) : new FacetOrFilter(this,vals,isNot,valConverter);
      }
      else
      {
        if (isNot)
        {
          return null;
        }
        else
        {
          return EmptyFilter.getInstance();
        }
      }
    }
    else
    {
      RandomAccessFilter f = buildRandomAccessFilter(vals[0], prop);
      if (f == null) return f;
      if (isNot)
      {
        f = new RandomAccessNotFilter(f);
      }
      return f;
    }
  }
  
	@Override
	public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel,final FacetSpec ospec) 
	{
		return new FacetCountCollectorSource() {
			
			@Override
			public FacetCountCollector getFacetCountCollector(BoboIndexReader reader,
					int docBase) {
				FacetDataCache dataCache = PathFacetHandler.this.getFacetData(reader);
				if (_multiValue){
					return new MultiValuedPathFacetCountCollector(_name, _separator, sel, ospec,dataCache);
				}
				else{
					return new PathFacetCountCollector(_name,_separator,sel,ospec,dataCache);
				}
			}
		};
	}

	@Override
	public FacetDataCache load(BoboIndexReader reader) throws IOException {
       if (!_multiValue){
	      FacetDataCache dataCache = new FacetDataCache();
	      dataCache.load(_indexedName, reader, _termListFactory);
	      return dataCache;
       }
       else{
    	   MultiValueFacetDataCache dataCache = new MultiValueFacetDataCache();
    	   dataCache.load(_indexedName, reader, _termListFactory);
 	      return dataCache;
       }
	}
}
