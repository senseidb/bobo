package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;

import com.kamikaze.docidset.impl.NotDocIdSet;

public class NotFilter extends Filter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final Filter _innerFilter;
	
	public NotFilter(Filter innerFilter)
	{
		_innerFilter = innerFilter;
	}
	@Override
	public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
		return new NotDocIdSet(_innerFilter.getDocIdSet(reader),reader.maxDoc());
	}

	
}
