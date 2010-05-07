package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

public class TermShortList extends TermNumberList<Short>
{
  private static Logger log = Logger.getLogger(TermShortList.class);
  private short[] _elements = null;

  private static short parse(String s)
  {
    if (s == null || s.length() == 0)
    {
      return (short) 0;
    } else
    {
      return Short.parseShort(s);
    }
  }

  public TermShortList()
  {
    super();
  }

  public TermShortList(String formatString)
  {
    super(formatString);
  }

  public TermShortList(int capacity, String formatString)
  {
    super(capacity, formatString);
  }

  @Override
  public boolean add(String o)
  {
    return ((ShortArrayList) _innerList).add(parse(o));
  }

  @Override
  public void clear()
  {
    super.clear();
  }

  @Override
  public String get(int index)
  {
    DecimalFormat formatter = _formatter.get();
    if (formatter == null)
      return String.valueOf(_elements[index]);
    return formatter.format(_elements[index]);
  }

  public short getPrimitiveValue(int index)
  {
    if (index < _elements.length)
      return _elements[index];
    else
      return -1;
  }

  @Override
  protected List<?> buildPrimitiveList(int capacity)
  {
    _type = Short.class;
    return capacity > 0 ? new ShortArrayList(capacity) : new ShortArrayList();
  }

  @Override
  public int indexOf(Object o)
  {
    short val = parse((String) o);
    short[] elements = ((ShortArrayList) _innerList).elements();
    return Arrays.binarySearch(elements, val);
  }
  public int indexOf(Short val)
  {
    if (val!=null)
      return Arrays.binarySearch(_elements, val);
    else
      return Arrays.binarySearch(_elements, (short)0); // turning null to 0 in parse
  }

  public int indexOf(short val)
  {
    return Arrays.binarySearch(_elements, val);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.browseengine.bobo.facets.data.TermValueList#indexOfWithType(java.lang
   * .Object)
   */
  @Override
  public int indexOfWithType(Short val)
  {
    return Arrays.binarySearch(_elements, val);
  }

  public int indexOfWithType(short val)
  {
    return Arrays.binarySearch(_elements, val);
  }

  @Override
  public void seal()
  {
    ((ShortArrayList) _innerList).trim();
    _elements = ((ShortArrayList) _innerList).elements();
  }

  @Override
  protected Object parseString(String o)
  {
    return parse(o);
  }

  public boolean contains(short val)
  {
    return Arrays.binarySearch(_elements, val) >= 0;
  }
  
  @Override
  public boolean containsWithType(Short val)
  {
    return Arrays.binarySearch(_elements, val) >= 0;
  }

  public boolean containsWithType(short val)
  {
    return Arrays.binarySearch(_elements, val) >= 0;
  }
}
