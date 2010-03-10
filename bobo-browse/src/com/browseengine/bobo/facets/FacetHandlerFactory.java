/**
 * 
 */
package com.browseengine.bobo.facets;

/**
 * This interface is intended for using with RuntimeFacetHandler, which typically
 * have local data that make them not only NOT thread safe but also dependent on
 * request. So it is necessary to have different instance for different client or
 * request. Typically, the new instance need to be initialized before use.
 * @author xiaoyang
 *
 */
public interface FacetHandlerFactory<F extends FacetHandler<?>>
{
  /**
   * @return a new instance of of type F.
   */
  F newInstance();
}
