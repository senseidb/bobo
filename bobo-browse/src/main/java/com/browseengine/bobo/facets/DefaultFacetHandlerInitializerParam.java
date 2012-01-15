package com.browseengine.bobo.facets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The 'generic' type for FacetHandler initialization parameters for the purpose of easy serialization.
 * When this type is used, it is completely up to the program logic of the utilizing RuntimeFacetHandler
 * and its client code to find the data at the right place.
 * @author ymatsuda
 *
 */
public class DefaultFacetHandlerInitializerParam extends FacetHandlerInitializerParam
{
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private final Map<String, boolean[]> _boolMap;
  private final Map<String, int[]> _intMap;
  private final Map<String, long[]> _longMap;
  private final Map<String, List<String>> _stringMap;
  private final Map<String, byte[]> _byteMap;
  private final Map<String, double[]> _doubleMap;

  public DefaultFacetHandlerInitializerParam()
  {
    _boolMap = new HashMap<String, boolean[]>();
    _intMap = new HashMap<String, int[]>();
    _longMap = new HashMap<String, long[]>();
    _stringMap = new HashMap<String, List<String>>();
    _byteMap = new HashMap<String, byte[]>();
    _doubleMap = new HashMap<String, double[]>();
  }

  public Set<String> getBooleanParamNames()
  {
    return _boolMap.keySet();
  }

  public Set<String> getStringParamNames()
  {
    return _stringMap.keySet();
  }

  public Set<String> getIntParamNames()
  {
    return _intMap.keySet();
  }

  public Set<String> getByteArrayParamNames()
  {
    return _byteMap.keySet();
  }

  public Set<String> getLongParamNames()
  {
    return _longMap.keySet();
  }

  public Set<String> getDoubleParamNames()
  {
    return _doubleMap.keySet();
  }

  public DefaultFacetHandlerInitializerParam putBooleanParam(String key, boolean[] value)
  {
    _boolMap.put(key, value);
    return this;
  }

  public boolean[] getBooleanParam(String name)
  {
    return _boolMap.get(name);
  }

  public DefaultFacetHandlerInitializerParam putByteArrayParam(String key, byte[] value)
  {
    _byteMap.put(key, value);
    return this;
  }

  public byte[] getByteArrayParam(String name)
  {
    return _byteMap.get(name);
  }

  public DefaultFacetHandlerInitializerParam putIntParam(String key, int[] value)
  {
    _intMap.put(key, value);
    return this;
  }

  public int[] getIntParam(String name)
  {
    return _intMap.get(name);
  }

  public DefaultFacetHandlerInitializerParam putLongParam(String key, long[] value)
  {
    _longMap.put(key, value);
    return this;
  }

  public long[] getLongParam(String name)
  {
    return _longMap.get(name);
  }

  public DefaultFacetHandlerInitializerParam putStringParam(String key, List<String> value)
  {
    _stringMap.put(key, value);
    return this;
  }

  public List<String> getStringParam(String name)
  {
    return _stringMap.get(name);
  }

  public DefaultFacetHandlerInitializerParam putDoubleParam(String key, double[] value)
  {
    _doubleMap.put(key, value);
    return this;
  }

  public double[] getDoubleParam(String name)
  {
    return _doubleMap.get(name);
  }

  public void clear()
  {
    _boolMap.clear();
    _intMap.clear();
    _longMap.clear();
    _stringMap.clear();
    _byteMap.clear();
  }

}
