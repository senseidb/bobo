package com.browseengine.bobo.query.scoring;

import java.util.Map;

import com.browseengine.bobo.api.BoboSegmentReader;

public interface FacetScoreable {
  BoboDocScorer getDocScorer(BoboSegmentReader reader,
      FacetTermScoringFunctionFactory scoringFunctionFactory, Map<String, Float> boostMap);
}
