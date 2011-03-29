package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

public class TermIntList extends TermNumberList<Integer>
{
  private static Logger log = Logger.getLogger(TermIntList.class);
  private int[] _elements = null;
  private int sanity = -1;
  private boolean withDummy = true;

  private static int parse(String s)
  {
    if (s == null || s.length() == 0)
    {
      return 0;
    } else
    {
      return Integer.parseInt(s);
    }
  }

  public TermIntList()
  {
    super();
  }

  public TermIntList(int capacity, String formatString)
  {
    super(capacity, formatString);
  }

  public TermIntList(String formatString)
  {
    super(formatString);
  }

  @Override
  public boolean add(String o)
  {
    if (_innerList.size() == 0 && o!=null) withDummy = false; // the first value added is not null
    int item = parse(o);
    if (sanity >= item) throw new RuntimeException("Values need to be added in ascending order and we only support non-negative numbers. Previous value: " + sanity + " adding value: " + item);
    if (_innerList.size() > 0 || !withDummy) sanity = item;
    return ((IntArrayList) _innerList).add(item);
  }

  @Override
  protected List<?> buildPrimitiveList(int capacity)
  {
    _type = Integer.class;
    return capacity > 0 ? new IntArrayList(capacity) : new IntArrayList();
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

  public int getPrimitiveValue(int index)
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
      int val = parse(String.valueOf(o));
      return Arrays.binarySearch(_elements, 1, _elements.length, val);
    } else
    {
      int val = parse(String.valueOf(o));
      return Arrays.binarySearch(_elements, val);
    }
  }

  public int indexOf(Integer value)
  {
    if (withDummy)
    {
      if (value==null) return -1;
      return Arrays.binarySearch(_elements, 1, _elements.length, value.intValue());
    } else
    {
      return Arrays.binarySearch(_elements, value.intValue());
    }
  }

  public int indexOf(int val)
  {
    if (withDummy)
      return Arrays.binarySearch(_elements, 1, _elements.length, val);
    else
      return Arrays.binarySearch(_elements, val);
  }

  @Override
  public int indexOfWithOffset(Object value, int offset)
  {
    if (withDummy)
    {
      if (value == null || offset >= _elements.length)
        return -1;
      int val = parse(String.valueOf(value));
      return Arrays.binarySearch(_elements, offset, _elements.length, val);
    }
    else
    {
      int val = parse(String.valueOf(value));
      return Arrays.binarySearch(_elements, offset, _elements.length, val);
    }
  }

  public int indexOfWithOffset(Integer value, int offset)
  {
    if (withDummy)
    {
      if (value==null || offset >= _elements.length)
        return -1;
      return Arrays.binarySearch(_elements, offset, _elements.length, value.intValue());
    }
    else
    {
      return Arrays.binarySearch(_elements, offset, _elements.length, value.intValue());
    }
  }

  public int indexOfWithOffset(int value, int offset)
  {
    if (withDummy)
    {
      if (offset >= _elements.length)
        return -1;
      return Arrays.binarySearch(_elements, offset, _elements.length, value);
    }
    else
    {
      return Arrays.binarySearch(_elements, offset, _elements.length, value);
    }
  }

  @Override
  public int indexOfWithType(Integer val)
  {
    if (withDummy)
    {
      if (val == null) return -1;
      return Arrays.binarySearch(_elements, 1, _elements.length, val.intValue());
    } else
    {
      return Arrays.binarySearch(_elements, val.intValue());
    }
  }

  public int indexOfWithType(int val)
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
    ((IntArrayList) _innerList).trim();
    _elements = ((IntArrayList) _innerList).elements();
  }

  @Override
  protected Object parseString(String o)
  {
    return parse(o);
  }

  public boolean contains(int val)
  {
    if (withDummy)
      return Arrays.binarySearch(_elements,1, _elements.length, val) >= 0;
    else
      return Arrays.binarySearch(_elements, val) >= 0;
  }
  
  @Override
  public boolean containsWithType(Integer val)
  {
    if (withDummy)
    {
      if (val == null) return false;
      return Arrays.binarySearch(_elements,1, _elements.length, val.intValue()) >= 0;
    } else
    {
      return Arrays.binarySearch(_elements, val.intValue()) >= 0;
    }
  }

  public boolean containsWithType(int val)
  {
    if (withDummy)
      return Arrays.binarySearch(_elements,1, _elements.length, val) >= 0;
    else
      return Arrays.binarySearch(_elements, val) >= 0;
  }
}
