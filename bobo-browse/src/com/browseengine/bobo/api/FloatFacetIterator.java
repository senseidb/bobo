/**
 * 
 */
package com.browseengine.bobo.api;

/**
 * @author "Xiaoyang Gu<xgu@linkedin.com>"
 *
 */
public abstract class FloatFacetIterator extends FacetIterator
{
  public float facet;
  public abstract float nextFloat();
  public abstract float nextFloat(int minHits);
  public abstract String format(float val);
}
