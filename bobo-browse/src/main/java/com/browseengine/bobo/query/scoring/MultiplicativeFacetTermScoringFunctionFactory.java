package com.browseengine.bobo.query.scoring;

public class MultiplicativeFacetTermScoringFunctionFactory implements FacetTermScoringFunctionFactory
{
  public FacetTermScoringFunction getFacetTermScoringFunction(int termCount, int docCount)
  {
    return new MultiplicativeFacetTermScoringFunction();
  }
}
