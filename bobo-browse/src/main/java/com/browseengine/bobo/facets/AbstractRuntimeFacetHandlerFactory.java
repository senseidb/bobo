package com.browseengine.bobo.facets;


public abstract class AbstractRuntimeFacetHandlerFactory<P extends FacetHandlerInitializerParam, F extends RuntimeFacetHandler<?>> implements
		RuntimeFacetHandlerFactory<P, F> {
  /**
   * @return if this facet support empty params or not. By default it returns
   * false.
   */
  @Override
  public boolean isLoadLazily()
  {
    return false;
  }
}
