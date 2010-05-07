/**
 * 
 */
package com.browseengine.bobo.api;

/**
 * @author "Xiaoyang Gu<xgu@linkedin.com>"
 *
 */
public abstract class ShortFacetIterator extends FacetIterator
{
  public short facet;
  public abstract short nextShort();
  public abstract short nextShort(int minHits);
  public abstract String format(short val);
}
