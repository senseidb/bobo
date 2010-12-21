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
    int item = parse(o);
    if (sanity >= item) throw new RuntimeException("Values need to be added in ascending order. Previous value: " + sanity + " adding value: " + item);
    sanity = item;
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
    int val = parse((String) o);
    int[] elements = ((IntArrayList) _innerList).elements();
    return Arrays.binarySearch(elements, val);
  }

  public int indexOf(Integer val)
  {
    if (val != null)
      return Arrays.binarySearch(_elements, val);
    else
      return Arrays.binarySearch(_elements, 0); // turning null to 0 in parse
  }

  public int indexOf(int val)
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
  public int indexOfWithType(Integer val)
  {
    return Arrays.binarySearch(_elements, val);
  }

  public int indexOfWithType(int val)
  {
    return Arrays.binarySearch(_elements, val);
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
    return Arrays.binarySearch(_elements, val) >= 0;
  }
  
  @Override
  public boolean containsWithType(Integer val)
  {
    return Arrays.binarySearch(_elements, val) >= 0;
  }

  public boolean containsWithType(int val)
  {
    return Arrays.binarySearch(_elements, val) >= 0;
  }
}
