package com.browseengine.bobo.query.scoring;

import org.apache.lucene.search.Explanation;

public interface FacetTermScoringFunction {
	public void clearScores();
	public float score(int df,float boost);
	public void scoreAndCollect(int df,float boost);
	public Explanation explain(int df,float boost);
	public float getCurrentScore();
	public Explanation explain(float...scores);
}
