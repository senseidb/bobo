/**
 * 
 */
package com.browseengine.bobo.facets;

import java.io.Serializable;

/**
 * The dummy interface to indicate that a class type can be used for initializing RuntimeFacetHandlers.
 * @author xiaoyang
 *
 */
public abstract class FacetHandlerInitializerParam implements Serializable
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  /**
   * The transaction ID
   */
  private long tid = -1;
  /**
   * Get the transaction ID.
   * @return the transaction ID.
   */
  public final long getTid()
  {
    return tid;
  }

  /**
   * Set the transaction ID;
   * @param tid
   */
  public final void setTid(long tid)
  {
    this.tid = tid;
  }

}
