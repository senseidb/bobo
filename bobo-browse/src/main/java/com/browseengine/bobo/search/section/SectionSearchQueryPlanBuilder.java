/**
 * 
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 *
 */
public class SectionSearchQueryPlanBuilder
{
  public static class TranslationException extends RuntimeException
  {
    private static final long serialVersionUID = 1L;

    public TranslationException(String message)
    {
      super(message);
    }
  }
  
  protected final IndexReader _reader;
  protected final MetaDataCacheProvider _cacheProvider;
  
  public SectionSearchQueryPlanBuilder(IndexReader reader)
  {
    _reader = reader;
    _cacheProvider = (reader instanceof MetaDataCacheProvider ? (MetaDataCacheProvider)reader : null);
  }
  
  /**
   * Gets a query plan for the given query.
   * It is assumed that <code>query</code> is already rewritten before this call.
   * @param query
   * @return SectionSearchQueryPlan
   * @throws IOException
   */
  public SectionSearchQueryPlan getPlan(Query query)
    throws IOException
  {
    if(query != null)
    {
      SectionSearchQueryPlan textSearchPlan = translate(query);
      
      if(!(textSearchPlan instanceof UnaryNotNode))
      {
        return textSearchPlan;
      }
    }
    return null;
  }
  
  /**
   * Translates a Lucence Query object to an SectionSearchQueryPlan 
   * @param query
   * @param reader
   * @return
   * @throws IOException
   */
  private SectionSearchQueryPlan translate(Query query)
    throws IOException
  {
    if(query != null)
    {
      if(query instanceof TermQuery)
      {
        return translateTermQuery((TermQuery) query);
      }
      else if(query instanceof PhraseQuery)
      {
        return translatePhraseQuery((PhraseQuery) query);
      }
      else if(query instanceof BooleanQuery)
      {
        return translateBooleanQuery((BooleanQuery) query);
      }
      else if(query instanceof MetaDataQuery)
      {
        MetaDataQuery mquery = (MetaDataQuery)query;
        MetaDataCache cache = (_cacheProvider != null ? _cacheProvider.get(mquery.getTerm()) : null);
        
        if(cache != null)
        {
          return ((MetaDataQuery)query).getPlan(cache);
        }
        else
        {
          return ((MetaDataQuery)query).getPlan(_reader);
        }
      }
      else
      {
        throw new TranslationException("unable to translate Query class: " + query.getClass().getName());
      }
    }
    return null;
  }
  
  private SectionSearchQueryPlan translateTermQuery(TermQuery query)
    throws IOException
  {
    return new TermNode(query.getTerm(), _reader);
  }

  private SectionSearchQueryPlan translatePhraseQuery(PhraseQuery query)
    throws IOException
  {
    Term[] terms = query.getTerms();
    TermNode[] nodes = new TermNode[terms.length];
    int[] positions = query.getPositions();
    for(int i = 0; i < terms.length; i++)
    {
      nodes[i] = new TermNode(terms[i], positions[i], _reader);
    }
    return new PhraseNode(nodes, _reader);
  }
  
  private SectionSearchQueryPlan translateBooleanQuery(BooleanQuery query)
    throws IOException
  {
    ArrayList<Query> requiredClauses = new ArrayList<Query>();
    ArrayList<Query> prohibitedClauses = new ArrayList<Query>();
    ArrayList<Query> optionalClauses = new ArrayList<Query>();
    BooleanClause[] clauses = query.getClauses();
    for(BooleanClause clause : clauses)
    {
      if(clause.isRequired())
      {
        requiredClauses.add(clause.getQuery());
      }
      else if(clause.isProhibited())
      {
        prohibitedClauses.add(clause.getQuery());
      }
      else
      {
        optionalClauses.add(clause.getQuery());
      }
    }
    
    SectionSearchQueryPlan positiveNode = null;
    SectionSearchQueryPlan negativeNode = null;
    
    if(requiredClauses.size() > 0)
    {
      if(requiredClauses.size() == 1)
      {
        positiveNode = translate(requiredClauses.get(0));
      }
      else
      {
        SectionSearchQueryPlan[] subqueries = translate(requiredClauses);
        if(subqueries != null && subqueries.length > 0) positiveNode = new AndNode(subqueries);
      }
    }
    else if(optionalClauses.size() > 0)
    {
      if(optionalClauses.size() == 1)
      {
        positiveNode = translate(optionalClauses.get(0));
      }
      else
      {
        SectionSearchQueryPlan[] subqueries = translate(optionalClauses);
        if(subqueries != null && subqueries.length > 0) positiveNode = new OrNode(subqueries);
      }
    }
    
    if(prohibitedClauses.size() > 0)
    {
      if(prohibitedClauses.size() == 1)
      {
        negativeNode = translate(prohibitedClauses.get(0));        
      }
      else
      {
        negativeNode = new OrNode(translate(prohibitedClauses));
      }
    }
    
    if(negativeNode == null)
    {
      return positiveNode;
    }
    else
    {
      if(positiveNode == null)
      {
        return new UnaryNotNode(negativeNode);
      }
      else
      {
        return new AndNotNode(positiveNode, negativeNode);
      }
    }
  }

  private SectionSearchQueryPlan[] translate(ArrayList<Query> queries)
    throws IOException
  {
    int size = queries.size();
    ArrayList<SectionSearchQueryPlan> result = new ArrayList<SectionSearchQueryPlan>(size);
    for(int i = 0; i < size; i++)
    {
      SectionSearchQueryPlan plan = translate(queries.get(i));
      if(plan != null) result.add(plan);
    }
    return result.toArray(new SectionSearchQueryPlan[result.size()]);
  }
}
