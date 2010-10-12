package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;

public class EmptyFilter extends RandomAccessFilter 
{
	private static final long serialVersionUID = 1L;

	private static EmptyFilter instance = new EmptyFilter();
	
	private EmptyFilter()
	{
		
	}

	 public double getFacetSelectivity(BoboIndexReader reader)
	  {
	   return 0.0;
	  }
	 
	@Override
	public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboIndexReader reader) throws IOException 
	{
		return EmptyDocIdSet.getInstance();
	}
	
	public static EmptyFilter getInstance()
	{
		return instance;
	}
	
}
