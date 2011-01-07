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
    long item = parse(o);
    if (sanity >= item) throw new RuntimeException("Values need to be added in ascending order and we only support non-negative numbers. Previous value: " + sanity + " adding value: " + item);
    sanity = item;
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
    long val = parse(String.valueOf(o));
    long[] elements = ((LongArrayList) _innerList).elements();
    return Arrays.binarySearch(elements, val);
  }

  public int indexOf(Long val)
  {
    if (val != null)
      return Arrays.binarySearch(_elements, val);
    else
      return Arrays.binarySearch(_elements, 0); // turning null to 0 in parse
  }

  public int indexOf(long val)
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
  public int indexOfWithType(Long val)
  {
    return Arrays.binarySearch(_elements, val);
  }

  public int indexOfWithType(long val)
  {
    return Arrays.binarySearch(_elements, val);
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
    return Arrays.binarySearch(_elements, val) >= 0;
  }
  
  @Override
  public boolean containsWithType(Long val)
  {
    return Arrays.binarySearch(_elements, val) >= 0;
  }

  public boolean containsWithType(long val)
  {
    return Arrays.binarySearch(_elements, val) >= 0;
  }
}
