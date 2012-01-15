package com.browseengine.bobo.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.query.scoring.BoboDocScorer;
import com.browseengine.bobo.query.scoring.FacetScoreable;
import com.browseengine.bobo.query.scoring.FacetTermScoringFunctionFactory;
import com.browseengine.bobo.query.scoring.MultiplicativeFacetTermScoringFunctionFactory;

public class FacetBasedBoostScorerBuilder implements ScorerBuilder
{
  protected final Map<String,Map<String,Float>> _boostMaps;
  protected final FacetTermScoringFunctionFactory _scoringFunctionFactory;
  
  public FacetBasedBoostScorerBuilder(Map<String,Map<String,Float>> boostMaps)
  {
    this(boostMaps, new MultiplicativeFacetTermScoringFunctionFactory());
  }
  
  protected FacetBasedBoostScorerBuilder(Map<String,Map<String,Float>> boostMaps, FacetTermScoringFunctionFactory scoringFunctionFactory)
  {
    _boostMaps = boostMaps;
    _scoringFunctionFactory = scoringFunctionFactory;
  }
  
  public Scorer createScorer(Scorer innerScorer, IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException
  {
    if(!(reader instanceof BoboIndexReader)) throw new IllegalArgumentException("IndexReader is not BoboIndexReader");
    
    return new FacetBasedBoostingScorer((BoboIndexReader)reader, innerScorer.getSimilarity(), innerScorer);
  }
  
  public Explanation explain(IndexReader indexReader, int docid, Explanation innerExplaination) throws IOException
  {
    if(!(indexReader instanceof BoboIndexReader)) throw new IllegalArgumentException("IndexReader is not BoboIndexReader");
    BoboIndexReader reader = (BoboIndexReader)indexReader;
    
    Explanation exp = new Explanation();
    exp.setDescription("FacetBasedBoost");
    
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
    exp.addDetail(innerExplaination);
    return exp;
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
        float fscore = facetScorer.score(_docid);
        if (fscore>0.0){
        	score*=fscore;
        }
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
  }
}
