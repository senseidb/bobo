package com.browseengine.bobo.util;

import org.apache.log4j.Logger;

import com.browseengine.bobo.api.BoboSubBrowser;

public class BoboSimpleDecimalFormat
{

  private static Logger                   log = Logger.getLogger(BoboSubBrowser.class);
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private static final int _maxPadding = 80;

  private static final BoboSimpleDecimalFormat[] _allBoboSimpleDecimalFormat = new BoboSimpleDecimalFormat[_maxPadding];

  private static final String [] _paddings = new String[_maxPadding];
  static{
    _paddings[0]="";
    for(int i=0; i<_maxPadding - 1; i++)
    {
      _paddings[i+1] = _paddings[i]+"0";
    }
  }

  static {
    for(int i=0; i<_maxPadding - 1; i++)
    {
      _allBoboSimpleDecimalFormat[i] = new BoboSimpleDecimalFormat(i);
    }
  }

  private final int _padLength;
  private final StringBuilder _builder;

  /**
   * @param padding the total number of digits for output.
   */
  private BoboSimpleDecimalFormat(int padding)
  {
    this._padLength = padding;
    this._builder = new StringBuilder(padding + 1);
  }
  private static long xx =0;
  public static BoboSimpleDecimalFormat getInstance(int padding)
  {
    if (xx < 10)
    {
      log.info("simpleFormat " + padding);
      xx++;
    }
    return _allBoboSimpleDecimalFormat[padding];
  }

  /**
   * <b>We assume that the total length is less than or equal to the padding length and do no check</b>
   * @param number
   * @return the formatted integer value with 0 padded in the front.
   */
  public String format(short number)
  {
    if (number>=0)
    {
      int i = _padLength - stringSizeOfShort(number);
      _builder.append(_paddings[i]).append(number);
      String ret = _builder.toString();
      _builder.setLength(0);
      return ret;
    } else
    {
      int i = _padLength - stringSizeOfLong(-number);
      _builder.append("-").append(_paddings[i]).append(-number);
      String ret = _builder.toString();
      _builder.setLength(0);
      return ret;
    }
  }

  /**
   * <b>We assume that the total length is less than or equal to the padding length and do no check</b>
   * @param number
   * @return the formatted integer value with 0 padded in the front.
   */
  public String format(int number)
  {
    if (number>=0)
    {
      int i = _padLength - stringSizeOfInt(number);
      _builder.append(_paddings[i]).append(number);
      String ret = _builder.toString();
      _builder.setLength(0);
      return ret;
    } else
    {
      int i = _padLength - stringSizeOfLong(-number);
      _builder.append("-").append(_paddings[i]).append(-number);
      String ret = _builder.toString();
      _builder.setLength(0);
      return ret;
    }
  }

  /**
   * <b>We assume that the total length is less than or equal to the padding length and do no check</b>
   * @param number
   * @return the formatted integer value with 0 padded in the front.
   */
  public String format(long number)
  {
    if (number>=0)
    {
      int i = _padLength - stringSizeOfLong(number);
      _builder.append(_paddings[i]).append(number);
      String ret = _builder.toString();
      _builder.setLength(0);
      return ret;
    } else
    {
      int i = _padLength - stringSizeOfLong(-number);
      _builder.append("-").append(_paddings[i]).append(-number);
      String ret = _builder.toString();
      _builder.setLength(0);
      return ret;
    }
  }
  // Requires positive x
  // this is what the java.lang method does.  Is binary search better?
  static final int stringSizeOfShort(short x) {
    if (x < 10L)
      return 1;
    if (x < 100L)
      return 2;
    if (x < 1000L)
      return 3;
    if (x < 10000L)
      return 4;
    if (x < 100000L)
      return 5;
    if (x < 1000000L)
      return 6;
    if (x < 10000000L)
      return 7;
    return 8;
  }

  // Requires positive x
  // this is what the java.lang method does.  Is binary search better?
  static final int stringSizeOfInt(int x) {
    if (x < 10L)
      return 1;
    if (x < 100L)
      return 2;
    if (x < 1000L)
      return 3;
    if (x < 10000L)
      return 4;
    if (x < 100000L)
      return 5;
    if (x < 1000000L)
      return 6;
    if (x < 10000000L)
      return 7;
    if (x < 100000000L)
      return 8;
    if (x < 1000000000L)
      return 9;
    if (x < 10000000000L)
      return 10;
    if (x < 100000000000L)
      return 11;
    return 12;
  }
  // Requires positive x
  // this is what the java.lang method does.  Is binary search better?
  static final int stringSizeOfLong(long x) {
    if (x < 10L)
      return 1;
    if (x < 100L)
      return 2;
    if (x < 1000L)
      return 3;
    if (x < 10000L)
      return 4;
    if (x < 100000L)
      return 5;
    if (x < 1000000L)
      return 6;
    if (x < 10000000L)
      return 7;
    if (x < 100000000L)
      return 8;
    if (x < 1000000000L)
      return 9;
    if (x < 10000000000L)
      return 10;
    if (x < 100000000000L)
      return 11;
    if (x < 1000000000000L)
      return 12;
    if (x < 10000000000000L)
      return 13;
    if (x < 100000000000000L)
      return 14;
    if (x < 1000000000000000L)
      return 15;
    if (x < 10000000000000000L)
      return 16;
    if (x < 100000000000000000L)
      return 17;
    if (x < 1000000000000000000L)
      return 18;
    return 19;
  }
}
