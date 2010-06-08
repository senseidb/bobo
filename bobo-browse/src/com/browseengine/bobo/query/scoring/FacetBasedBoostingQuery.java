package com.browseengine.bobo.query.scoring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.FacetHandler;

public class FacetBasedBoostingQuery extends Query
{
  private static final long serialVersionUID = 1L;
  
  protected final Query _query;
  protected final Map<String,Map<String,Float>> _boostMaps;
  protected final FacetTermScoringFunctionFactory _scoringFunctionFactory;
  
  public FacetBasedBoostingQuery(Query query, Map<String,Map<String,Float>> boostMaps)
  {
    this(query, boostMaps, new MultiplicativeFacetTermScoringFunctionFactory());
  }
  
  public FacetBasedBoostingQuery(Query query, Map<String,Map<String,Float>> boostMaps, FacetTermScoringFunctionFactory scoringFunctionFactory)
  {
    _query = query;
    _boostMaps = boostMaps;
    _scoringFunctionFactory = scoringFunctionFactory;
  }
    
  @Override
  public Weight createWeight(Searcher searcher) throws IOException
  {
    return new FacetBasedBoostingWeight(searcher, _query.createWeight(searcher));
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException
  {
    _query.rewrite(reader);
    return this;
  }
  
  @Override
  public void extractTerms(Set terms)
  {
    _query.extractTerms(terms);
  }

  @Override
  public String toString(String field)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("FacetBasedBoosting(");
    sb.append(_query.toString(field));
    sb.append(")");
    return sb.toString();
  }
  
  private class FacetBasedBoostingWeight extends Weight
  {
    private static final long serialVersionUID = 1L;
    
    private Searcher _searcher;
    private Weight _innerWeight;
    
    public FacetBasedBoostingWeight(Searcher searcher, Weight innerWeight) throws IOException
    {
      _searcher = searcher;
      _innerWeight = innerWeight;
    }
    
    public String toString()
    {
      return "weight(" + FacetBasedBoostingWeight.this + ")";
    }
    
    public Query getQuery()
    {
      return _innerWeight.getQuery();
    }
    
    public float getValue()
    {
      return _innerWeight.getValue();
    }
    
    public float sumOfSquaredWeights() throws IOException
    {
      return _innerWeight.sumOfSquaredWeights();
    }
    
    public void normalize(float queryNorm)
    {
      _innerWeight.normalize(queryNorm);
    }
    
    @Override
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException
    {
      if(!(reader instanceof BoboIndexReader)) throw new IllegalArgumentException("IndexReader is not BoboIndexReader");
      
      Scorer innerScorer = _innerWeight.scorer(reader, scoreDocsInOrder, topScorer);
      return new FacetBasedBoostingScorer((BoboIndexReader)reader, _searcher.getSimilarity(), innerScorer);
    }
    
    public Explanation explain(IndexReader indexReader, int docid) throws IOException
    {
      if(!(indexReader instanceof BoboIndexReader)) throw new IllegalArgumentException("IndexReader is not BoboIndexReader");
      BoboIndexReader reader = (BoboIndexReader)indexReader;
      
      Explanation exp = new Explanation();
      exp.setDescription("FacetBasedBoosting");
      
      float boost = 1.0f;
      for(Map.Entry<String,Map<String,Float>> boostEntry : _boostMaps.entrySet())
      {
        String facetName = boostEntry.getKey();
        FacetHandler<?> handler = reader.getFacetHandler(facetName);
        if(!(handler instanceof FacetScoreable))
          throw new IllegalArgumentException(facetName + " does not implement FacetScoreable");
        
        FacetScoreable facetScoreable = (FacetScoreable)handler;
        BoboDocScorer scorer = facetScoreable.getDocScorer(reader, _scoringFunctionFactory, boostEntry.getValue());
        float facetBoost = scorer.score(docid);

        Explanation facetExp = new Explanation();
        facetExp.setDescription(facetName);
        facetExp.setValue(facetBoost);
        facetExp.addDetail(scorer.explain(docid));
        boost *= facetBoost;
        exp.addDetail(facetExp);
      }
      exp.setValue(boost);
      exp.addDetail(_innerWeight.explain(reader, docid));
      return exp;
    }
  }

  private class FacetBasedBoostingScorer extends Scorer
  {
    private final Scorer _innerScorer;
    private final BoboDocScorer[] _facetScorers;
    
    private int _docid;
    
    public FacetBasedBoostingScorer(BoboIndexReader reader, Similarity similarity, Scorer innerScorer)
    {
      super(similarity);
      _innerScorer = innerScorer;
            
      ArrayList<BoboDocScorer> list = new ArrayList<BoboDocScorer>();
      
      for(Map.Entry<String,Map<String,Float>> boostEntry : _boostMaps.entrySet())
      {
        String facetName = boostEntry.getKey();
        FacetHandler<?> handler = reader.getFacetHandler(facetName);
        if(!(handler instanceof FacetScoreable))
          throw new IllegalArgumentException(facetName + " does not implement FacetScoreable");
        FacetScoreable facetScoreable = (FacetScoreable)handler;
        BoboDocScorer scorer = facetScoreable.getDocScorer(reader, _scoringFunctionFactory, boostEntry.getValue());
        if(scorer != null) list.add(scorer);
      }
      _facetScorers = list.toArray(new BoboDocScorer[list.size()]);
      _docid = -1;
    }
    
    @Override
    public float score() throws IOException
    {
      float score = _innerScorer.score();
      for(BoboDocScorer facetScorer : _facetScorers)
      {
        score *= facetScorer.score(_docid);
      }
      return score;
    }
    
    @Override
    public int docID()
    {
      return _docid;
    }
    
    @Override
    public int nextDoc() throws IOException
    {
      return (_docid = _innerScorer.nextDoc());
    }
    
    @Override
    public int advance(int target) throws IOException
    {
      return (_docid = _innerScorer.advance(target));
    }

    @Override
    public int doc()
    {
      return _docid;
    }

    @Override
    public boolean next() throws IOException
    {
      return (nextDoc() != NO_MORE_DOCS);
    }

    @Override
    public boolean skipTo(int target) throws IOException
    {
      return (advance(target) != NO_MORE_DOCS);
    }
  }
}
