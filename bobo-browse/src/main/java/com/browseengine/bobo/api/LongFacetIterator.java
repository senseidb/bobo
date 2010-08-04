/**
 * 
 */
package com.browseengine.bobo.api;

/**
 * @author "Xiaoyang Gu<xgu@linkedin.com>"
 *
 */
public abstract class LongFacetIterator extends FacetIterator
{
  public long facet;
  public abstract long nextLong();
  public abstract long nextLong(int minHits);
  public abstract String format(long val);
}
