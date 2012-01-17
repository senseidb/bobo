/**
 * 
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

/**
 *
 */
public class SectionSearchQuery extends Query
{
  private static final long serialVersionUID = 1L;
  
  private Query _query;
  
  private class SectionSearchWeight extends Weight
  {
    private static final long serialVersionUID = 1L;
    
    float _weight;
    Similarity _similarity;

    public SectionSearchWeight(Searcher searcher) throws IOException
    {
      _similarity = getSimilarity(searcher);
    }

    public String toString()
    {
      return "weight(" + SectionSearchQuery.this + ")";
    }

    public Query getQuery()
    {
      return SectionSearchQuery.this;
    }

    public float getValue()
    {
      return getBoost();
    }

    public float sumOfSquaredWeights()
    {
      _weight = getBoost();
      return _weight * _weight;
    }

    @Override
    public void normalize(float queryNorm)
    {
      _weight *= queryNorm;
    }

    public Scorer scorer(IndexReader reader) throws IOException
    {
      SectionSearchScorer scorer = new SectionSearchScorer(_similarity, getValue(), reader);
      
      return scorer;
    }

    @Override
    public Explanation explain(IndexReader reader, int doc) throws IOException
    {
      Explanation result = new Explanation();
      result.setValue(_weight);
      result.setDescription(SectionSearchQuery.this.toString());

      return result;
    }

    @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException
    {
      return scorer(reader);
    }
  }

  public class SectionSearchScorer extends Scorer
  {
    private int              _curDoc = -1;
    private float            _curScr;
    private boolean          _more = true; // more hits
    private SectionSearchQueryPlan _plan;
    
    public SectionSearchScorer(Similarity similarity, float score, IndexReader reader)
      throws IOException
    {
      super(similarity);
      _curScr = score;
      
      SectionSearchQueryPlanBuilder builer = new SectionSearchQueryPlanBuilder(reader);
      _plan = builer.getPlan(_query);
      if(_plan != null)
      {
        _curDoc = -1;
        _more = true;
      }
      else
      {
        _curDoc = DocIdSetIterator.NO_MORE_DOCS;
        _more = false;;        
      }
    }
    
    @Override
    public int docID()
    {
      return _curDoc;
    }

    @Override
    public int nextDoc() throws IOException
    {
      return advance(0);
    }

    @Override
    public float score() throws IOException
    {
      return _curScr;
    }

    @Override
    public int advance(int target) throws IOException
    {
      if(_curDoc < DocIdSetIterator.NO_MORE_DOCS)
      {
        if(target <= _curDoc) target = _curDoc + 1;
  
        return _plan.fetch(target);
      }
      return _curDoc;
    }
  }
  
  /**
   * constructs SectionSearchQuery
   * 
   * @param query
   */
  public SectionSearchQuery(Query query)
  {
    _query = query;
  }

  @Override
  public String toString(String field)
  {
    StringBuilder buffer = new StringBuilder();
    buffer.append("SECTION(" + _query.toString() + ")");
    return buffer.toString();
  }

  @Override
  public Weight createWeight(Searcher searcher) throws IOException
  {
    return new SectionSearchWeight(searcher);
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException
  {
    _query.rewrite(reader);
    return this;
  }
}
