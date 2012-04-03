package com.browseengine.bobo.facets.statistics;



public class ChiSquaredFacetCountStatisticsGenerator extends FacetCountStatisicsGenerator
{

  @Override
  public double calculateDistributionScore(int[] distribution,
                                          int collectedSampleCount,
                                          int numSamplesCollected,
                                          int totalSamplesCount)
  {
    double expected = (double)collectedSampleCount / (double)numSamplesCollected;
    
    double sum = 0.0;
    for (int count : distribution)
    {
      double v = (double)count - expected;
      sum += (v * v);
    }
    
    return sum/expected;
  }
}
