package com.browseengine.bobo.facets.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.kamikaze.docidset.impl.OrDocIdSet;

public class RandomAccessOrFilter extends RandomAccessFilter
{
  private static final long serialVersionUID = 1L;
 
  protected final List<RandomAccessFilter> _filters;
  
  public RandomAccessOrFilter(List<RandomAccessFilter> filters)
  {
    if (filters==null)
    {
      Exception e =new Exception();
      e.fillInStackTrace();
      e.printStackTrace();
    }
    _filters = filters;
  }
  
  public double getFacetSelectivity(BoboIndexReader reader)
  {
    double selectivity = 0;
    for(RandomAccessFilter filter : _filters)
    {
      selectivity += filter.getFacetSelectivity(reader);
    }
    
    if(selectivity > 0.999)
    {
      selectivity = 1.0;
    }
    return selectivity;
  }
  
  @Override
  public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboIndexReader reader) throws IOException
  {
    if(_filters.size() == 1)
    {
      return _filters.get(0).getRandomAccessDocIdSet(reader);
    }
    else
    {
      List<DocIdSet> list = new ArrayList<DocIdSet>(_filters.size());
      List<RandomAccessDocIdSet> randomAccessList = new ArrayList<RandomAccessDocIdSet>(_filters.size());
      for (RandomAccessFilter f : _filters)
      {
        RandomAccessDocIdSet s = f.getRandomAccessDocIdSet(reader);
        list.add(s);
        randomAccessList.add(s);
      }
      final RandomAccessDocIdSet[] randomAccessDocIdSets = randomAccessList.toArray(new RandomAccessDocIdSet[randomAccessList.size()]);
      final DocIdSet orDocIdSet = new OrDocIdSet(list);
      return new RandomAccessDocIdSet()
      {
        @Override
        public boolean get(int docId)
        {
          for(RandomAccessDocIdSet s : randomAccessDocIdSets)
          {
            if(s.get(docId)) return true;
          }
          return false;
        }

        @Override
        public DocIdSetIterator iterator() throws IOException
        {
          return orDocIdSet.iterator();
        }
      };
    }
  }

}
