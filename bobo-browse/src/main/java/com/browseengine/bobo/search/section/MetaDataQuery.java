/**
 * 
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

public abstract class MetaDataQuery extends Query
{
  private static final long serialVersionUID = 1L;
  
  protected Term _term;
  
  public MetaDataQuery(Term term)
  {
    _term = term;
  }
  
  public Term getTerm()
  {
    return _term;
  }
  
  public abstract SectionSearchQueryPlan getPlan(IndexReader reader) throws IOException;
  public abstract SectionSearchQueryPlan getPlan(MetaDataCache cache) throws IOException;
}
