package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class TermFloatList extends TermNumberList<Float>
{

  private float[] _elements = null;

  private static float parse(String s)
  {
    if (s == null || s.length() == 0)
    {
      return 0.0f;
    } else
    {
      return Float.parseFloat(s);
    }
  }

  public TermFloatList()
  {
    super();
  }

  public TermFloatList(String formatString)
  {
    super(formatString);
  }

  public TermFloatList(int capacity, String formatString)
  {
    super(capacity, formatString);
  }

  @Override
  public boolean add(String o)
  {
    return ((FloatArrayList) _innerList).add(parse(o));
  }

  @Override
  protected List<?> buildPrimitiveList(int capacity)
  {
    _type = Float.class;
    return capacity > 0 ? new FloatArrayList(capacity) : new FloatArrayList();
  }

  @Override
  public String get(int index)
  {
    DecimalFormat formatter = _formatter.get();
    if (formatter == null)
      return String.valueOf(_elements[index]);
    return formatter.format(_elements[index]);
  }

  public float getPrimitiveValue(int index)
  {
    if (index < _elements.length)
      return _elements[index];
    else
      return -1;
  }

  @Override
  public int indexOf(Object o)
  {
    float val = parse((String) o);
    float[] elements = ((FloatArrayList) _innerList).elements();
    return Arrays.binarySearch(elements, val);
  }

  public int indexOf(float o)
  {
    return Arrays.binarySearch(_elements, o);
  }

  @Override
  public void seal()
  {
    ((FloatArrayList) _innerList).trim();
    _elements = ((FloatArrayList) _innerList).elements();
  }

  @Override
  protected Object parseString(String o)
  {
    return parse(o);
  }

  public boolean contains(float val)
  {
    return Arrays.binarySearch(_elements, val) >= 0;
  }

  @Override
  public boolean containsWithType(Float val)
  {
    return Arrays.binarySearch(_elements, val) >= 0;
  }

  public boolean containsWithType(float val)
  {
    return Arrays.binarySearch(_elements, val) >= 0;
  }

  @Override
  public int indexOfWithType(Float o)
  {
    return Arrays.binarySearch(_elements, o);
  }

  public int indexOfWithType(float o)
  {
    return Arrays.binarySearch(_elements, o);
  }
}
