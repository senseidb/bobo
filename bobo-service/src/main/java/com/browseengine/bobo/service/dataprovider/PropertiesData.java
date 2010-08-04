package com.browseengine.bobo.service.dataprovider;

import java.io.Serializable;
import java.util.HashMap;

public class PropertiesData implements Serializable
{

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  
  private boolean _skip;
  private final HashMap<String,String> _data;
  private final long _id;

  public PropertiesData(HashMap<String,String> data,long id)
  {
    _data = data;
    _id = id;
    _skip = false;
  }
  
  public long getID()
  {
    return _id;
  }
  
  public HashMap<String,String> getData()
  {
    return _data;
  }
  
  public void setSkip(boolean skip)
  {
    _skip = skip;
  }
  
  public boolean isSkip()
  {
    return _skip;
  }
}
