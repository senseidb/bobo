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
import com.browseengine.bobo.facets.data.MultiValueWithWeightFacetDataCache;
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

public class MultiValueWithWeightFacetHandler extends MultiValueFacetHandler
{
  public MultiValueWithWeightFacetHandler(String name, String indexFieldName, TermListFactory termListFactory) 
  {
    super(name, indexFieldName, termListFactory, null, null);
  }

  public MultiValueWithWeightFacetHandler(String name, String indexFieldName) 
  {
    super(name, indexFieldName, null, null, null);
  }

  public MultiValueWithWeightFacetHandler(String name) 
  {
    super(name, name, null, null, null);
  }

  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop) throws IOException
  {
    MultiValueFacetFilter f= new MultiValueFacetFilter(new MultiDataCacheBuilder(getName()), value);
    return f;
  }
  @Override
  public MultiValueWithWeightFacetDataCache load(BoboIndexReader reader, WorkArea workArea) throws IOException
  {
    MultiValueWithWeightFacetDataCache dataCache = new MultiValueWithWeightFacetDataCache();
      
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
}
