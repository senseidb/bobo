package com.browseengine.bobo.query.scoring;

import java.util.Arrays;

import org.apache.lucene.search.Explanation;

public class DefaultFacetTermScoringFunction implements FacetTermScoringFunction {
	private float _sum=0.0f;
	
	public final void clearScores(){
		_sum = 0.0f;
	}
	
	public final float score(int df, float boost) {
		return boost;
	}
	
	public final void scoreAndCollect(int df,float boost){
		_sum+=boost;
	}

	public final float getCurrentScore() {
		return _sum;
	}

	public Explanation explain(int df, float boost) {
		Explanation expl = new Explanation();
		expl.setValue(score(df,boost));
		expl.setDescription("facet boost value of: "+boost);
		return expl;
	}

	public Explanation explain(float... scores) {
		Explanation expl = new Explanation();
		float sum = 0.0f;
		for (float score : scores){
			sum+=score;
		}
		expl.setValue(sum);
		expl.setDescription("sum of: "+Arrays.toString(scores));
		return expl;
	}
}
