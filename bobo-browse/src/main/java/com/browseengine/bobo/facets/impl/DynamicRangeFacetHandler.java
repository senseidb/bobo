/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.RuntimeFacetHandler;
import com.browseengine.bobo.facets.FacetHandler.FacetDataNone;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.sort.DocComparatorSource;
import com.browseengine.bobo.util.ListMerger;

/**
 * @author ymatsuda
 *
 */
public abstract class DynamicRangeFacetHandler extends RuntimeFacetHandler<FacetDataNone>
{
  protected final String _dataFacetName;
  protected RangeFacetHandler _dataFacetHandler;
  
  public DynamicRangeFacetHandler(String name, String dataFacetName)
  {
    super(name,new HashSet<String>(Arrays.asList(new String[]{dataFacetName})));
    _dataFacetName = dataFacetName;
  }
  
  protected abstract String buildRangeString(String val);
  protected abstract List<String> buildAllRangeStrings();
  protected abstract String getValueFromRangeString(String rangeString);
  
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String val, Properties props) throws IOException
  {
    return _dataFacetHandler.buildRandomAccessFilter(buildRangeString(val), props);
  }

  @Override
  public RandomAccessFilter buildRandomAccessAndFilter(String[] vals, Properties prop) throws IOException
  {
    List<String> valList = new ArrayList<String>(vals.length);
    for(String val : vals)
    {
      valList.add(buildRangeString(val));
    }
    
    return _dataFacetHandler.buildRandomAccessAndFilter(valList.toArray(new String[valList.size()]), prop);
  }

  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,Properties prop,boolean isNot) throws IOException
  {
    List<String> valList = new ArrayList<String>(vals.length);
    for(String val : vals)
    {
      valList.add(buildRangeString(val));
    }
    return _dataFacetHandler.buildRandomAccessOrFilter(valList.toArray(new String[valList.size()]), prop, isNot);
  }

  @Override
  public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel, final FacetSpec fspec)
  {
    final List<String> list = buildAllRangeStrings();
    
    return new FacetCountCollectorSource(){
		@Override
		public FacetCountCollector getFacetCountCollector(BoboIndexReader reader, int docBase) {
		    FacetDataCache dataCache = _dataFacetHandler.getFacetData(reader);
		    return new DynamicRangeFacetCountCollector(getName(), dataCache, docBase, fspec, list);
		}
    };
  }

  @Override
  public String[] getFieldValues(BoboIndexReader reader,int docid)
  {
    return _dataFacetHandler.getFieldValues(reader,docid);
  }
  
  @Override
  public Object[] getRawFieldValues(BoboIndexReader reader,int docid)
  {
    return _dataFacetHandler.getRawFieldValues(reader,docid);
  }

  @Override
  public DocComparatorSource getDocComparatorSource()
  {
    return _dataFacetHandler.getDocComparatorSource();
  }

  @Override
  public FacetDataNone load(BoboIndexReader reader) throws IOException
  {
    _dataFacetHandler = (RangeFacetHandler)getDependedFacetHandler(_dataFacetName);
    return FacetDataNone.instance;
  }
  
  private class DynamicRangeFacetCountCollector extends RangeFacetCountCollector
  {
    DynamicRangeFacetCountCollector(String name, FacetDataCache dataCache,int docBase, FacetSpec fspec, List<String> predefinedList)
    {
      super(name,dataCache,docBase,fspec,predefinedList);
    }

    @Override
    public BrowseFacet getFacet(String value)
    {
      String rangeString = buildRangeString(value);
      BrowseFacet facet = super.getFacet(rangeString);
      if (facet!=null)
      {
        return new BrowseFacet(value,facet.getHitCount());
      }
      else
      {
        return null;
      }
    }

    @Override
    public int getFacetHitsCount(Object value) 
    {
      String rangeString = buildRangeString((String)value);
      return super.getFacetHitsCount(rangeString);
    }

    @Override
    public List<BrowseFacet> getFacets()
    {
      List<BrowseFacet> list = super.getFacets();      
      ArrayList<BrowseFacet> retList = new ArrayList<BrowseFacet>(list.size());
      Iterator<BrowseFacet> iter = list.iterator();
      while(iter.hasNext())
      {
        BrowseFacet facet = iter.next();
        String val = facet.getValue();
        String rangeString = getValueFromRangeString(val);
        if (rangeString != null)
        {
          BrowseFacet convertedFacet = new BrowseFacet(rangeString, facet.getHitCount());
          retList.add(convertedFacet);
        }
      }
      return retList;
    }

    public FacetIterator iterator()
    {
      FacetIterator iter = super.iterator();

      List<BrowseFacet> facets = new ArrayList<BrowseFacet>();
      while(iter.hasNext())
      {
        Comparable facet = iter.next();
        int count = iter.count;
        facets.add(new BrowseFacet(getValueFromRangeString(String.valueOf(facet)), count));
      }
      Collections.sort(facets, ListMerger.FACET_VAL_COMPARATOR);
      return new PathFacetIterator(facets);
    }
  }
}
