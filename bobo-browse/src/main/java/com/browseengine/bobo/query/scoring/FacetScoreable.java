package com.browseengine.bobo.query.scoring;

import java.util.Map;

import com.browseengine.bobo.api.BoboIndexReader;

public interface FacetScoreable {
	 BoboDocScorer getDocScorer(BoboIndexReader reader,
			 					FacetTermScoringFunctionFactory scoringFunctionFactory,
			 					Map<String,Float> boostMap);
}
