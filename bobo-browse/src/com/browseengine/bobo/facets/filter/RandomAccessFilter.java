package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;

public abstract class RandomAccessFilter extends Filter
{
  private static final long serialVersionUID = 1L;

  @Override 
  public DocIdSet getDocIdSet(IndexReader reader) throws IOException
  {
	if (reader instanceof BoboIndexReader){
      return getRandomAccessDocIdSet((BoboIndexReader)reader);
	}
	else{
	  throw new IllegalStateException("reader not instance of "+BoboIndexReader.class);
	}
  }
  
  public abstract RandomAccessDocIdSet getRandomAccessDocIdSet(BoboIndexReader reader) throws IOException;
  public double getFacetSelectivity(BoboIndexReader reader) { return 0.50; }
  
}
