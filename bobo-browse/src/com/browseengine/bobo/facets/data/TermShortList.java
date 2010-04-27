package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.browseengine.bobo.util.BoboSimpleDecimalFormat;

public class TermShortList extends TermNumberList<Short> {
  private static Logger                   log = Logger.getLogger(TermShortList.class);
  private boolean simpleFormat;
  private BoboSimpleDecimalFormat _simpleFormatter;
  private ArrayList<String> _innerTermList = new ArrayList<String>();
  private String zero = "0".intern();
  private short[] _elements = null;
	private static short parse(String s)
	{
		if (s==null || s.length() == 0)
		{
			return (short)0;
		}
		else
		{
			return Short.parseShort(s);
		}
	}

	public TermShortList() {
		super();
	}

	public TermShortList(String formatString) {
		super(formatString);
	}

	public TermShortList(int capacity, String formatString) {
		super(capacity, formatString);
	}

	@Override
	public boolean add(String o) {
    _innerTermList.add(o==null||o.length()==0?zero:o.intern());
		return ((ShortArrayList)_innerList).add(parse(o));
	}

  @Override
  public void clear() {
    super.clear();
    _innerTermList.clear();
  }

  @Override
  public String get(int index) {
    return _innerTermList.get(index);
  }

  @Override
  public Iterator<String> iterator() {
    final Iterator<String> iter=_innerTermList.iterator();
    
    return new Iterator<String>()
    {
      public final boolean hasNext() {
        return iter.hasNext();
      }

      public final String next() {
        return iter.next();
      }

      public final void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  @Override
	protected List<?> buildPrimitiveList(int capacity)
	{
    _type = Short.class;
		return  capacity>0 ? new ShortArrayList(capacity) : new ShortArrayList();
	}

	@Override
	public int indexOf(Object o) {
		short val=parse((String)o);
		short[] elements=((ShortArrayList)_innerList).elements();
		return Arrays.binarySearch(elements, val);
	}

  /* (non-Javadoc)
   * @see com.browseengine.bobo.facets.data.TermValueList#indexOfWithType(java.lang.Object)
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
	public void seal() {
		((ShortArrayList)_innerList).trim();
    _innerTermList.trimToSize();
    _elements = ((ShortArrayList)_innerList).elements();
	}

	@Override
	protected Object parseString(String o) {
		return parse(o);
	}

	@Override
	protected void setFormatString(String formatString)
  {
    if (formatString!=null && formatString.matches("0*"))
    {
      simpleFormat = true;
      _simpleFormatter = BoboSimpleDecimalFormat.getInstance(formatString.length());
    } else
    {
      simpleFormat = false;
      super.setFormatString(formatString);
    }
    zero = format(0).intern();
  }

  @Override
  public String format(final Object o) {
    if (!simpleFormat) return super.format(o);
    if (o instanceof Short) return format((Short) o); 
    if (o == null) return null;
    long number = 0;
    if (o instanceof String){
      number = parse((String)o);
    }
    return _simpleFormatter.format(number);
  }

  public String format(final Short o) {
    return _simpleFormatter.format(o);
  }

  @Override
  public boolean containsWithType(Short val)
  {
    return Arrays.binarySearch(_elements, val)>=0;
  }

  public boolean containsWithType(short val)
  {
    return Arrays.binarySearch(_elements, val)>=0;
  }
}
