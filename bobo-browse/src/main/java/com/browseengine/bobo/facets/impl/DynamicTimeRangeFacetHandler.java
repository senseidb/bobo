/**
 * 
 */
package com.browseengine.bobo.facets.impl;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

public class DynamicTimeRangeFacetHandler extends DynamicRangeFacetHandler
{
  private static final Logger log = Logger.getLogger(DynamicTimeRangeFacetHandler.class.getName());
  public static final String NUMBER_FORMAT = "00000000000000000000";

  protected ThreadLocal<DecimalFormat> _formatter = null;
  
  public static long MILLIS_IN_DAY = 24L * 60L * 60L * 1000L;
  public static long MILLIS_IN_HOUR = 60L * 60L * 1000L;
  public static long MILLIS_IN_MIN = 60L * 1000L;
  public static long MILLIS_IN_SEC = 1000L;
  
  private final HashMap<String,String> _valueToRangeStringMap;
  private final HashMap<String,String> _rangeStringToValueMap;
  private final ArrayList<String> _rangeStringList;
  
  /**
   * the format of range string is dddhhmmss. (ddd: days (000-999), hh : hours (00-23), mm: minutes (00-59), ss: seconds (00-59))
   * @param name
   * @param dataFacetName
   * @param currentTime
   * @param ranges
   */
  public DynamicTimeRangeFacetHandler(String name, String dataFacetName, long currentTime, List<String> ranges) throws ParseException
  {
    super(name, dataFacetName);
    _formatter = new ThreadLocal<DecimalFormat>()
    {
      protected DecimalFormat initialValue()
      {
        return new DecimalFormat(NUMBER_FORMAT);
      }
    };
    
    if (log.isDebugEnabled()){
      log.debug(name +" " + dataFacetName + " " + currentTime);
    }
    ArrayList<String> sortedRanges = new ArrayList<String>(ranges);
    Collections.sort(sortedRanges);
    
    _valueToRangeStringMap = new HashMap<String,String>();
    _rangeStringToValueMap = new HashMap<String,String>();
    _rangeStringList = new ArrayList<String>(ranges.size());
    
    String prev = "000000000";
    for(String range : sortedRanges)
    {
      String rangeString = buildRangeString(currentTime, prev, range);
      _valueToRangeStringMap.put(range, rangeString);
      _rangeStringToValueMap.put(rangeString, range);
      _rangeStringList.add(rangeString);
      prev = range;
      
      if (log.isDebugEnabled()){
        log.debug(range + "\t " + rangeString);
      }
    }
  }
  
  private DynamicTimeRangeFacetHandler(String name, String dataFacetName,
                                 HashMap<String,String> valueToRangeStringMap,
                                 HashMap<String,String> rangeStringToValueMap,
                                 ArrayList<String> rangeStringList)
  {
    super(name, dataFacetName);
    _valueToRangeStringMap = valueToRangeStringMap;
    _rangeStringToValueMap = rangeStringToValueMap;
    _rangeStringList = rangeStringList;
  }
  
  private static long getTime(long time, String range) throws ParseException
  {
    if(range.length() != 9) throw new ParseException("invalid range format: " + range, 0);
    try
    {
      int val;
      
      val = Integer.parseInt(range.substring(0, 3));
      time -= val * MILLIS_IN_DAY;
      
      val = Integer.parseInt(range.substring(3, 5));
      if(val >= 24) throw new ParseException("invalid range format: " + range, 0);
      time -= val * MILLIS_IN_HOUR;
      
      val = Integer.parseInt(range.substring(5, 7));
      if(val >= 60) throw new ParseException("invalid range format: " + range, 0);
      time -= val * MILLIS_IN_MIN;

      val = Integer.parseInt(range.substring(7, 9));
      if(val >= 60) throw new ParseException("invalid range format: " + range, 0);
      time -= val * MILLIS_IN_SEC;
      
      return time;
    }
    catch (NumberFormatException e)
    {
      throw new ParseException("invalid time format:" + range, 0);
    }
  }
  
  private String buildRangeString(long currentTime, String dStart, String dEnd) throws ParseException
  {
    String end = _formatter.get().format(getTime(currentTime, dStart));
    String start = _formatter.get().format(getTime(currentTime, dEnd) + 1);
    StringBuilder buf = new StringBuilder();
    buf.append("[").append(start).append(" TO ").append(end).append("]");
    return buf.toString();
  }
  
  @Override
  protected String buildRangeString(String val)
  {
    return _valueToRangeStringMap.get(val);
  }

  @Override
  protected List<String> buildAllRangeStrings()
  {
    return _rangeStringList;
  }

  @Override
  protected String getValueFromRangeString(String val)
  {
    return _rangeStringToValueMap.get(val);
  }
  
  public DynamicTimeRangeFacetHandler newInstance()
  {
    return new DynamicTimeRangeFacetHandler(getName(), _dataFacetName, _valueToRangeStringMap, _rangeStringToValueMap, _rangeStringList);
  }
}
