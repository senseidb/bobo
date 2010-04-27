package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.browseengine.bobo.util.BoboSimpleDecimalFormat;

public class TermLongList extends TermNumberList<Long> {
  private static Logger                   log = Logger.getLogger(TermLongList.class);
  private boolean simpleFormat;
  private BoboSimpleDecimalFormat _simpleFormatter;
  private ArrayList<String> _innerTermList = new ArrayList<String>();
  private String zero = "0".intern();
  private long[] _elements = null;
	private static long parse(String s)
	{
		if (s==null || s.length() == 0)
		{
			return 0L;
		}
		else
		{
			return Long.parseLong(s);
		}
	}
	
	public TermLongList() {
		super();
	}

	public TermLongList(int capacity, String formatString) {
		super(capacity, formatString);
	}

	public TermLongList(String formatString) {
		super(formatString);
	}
	
	@Override
	public boolean add(String o) {
    _innerTermList.add(o==null||o.length()==0?zero:o.intern());
		return ((LongArrayList)_innerList).add(parse(o));
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
	  _type = Long.class;
		return  capacity>0 ? new LongArrayList(capacity) : new LongArrayList();
	}

	@Override
	public int indexOf(Object o) {
		long val=parse((String)o);
		long[] elements=((LongArrayList)_innerList).elements();
		return Arrays.binarySearch(elements, val);
	}

  /* (non-Javadoc)
   * @see com.browseengine.bobo.facets.data.TermValueList#indexOfWithType(java.lang.Object)
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
	public void seal() {
		((LongArrayList)_innerList).trim();
    _innerTermList.trimToSize();
    _elements = ((LongArrayList)_innerList).elements();
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
    if (o instanceof Long) return format((Long) o); 
    if (o == null) return null;
    long number = 0;
    if (o instanceof String){
      number = parse((String)o);
    }
    return _simpleFormatter.format(number);
  }

  public String format(final Long o) {
    return _simpleFormatter.format(o);
  }

  @Override
  public boolean containsWithType(Long val)
  {
    return Arrays.binarySearch(_elements, val)>=0;
  }

  public boolean containsWithType(long val)
  {
    return Arrays.binarySearch(_elements, val)>=0;
  }
}
