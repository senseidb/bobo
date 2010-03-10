package com.browseengine.bobo.facets;

/**
 * This interface is intended to use with RuntimeFacetHandlers so that
 * they can be initialized using data supplied by param at run time.
 * @author xiaoyang
 *
 * @param <P>
 */
public interface RuntimeInitializable<P extends FacetHandlerInitializerParam>
{
  /**
   * Initialize this object with data from params.
   * @param params
   */
  void init(P params);
}
