package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

public class TermLongList extends TermNumberList<Long>
{
  private static Logger log = Logger.getLogger(TermLongList.class);
  protected long[] _elements = null;
  private long sanity = -1;
  private boolean withDummy = true;

  protected long parse(String s)
  {
    if (s == null || s.length() == 0)
    {
      return 0L;
    } else
    {
      return Long.parseLong(s);
    }
  }

  public TermLongList()
  {
    super();
  }

  public TermLongList(int capacity, String formatString)
  {
    super(capacity, formatString);
  }

  public TermLongList(String formatString)
  {
    super(formatString);
  }

  @Override
  public boolean add(String o)
  {
    if (_innerList.size() == 0 && o!=null) withDummy = false; // the first value added is not null
    long item = parse(o);
    if (sanity >= item) throw new RuntimeException("Values need to be added in ascending order and we only support non-negative numbers. Previous value: " + sanity + " adding value: " + item);
    if (_innerList.size() > 0 || !withDummy) sanity = item;
    return ((LongArrayList) _innerList).add(item);
  }

  @Override
  protected List<?> buildPrimitiveList(int capacity)
  {
    _type = Long.class;
    return capacity > 0 ? new LongArrayList(capacity) : new LongArrayList();
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

  public long getPrimitiveValue(int index)
  {
    if (index < _elements.length)
      return _elements[index];
    else
      return -1;
  }

  @Override
  public int indexOf(Object o)
  {
    if (withDummy)
    {
      if (o==null) return -1;
      long val = parse(String.valueOf(o));
      return Arrays.binarySearch(_elements, 1, _elements.length, val);
    } else
    {
      long val = parse(String.valueOf(o));
      return Arrays.binarySearch(_elements, val);
    }
  }

  public int indexOf(Long value)
  {
    if (withDummy)
    {
      if (value==null) return -1;
      return Arrays.binarySearch(_elements, 1, _elements.length, value.longValue());
    } else
    {
      return Arrays.binarySearch(_elements, value.longValue());
    }
  }

  public int indexOf(long val)
  {
    if (withDummy)
      return Arrays.binarySearch(_elements, 1, _elements.length, val);
    else
      return Arrays.binarySearch(_elements, val);
  }

  @Override
  public int indexOfWithType(Long val)
  {
    if (withDummy)
    {
      if (val == null) return -1;
      return Arrays.binarySearch(_elements, 1, _elements.length, val.longValue());
    } else
    {
      return Arrays.binarySearch(_elements, val.longValue());
    }
  }

  public int indexOfWithType(long val)
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
    ((LongArrayList) _innerList).trim();
    _elements = ((LongArrayList) _innerList).elements();
  }

  @Override
  protected Object parseString(String o)
  {
    return parse(o);
  }

  public boolean contains(long val)
  {
    if (withDummy)
      return Arrays.binarySearch(_elements,1, _elements.length, val) >= 0;
    else
      return Arrays.binarySearch(_elements, val) >= 0;
  }
  
  @Override
  public boolean containsWithType(Long val)
  {
    if (withDummy)
    {
      if (val == null) return false;
      return Arrays.binarySearch(_elements,1, _elements.length, val.longValue()) >= 0;
    } else
    {
      return Arrays.binarySearch(_elements, val.longValue()) >= 0;
    }
  }

  public boolean containsWithType(long val)
  {
    if (withDummy)
      return Arrays.binarySearch(_elements,1, _elements.length, val) >= 0;
    else
      return Arrays.binarySearch(_elements, val) >= 0;
  }
}
