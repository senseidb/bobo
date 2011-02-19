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
  private short sanity = -1;
  private boolean withDummy = true;

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
    if (_innerList.size() == 0 && o!=null) withDummy = false; // the first value added is not null
    short item = parse(o);
    if (sanity >= item) throw new RuntimeException("Values need to be added in ascending order and we only support non-negative numbers. Previous value: " + sanity + " adding value: " + item);
    if (_innerList.size() > 0 || !withDummy) sanity = item;
    return ((ShortArrayList) _innerList).add(item);
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
    if (withDummy)
    {
      if (o==null) return -1;
      short val = parse(String.valueOf(o));
      return Arrays.binarySearch(_elements, 1, _elements.length, val);
    } else
    {
      short val = parse(String.valueOf(o));
      return Arrays.binarySearch(_elements, val);
    }
  }
  public int indexOf(Short val)
  {
    if (withDummy)
    {
      if (val==null) return -1;
      return Arrays.binarySearch(_elements, 1, _elements.length, val.shortValue());
    } else
    {
      return Arrays.binarySearch(_elements, val.shortValue());
    }
  }

  public int indexOf(short val)
  {
    if (withDummy)
      return Arrays.binarySearch(_elements, 1, _elements.length, val);
    else
      return Arrays.binarySearch(_elements, val);
  }

  @Override
  public int indexOfWithType(Short val)
  {
    if (withDummy)
    {
      if (val == null) return -1;
      return Arrays.binarySearch(_elements, 1, _elements.length, val.shortValue());
    } else
    {
      return Arrays.binarySearch(_elements, val.shortValue());
    }
  }

  public int indexOfWithType(short val)
  {
    if (withDummy)
    {
      return Arrays.binarySearch(_elements, 1, _elements.length, val);
    } else
    {
      return Arrays.binarySearch(_elements, val);
    }
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
    if (withDummy)
      return Arrays.binarySearch(_elements,1, _elements.length, val) >= 0;
    else
      return Arrays.binarySearch(_elements, val) >= 0;
  }
  
  @Override
  public boolean containsWithType(Short val)
  {
    if (withDummy)
    {
      if (val == null) return false;
      return Arrays.binarySearch(_elements,1, _elements.length, val.shortValue()) >= 0;
    } else
    {
      return Arrays.binarySearch(_elements, val.shortValue()) >= 0;
    }
  }

  public boolean containsWithType(short val)
  {
    if (withDummy)
      return Arrays.binarySearch(_elements,1, _elements.length, val) >= 0;
    else
      return Arrays.binarySearch(_elements, val) >= 0;
  }
}
