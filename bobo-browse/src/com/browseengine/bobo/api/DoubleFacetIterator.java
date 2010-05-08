/**
 * 
 */
package com.browseengine.bobo.api;

/**
 * @author "Xiaoyang Gu<xgu@linkedin.com>"
 *
 */
public abstract class DoubleFacetIterator extends FacetIterator
{
  public double facet;
  public abstract double nextDouble();
  public abstract double nextDouble(int minHits);
  public abstract String format(double val);
}
