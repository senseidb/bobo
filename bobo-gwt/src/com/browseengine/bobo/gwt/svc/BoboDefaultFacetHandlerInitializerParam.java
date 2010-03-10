/**
 * 
 */
package com.browseengine.bobo.gwt.svc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author xiaoyang
 *
 */
public class BoboDefaultFacetHandlerInitializerParam implements IsSerializable
{
  private Map<String, boolean[]> _boolMap;
  private Map<String, int[]> _intMap;
  private Map<String, long[]> _longMap;
  private Map<String, List<String>> _stringMap;
  private Map<String, byte[]> _byteMap;
  private Map<String, double[]> _doubleMap;

  public BoboDefaultFacetHandlerInitializerParam()
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

  public void putBooleanParam(String key, boolean[] value)
  {
    _boolMap.put(key, value);
  }

  public boolean[] getBooleanParam(String name)
  {
    return _boolMap.get(name);
  }

  public void putByteArrayParam(String key, byte[] value)
  {
    _byteMap.put(key, value);
  }

  public byte[] getByteArrayParam(String name)
  {
    return _byteMap.get(name);
  }

  public void putIntParam(String key, int[] value)
  {
    _intMap.put(key, value);
  }

  public int[] getIntParam(String name)
  {
    return _intMap.get(name);
  }

  public void putLongParam(String key, long[] value)
  {
    _longMap.put(key, value);
  }

  public long[] getLongParam(String name)
  {
    return _longMap.get(name);
  }

  public void putStringParam(String key, List<String> value)
  {
    _stringMap.put(key, value);
  }

  public List<String> getStringParam(String name)
  {
    return _stringMap.get(name);
  }

  public void putDoubleParam(String key, double[] value)
  {
    _doubleMap.put(key, value);
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
