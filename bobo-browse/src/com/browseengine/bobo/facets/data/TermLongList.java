package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Arrays;
import java.util.List;

import com.browseengine.bobo.util.BoboSimpleDecimalFormat;

public class TermLongList extends TermNumberList {
  private boolean simpleFormat;
  private BoboSimpleDecimalFormat _simpleFormatter;
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
		return ((LongArrayList)_innerList).add(parse(o));
	}

	@Override
	protected List<?> buildPrimitiveList(int capacity) {
		return  capacity>0 ? new LongArrayList(capacity) : new LongArrayList();
	}

	@Override
	public int indexOf(Object o) {
		long val=parse((String)o);
		long[] elements=((LongArrayList)_innerList).elements();
		return Arrays.binarySearch(elements, val);
	}

	@Override
	public void seal() {
		((LongArrayList)_innerList).trim();
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
}
