package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.facets.filter.RandomAccessOrFilter;
import com.browseengine.bobo.sort.DocComparatorSource;

public class BucketFacetHandler extends FacetHandler<FacetDataCache>{
  private static Logger logger = Logger.getLogger(BucketFacetHandler.class);
  private final List<String> _predefinedBuckets;
  private final String _dependOnFacetName;
  private FacetHandler<?> _dependOnFacetHandler;
  String _name;
  
  public BucketFacetHandler(String name, List<String> predefinedBuckets, String dependsOnFacetName)
  {
    super(name, null);
    _name = name;
    _predefinedBuckets = predefinedBuckets;
    _dependOnFacetName  = dependsOnFacetName;
    _dependOnFacetHandler = null;
  }
  
  private static String DEFAULT_SEP = ",";
  

  public static String[] convertBucketStringsToNonOverlapElementStrings(String[] bucketStrings)
  {
    if(bucketStrings.length == 1)
    {
      return bucketStrings[0].split(DEFAULT_SEP);
    }
    SortedSet<String> elementSet =  new TreeSet<String>();
    for (String bucketString : bucketStrings)
    {
      String[] elementStrings =  bucketString.split(DEFAULT_SEP);
      for(String elementString : elementStrings)
      {
        elementSet.add(elementString);
      }  
    }
    
    List<String> elemList = new ArrayList<String>(elementSet);
    String[] elems = elemList.toArray(new String[elemList.size()]);
    
    return elems;
  }
  
  public static String[] convertAndBucketStringsToNonOverlapElementStrings(String[] bucketStrings)
  {
    if(bucketStrings.length == 1)
    {
      return bucketStrings[0].split(DEFAULT_SEP);
    }
    
    Map<String, Integer> elemCount =  new HashMap<String, Integer>();
    for (String bucketString : bucketStrings)
    {
      String[] elems =  bucketString.split(DEFAULT_SEP);
      for(String elem : elems)
      {
        if(elemCount.containsKey(elem))
        {
          elemCount.put(elem, elemCount.get(elem)+1);
        }
        else
        {
          elemCount.put(elem,1);
        }
      }  
    }
    
    List<String> elemList = new ArrayList<String>();
    int size = bucketStrings.length;
    for(String elem : elemCount.keySet())
    {
      if(elemCount.get(elem) == size)
      {
        elemList.add(elem);
      }
    }
   
    String[] elems = elemList.toArray(new String[elemList.size()]);
    
    return elems;
  }

  
  @Override
  public DocComparatorSource getDocComparatorSource() {
    return _dependOnFacetHandler.getDocComparatorSource();
  }
  
  @Override
  public String[] getFieldValues(BoboIndexReader reader, int id) {
    return _dependOnFacetHandler.getFieldValues(reader, id);
  }
  
  @Override
  public Object[] getRawFieldValues(BoboIndexReader reader,int id){
    return _dependOnFacetHandler.getRawFieldValues(reader, id);
  }

  @Override
  public RandomAccessFilter buildRandomAccessFilter(String bucketString, Properties prop) throws IOException
  {
      String[] elems = convertBucketStringsToNonOverlapElementStrings(new String[]{bucketString});
  
      if(elems == null || elems.length==0) return  EmptyFilter.getInstance();
      if(elems.length == 1) return _dependOnFacetHandler.buildRandomAccessFilter(elems[0], prop);
      return _dependOnFacetHandler.buildRandomAccessOrFilter(elems, prop, false);
  }
  
  
  @Override
  public RandomAccessFilter buildRandomAccessAndFilter(String[] bucketStrings,Properties prop) throws IOException
  {
    // The AND operation is hidden here. Convert the AND of buckets to the OR of the intersected underlying elements
    String[] elems = convertAndBucketStringsToNonOverlapElementStrings(bucketStrings);
    
    if(elems == null || elems.length==0) return  EmptyFilter.getInstance();
    if(elems.length == 1) return _dependOnFacetHandler.buildRandomAccessFilter(elems[0], prop);
    return _dependOnFacetHandler.buildRandomAccessOrFilter(elems, prop, false);
  }

  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,Properties prop,boolean isNot) throws IOException
  {
    String[] elems = convertBucketStringsToNonOverlapElementStrings(vals);
    
    if (vals.length > 1 || !isNot)
    {
      if(elems == null || elems.length == 0) return  EmptyFilter.getInstance();
      if(elems.length == 1) return _dependOnFacetHandler.buildRandomAccessFilter(elems[0], prop);
      return _dependOnFacetHandler.buildRandomAccessOrFilter(elems, prop, isNot);
    }
    else // vals.length == 1 and isNot == true
    {
      ArrayList<RandomAccessFilter> filterList = new ArrayList<RandomAccessFilter>(elems.length);
      
      for (String elem : elems)
      {
        RandomAccessFilter f = _dependOnFacetHandler.buildRandomAccessFilter(elem, prop);
        if(f != null) 
        {
          filterList.add(f); 
        }
      }
      
      RandomAccessFilter filter;
      if(filterList.size() == 0)
      {
        filter = EmptyFilter.getInstance();
      }
      else if(filterList.size() == 1)
      {
        filter = new RandomAccessNotFilter(filterList.get(0));
      }
      else
      {
        filter =  new RandomAccessNotFilter(new RandomAccessOrFilter(filterList));
      }
      
      return filter;
    }
  }

  @Override
  public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel, final FacetSpec ospec) 
  {
    return new FacetCountCollectorSource() 
    {
      @Override
      public FacetCountCollector getFacetCountCollector(BoboIndexReader reader, int docBase)
      {
        FacetDataCache dataCache = getFacetData(reader);
        
        FacetSpec elemSpec = new FacetSpec();
        elemSpec.setCustomComparatorFactory(ospec.getCustomComparatorFactory());
        elemSpec.setExpandSelection(ospec.isExpandSelection());
        elemSpec.setOrderBy(ospec.getOrderBy());
        elemSpec.setMaxCount(Integer.MAX_VALUE);
        elemSpec.setMinHitCount(1);
        
        return new BucketFacetCountCollector(_name, _dependOnFacetHandler.getFacetCountCollectorSource(sel, elemSpec).getFacetCountCollector(reader, docBase), ospec, _predefinedBuckets);
      }
    };

  }
  
  public boolean hasPredefinedBuckets()
  {
    return (_predefinedBuckets != null);
  }
  
  @Override
  public FacetDataCache load(BoboIndexReader reader) throws IOException
  {
    _dependOnFacetHandler = reader.getFacetHandler(_dependOnFacetName);
    if (_dependOnFacetHandler==null)
    {
      throw new IllegalStateException("bucketFacetHandler need to be supported by other underlying facetHandlers");
    }
    return (FacetDataCache)_dependOnFacetHandler.load(reader);
  } 
}

