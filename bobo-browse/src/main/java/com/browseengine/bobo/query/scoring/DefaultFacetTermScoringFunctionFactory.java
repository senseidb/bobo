package com.browseengine.bobo.query.scoring;

public class DefaultFacetTermScoringFunctionFactory implements
		FacetTermScoringFunctionFactory {

	public FacetTermScoringFunction getFacetTermScoringFunction(int termCount,
			int docCount) {
		return new DefaultFacetTermScoringFunction();
	}

}
