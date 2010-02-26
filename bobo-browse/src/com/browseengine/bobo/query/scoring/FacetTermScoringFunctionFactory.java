package com.browseengine.bobo.query.scoring;

public interface FacetTermScoringFunctionFactory {
	FacetTermScoringFunction getFacetTermScoringFunction(int termCount,int docCount);
}
