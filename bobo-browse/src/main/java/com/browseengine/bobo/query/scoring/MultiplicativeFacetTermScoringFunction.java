package com.browseengine.bobo.query.scoring;

import java.util.Arrays;

import org.apache.lucene.search.Explanation;

public class MultiplicativeFacetTermScoringFunction implements FacetTermScoringFunction
{
  private float _boost = 1.0f;
  
  public final void clearScores()
  {
    _boost = 1.0f;
  }
  
  public final float score(int df, float boost)
  {
    return boost;
  }
  
  public final void scoreAndCollect(int df,float boost)
  {
	if (boost>0){
      _boost *= boost;
	}
  }

  public final float getCurrentScore()
  {
    return _boost;
  }

  public Explanation explain(int df, float boost)
  {
    Explanation expl = new Explanation();
    expl.setValue(score(df,boost));
    expl.setDescription("boost value of: "+boost);
    return expl;
  }

  public Explanation explain(float... scores) {
      Explanation expl = new Explanation();
      float boost = 1.0f;
      for (float score : scores){
          boost *=score;
      }
      expl.setValue(boost);
      expl.setDescription("product of: "+Arrays.toString(scores));
      return expl;
  }
}
