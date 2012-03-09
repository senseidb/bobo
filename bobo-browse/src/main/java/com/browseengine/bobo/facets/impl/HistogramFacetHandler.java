package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.api.IntFacetIterator;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandler.FacetDataNone;
import com.browseengine.bobo.facets.RuntimeFacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermIntList;
import com.browseengine.bobo.facets.data.TermLongList;
import com.browseengine.bobo.facets.data.TermValueList;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.sort.DocComparatorSource;

public class HistogramFacetHandler<T extends Number> extends RuntimeFacetHandler<FacetDataNone>
{
  private final String _dataHandlerName;
  private final T _start;
  private final T _end;
  private final T _unit;
  
  private FacetHandler<?> _dataFacetHandler;
  
  public HistogramFacetHandler(String name, String dataHandlerName, T start, T end, T unit)
  {
    super(name, new HashSet<String>(Arrays.asList(new String[]{ dataHandlerName })));
    _dataHandlerName = dataHandlerName;
    _start = start;
    _end = end;
    _unit = unit;
  }
  
  @Override
  public FacetDataNone load(BoboIndexReader reader) throws IOException
  {
    _dataFacetHandler = reader.getFacetHandler(_dataHandlerName);
    if(_dataFacetHandler instanceof RangeFacetHandler)
    {
      if(((RangeFacetHandler)_dataFacetHandler).hasPredefinedRanges())
      {
        throw new UnsupportedOperationException("underlying range facet handler should not have the predefined ranges");
      }
    }
	return FacetDataNone.instance;
  }	
  
  @Override
  public DocComparatorSource getDocComparatorSource()
  {
    throw new UnsupportedOperationException();
  }
	
  @Override
  public String[] getFieldValues(BoboIndexReader reader, int id)
  {
    return null;
  }
	
  @Override
  public Object[] getRawFieldValues(BoboIndexReader reader,int id)
  {
    return null;
  }
  
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop) throws IOException
  {
    return _dataFacetHandler.buildRandomAccessFilter(value, prop);
  }
  
  @Override
  public RandomAccessFilter buildRandomAccessAndFilter(String[] vals, Properties prop) throws IOException
  {
    return _dataFacetHandler.buildRandomAccessAndFilter(vals, prop);
  }
  
  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] vals, Properties prop, boolean isNot) throws IOException
  {
    return _dataFacetHandler.buildRandomAccessOrFilter(vals, prop, isNot);
  }
  
  @Override
  public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel,final FacetSpec ospec)
  {
    final FacetCountCollectorSource baseCollectorSrc = _dataFacetHandler.getFacetCountCollectorSource(sel, ospec);
    
    return new FacetCountCollectorSource()
    {
      @Override
      public FacetCountCollector getFacetCountCollector(BoboIndexReader reader, int docBase)
      {
        FacetDataCache<?> dataCache = (FacetDataCache<?>)reader.getFacetData(_dataHandlerName);
        FacetCountCollector baseCollector = baseCollectorSrc.getFacetCountCollector(reader, docBase);
        return new HistogramCollector<T>(getName(),baseCollector, dataCache, ospec, _start, _end, _unit);
      }
	};
  }

  public static class HistogramCollector<T extends Number> implements FacetCountCollector
  {
    private final DecimalFormat _formatter = new DecimalFormat("0000000000");
    private final FacetSpec _ospec;
    private final T _start;
    private final T _end;
    private final T _unit;
    private final int[] _count;
    private final TermValueList<?> _valArray;
    private final FacetCountCollector _baseCollector;
    private final String _facetName;
    
    private boolean _isAggregated;
    
    protected HistogramCollector(String facetName,FacetCountCollector baseCollector, FacetDataCache<?> dataCache, FacetSpec ospec, T start, T end, T unit)
    {
      _facetName = facetName;
      _baseCollector = baseCollector;
  	  _valArray = dataCache.valArray;
      _ospec = ospec;
      _isAggregated = false;
      _start = start;
      _end = end;
      _unit = unit;
      _count = new int[countArraySize()];
    }
    
    private int countArraySize()
    {
      if(_start instanceof Long)
      {
        long range = _end.longValue() - _start.longValue();
        return (int)(range / _unit.longValue()) + 1;
      }
      else if(_start instanceof Integer)
      {
        int range = _end.intValue() - _start.intValue();
        return (range / _unit.intValue()) + 1;      
      }
      else
      {
        double range = _end.doubleValue() - _start.doubleValue();
        return (int)(range / _unit.doubleValue()) + 1;
      }
    }
    
    /**
     * not supported
     */
    public int[] getCountDistribution()
    {
      if(!_isAggregated) aggregate();
      return _count;
    }
    
    public BrowseFacet getFacet(String value)
    {
      if(!_isAggregated) aggregate();
      
      int idx = Integer.parseInt(value);
      if(idx >= 0 && idx < _count.length)
      {
        return new BrowseFacet(value, _count[idx]);
      }
      return null; 
    }

    public int getFacetHitsCount(Object value) 
    {
      if(!_isAggregated) aggregate();
      
      int idx;
      if (value instanceof String)
        idx = Integer.parseInt((String)value);
      else
        idx = ((Number)value).intValue();
      if(idx >= 0 && idx < _count.length)
      {
        return _count[idx];
      }
      return 0; 
    }
    
    public final void collect(int docid)
    {
      _baseCollector.collect(docid);
    }
    
    public final void collectAll()
    {
      _baseCollector.collectAll();
    }
    
    private void aggregate()
    {
      if(_isAggregated) return;
      
      _isAggregated = true;
      
      int startIdx = _valArray.indexOf(_start);
      if (startIdx < 0) startIdx = -(startIdx + 1);
      
      int endIdx = _valArray.indexOf(_end);
      if (endIdx < 0) endIdx = -(endIdx + 1);
      
      int[] baseCounts = _baseCollector.getCountDistribution();
      if(_start instanceof Long)
      {
        long start = _start.longValue();
        long unit = _unit.longValue();
        TermLongList valArray = (TermLongList)_valArray;
        for(int i = startIdx; i < endIdx; i++)
        {
          long val = valArray.getPrimitiveValue(i);
          int idx = (int)((val - start) / unit);
          if(idx >= 0 && idx < _count.length)
          {
            _count[idx] += baseCounts[i];
          }
        }
      }
      else if(_start instanceof Integer)
      {
        int start = _start.intValue();
        int unit = _unit.intValue();
        TermIntList valArray = (TermIntList)_valArray;
        for(int i = startIdx; i < endIdx; i++)
        {
          int val = valArray.getPrimitiveValue(i);
          int idx = ((val - start) / unit);
          if(idx >= 0 && idx < _count.length)
          {
            _count[idx] += baseCounts[i];
          }
        }
      }
      else
      {
        double start = _start.doubleValue();
        double unit = _unit.doubleValue();
        for(int i = startIdx; i < endIdx; i++)
        {
          Number val = (Number)_valArray.getRawValue(i);
          int idx = (int)((val.doubleValue() - start) / unit);
          if(idx >= 0 && idx < _count.length)
          {
            _count[idx] += baseCounts[i];
          }
        }
      }
    }
    
    public List<BrowseFacet> getFacets()
    {
  	  if (_ospec != null)
  	  {
        int minCount = _ospec.getMinHitCount();
        int max = _ospec.getMaxCount();
        if (max <= 0) max = _count.length;
        
        List<BrowseFacet> facetColl;
        FacetSortSpec sortspec = _ospec.getOrderBy();
        if (sortspec == FacetSortSpec.OrderValueAsc)
        {
          facetColl = new ArrayList<BrowseFacet>(max);
          for (int i = 0; i < _count.length; ++i)
          {
        	int hits = _count[i];
  	        if (hits >= minCount)
  	        {
  	          BrowseFacet facet = new BrowseFacet(_formatter.format(i),hits);
  	          facetColl.add(facet);
  	        }
  	        if (facetColl.size() >= max) break;
          }
          return facetColl;
        }
        else
        {
          return FacetCountCollector.EMPTY_FACET_LIST;
  		}
  	  }
  	  else
  	  {
  		return FacetCountCollector.EMPTY_FACET_LIST;
  	  }
    }
    
    public FacetIterator iterator()
    {
      if(!_isAggregated) aggregate();
      return new HistogramFacetIterator(_count, _formatter);
    }
    
    public String getName()
    {
      return _facetName;
	}
    
    public void close()
    {
    }
  }
  
  public static class HistogramFacetIterator extends IntFacetIterator
  {
    private final DecimalFormat _formatter;
    private final int[] _count;
    private final int _maxMinusOne;
    private int _idx;

    public HistogramFacetIterator(int count[], DecimalFormat formatter)
    {
      _idx = -1;
      _count = count;
      _maxMinusOne = count.length - 1;
      _formatter = formatter;
    }

    public Integer next()
    {
      if(hasNext())
      {
        count = _count[++_idx];
        return (facet = _idx);
      }
      return null;
    }
    
    public Integer next(int minHits)
    {
      while(_idx < _maxMinusOne)
      {
        if(_count[++_idx] >= minHits)
        {
          count = _count[_idx];          
          return (facet = _idx);
        }
      }
      return null;    
    }
    
    public int nextInt()
    {
      if(hasNext())
      {
        count = _count[++_idx];
        return (facet = _idx);
      }
      return TermIntList.VALUE_MISSING;
    }
    
    public int nextInt(int minHits)
    {
      while(_idx < _maxMinusOne)
      {
        if(_count[++_idx] >= minHits)
        {
          count = _count[_idx];
          return (facet = _idx);
        }
      }
      return TermIntList.VALUE_MISSING;    
    }

    public boolean hasNext()
    {
      return (_idx < _maxMinusOne);
    }
    
    public void remove()
    {
      throw new UnsupportedOperationException();
	}

    @Override
    public String format(Object val)
    {
      return _formatter.format(val);
    }
    
    @Override
    public String format(int val)
    {
      return _formatter.format(val);
    }
  }
}
