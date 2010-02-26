package com.browseengine.bobo.facets.statistics;

import java.io.Serializable;

public class FacetCountStatistics implements Serializable
{

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  private double _distribution;
  private int _totalSampleCount;
  private int _collectedSampleCount;
  private int _numSamplesCollected;
  
  public double getDistribution()
  {
    return _distribution;
  }
  
  public void setDistribution(double distribution)
  {
    _distribution = distribution;
  }
  
  public int getTotalSampleCount()
  {
    return _totalSampleCount;
  }
  
  public void setTotalSampleCount(int totalSampleCount)
  {
    _totalSampleCount = totalSampleCount;
  }
  
  public int getCollectedSampleCount()
  {
    return _collectedSampleCount;
  }
  
  public void setCollectedSampleCount(int collectedSampleCount)
  {
    _collectedSampleCount = collectedSampleCount;
  }

  public int getNumSamplesCollected()
  {
    return _numSamplesCollected;
  }

  public void setNumSamplesCollected(int numSamplesCollected)
  {
    _numSamplesCollected = numSamplesCollected;
  }
  
  @Override
  public String toString()
  {
    StringBuffer buf = new StringBuffer();
    buf.append("num samples collected: ").append(_numSamplesCollected);
    buf.append("\ncollected sample count: ").append(_collectedSampleCount);
    buf.append("\ntotal samples count: ").append(_totalSampleCount);
    buf.append("\ndistribution score: ").append(_distribution);
    return buf.toString();
  }
  
  @Override
  public boolean equals(Object o)
  {
    boolean ret = false;
    if (o instanceof FacetCountStatistics)
    {
      FacetCountStatistics stat = (FacetCountStatistics)o;
      if (_collectedSampleCount == stat._collectedSampleCount &&
          _numSamplesCollected == stat._numSamplesCollected &&
          _totalSampleCount == stat._totalSampleCount &&
          _distribution == stat._distribution)
      {
        ret = true;
      }
    }
    return ret;
  }
}
