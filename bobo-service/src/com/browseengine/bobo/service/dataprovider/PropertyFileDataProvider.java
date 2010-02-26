package com.browseengine.bobo.service.dataprovider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import proj.zoie.api.DataConsumer.DataEvent;
import proj.zoie.impl.indexing.StreamDataProvider;

public class PropertyFileDataProvider extends StreamDataProvider<PropertiesData>
{
  private boolean _replay;
  private HashMap<String,String>[] _propDataList;
  private int _currentVersion;
  private int _currentID;
  private final int _maxID;
  
  public PropertyFileDataProvider(File datafile, int maxID) throws IOException
  {
    FileInputStream fin = null;
    ArrayList<HashMap<String,String>> propDataList = new ArrayList<HashMap<String,String>>();
    try
    {
      fin = new FileInputStream(datafile);
      BufferedReader br = new BufferedReader(new InputStreamReader(fin,"UTF-8"));
      HashMap<String,String> dataMap = null;
      while (true)
      {
        String line = br.readLine();
        if (line == null) break;
        if ("<EOD>".equals(line))
        {
          if (dataMap!=null)
          {
            propDataList.add(dataMap);
            dataMap=null;
          }
        }
        else
        {
          if (dataMap == null)
          {
            dataMap = new HashMap<String,String>();
          }
          String[] pair = line.split(":");
          dataMap.put(pair[0],pair[1]);
        }
      }
    }
    finally
    {
      if (fin!=null)
      {
        fin.close();
      }
    }
    _propDataList = propDataList.toArray(new HashMap[propDataList.size()]);
    _replay = false;
    reset();
    _maxID = maxID;
  }
  
  public void setReplay(boolean replay)
  {
    _replay = replay;
  }
  
  @Override
  public DataEvent<PropertiesData> next()
  {

    if (!_replay && _currentID>=_propDataList.length) return null;
    
    int index = _currentID % _propDataList.length;

    HashMap<String,String> data = _propDataList[index];
    PropertiesData propData = new PropertiesData(data,_currentID);
    DataEvent<PropertiesData> event = new DataEvent<PropertiesData>(_currentVersion,propData);
    _currentID ++;
    if (index == 0)
    {
      _currentVersion ++;
    }
    if (_currentID>=_maxID)
    {
      _currentID = 0;
    }
    return event;
  }

  @Override
  public void reset()
  {
    _currentID = 0;
    _currentVersion = 0;
  }

}
