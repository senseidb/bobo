package com.browseengine.bobo.facets.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;

import com.kamikaze.docidset.impl.AndDocIdSet;

public class AndFilter extends Filter 
{

	private static final long serialVersionUID = 1L;
	
	private final List<? extends Filter> _filters;
	
	public AndFilter(List<? extends Filter> filters)
	{
		_filters = filters;
	}

	@Override
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException
  {
    if (_filters.size() == 1)
    {
      return _filters.get(0).getDocIdSet(reader);
    }
    else
    {
      List<DocIdSet> list = new ArrayList<DocIdSet>(_filters.size());
      for (Filter f : _filters)
      {
        list.add(f.getDocIdSet(reader));
      }
      return new AndDocIdSet(list);
    }
  }
}
