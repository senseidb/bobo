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
public interface RuntimeFacetHandlerFactory<P extends FacetHandlerInitializerParam, F extends RuntimeFacetHandler<?>>
{
  /**
   * @return the facet name of the RuntimeFacetHandler it creates.
   */
  String getName();

  /**
   * @return if this facet support empty params or not.
   */
  boolean isLoadLazily();

  /**
   * @param params the data used to initialize the RuntimeFacetHandler.
   * @return a new instance of 
   */
  F get(P params);
}
