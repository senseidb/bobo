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
  public static final short VALUE_MISSING = Short.MIN_VALUE;
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
      return VALUE_MISSING;
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
      short val;
      if (o instanceof String)
        val = parse((String) o);
      else
        val = (Short)o;
      return Arrays.binarySearch(_elements, 1, _elements.length, val);
    } else
    {
      short val;
      if (o instanceof String)
        val = parse((String) o);
      else
        val = (Short)o;
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
    int negativeIndexCheck = withDummy ? 1 : 0;
    //reverse negative elements, because string order and numeric orders are completely opposite
    if (_elements.length > negativeIndexCheck && _elements[negativeIndexCheck] < 0) {
      int endPosition = indexOfWithType((short) 0);
      if (endPosition < 0) {
        endPosition = -1 *endPosition - 1;
      }
      short tmp;
      for (int i = 0;  i < (endPosition - negativeIndexCheck) / 2; i++) {
         tmp = _elements[i + negativeIndexCheck];
         _elements[i + negativeIndexCheck] = _elements[endPosition -i -1];
         _elements[endPosition -i -1] = tmp;
      }
    }
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

  public short[] getElements() {
    return _elements;
  }

  @Override
  public double getDoubleValue(int index) {    
    return _elements[index];
  }
  
}
