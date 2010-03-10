/**
 * 
 */
package com.browseengine.bobo.facets;

/**
 * @author xiaoyang
 *
 */
public interface FacetHandlerFactory<F extends FacetHandler<?>>
{
  F newInstance();
}
