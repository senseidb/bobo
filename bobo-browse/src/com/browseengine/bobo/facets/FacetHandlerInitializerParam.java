/**
 * 
 */
package com.browseengine.bobo.facets;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The dummy interface to indicate that a class type can be used for initializing RuntimeFacetHandlers.
 * @author xiaoyang
 *
 */
public abstract class FacetHandlerInitializerParam implements Serializable
{
  public static final FacetHandlerInitializerParam EMPTY_PARAM = new FacetHandlerInitializerParam()
  {
    @Override
    public List<String> getStringParam(String name)
    {
      return null;
    }

    @Override
    public int[] getIntParam(String name)
    {
      return null;
    }

    @Override
    public boolean[] getBooleanParam(String name)
    {
      return null;
    }

    @Override
    public long[] getLongParam(String name)
    {
      return null;
    }

    @Override
    public byte[] getByteArrayParam(String name)
    {
      return null;
    }

    @Override
    public double[] getDoubleParam(String name)
    {
      return null;
    }

    @Override
    public Set<String> getBooleanParamNames()
    {
      return Collections.EMPTY_SET;
    }

    @Override
    public Set<String> getStringParamNames()
    {
      return Collections.EMPTY_SET;
    }

    @Override
    public Set<String> getIntParamNames()
    {
      return Collections.EMPTY_SET;
    }

    @Override
    public Set<String> getByteArrayParamNames()
    {
      return Collections.EMPTY_SET;
    }

    @Override
    public Set<String> getLongParamNames()
    {
      return Collections.EMPTY_SET;
    }

    @Override
    public Set<String> getDoubleParamNames()
    {
      return Collections.EMPTY_SET;
    }
  };

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

  public abstract List<String> getStringParam(String name);
  public abstract int[] getIntParam(String name);
  public abstract boolean[] getBooleanParam(String name);
  public abstract long[] getLongParam(String name);
  public abstract byte[] getByteArrayParam(String name);
  public abstract double[] getDoubleParam(String name);
  public abstract Set<String> getBooleanParamNames();
  public abstract Set<String> getStringParamNames();
  public abstract Set<String> getIntParamNames();
  public abstract Set<String> getByteArrayParamNames();
  public abstract Set<String> getLongParamNames();
  public abstract Set<String> getDoubleParamNames();
}
