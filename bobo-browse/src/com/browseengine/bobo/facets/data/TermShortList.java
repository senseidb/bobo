package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import java.util.Arrays;
import java.util.List;

import com.browseengine.bobo.util.BoboSimpleDecimalFormat;

public class TermShortList extends TermNumberList {
  private boolean simpleFormat;
  private BoboSimpleDecimalFormat _simpleFormatter;
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
		return ((ShortArrayList)_innerList).add(parse(o));
	}

	@Override
	protected List<?> buildPrimitiveList(int capacity) {
		return  capacity>0 ? new ShortArrayList(capacity) : new ShortArrayList();
	}

	@Override
	public int indexOf(Object o) {
		short val=parse((String)o);
		short[] elements=((ShortArrayList)_innerList).elements();
		return Arrays.binarySearch(elements, val);
	}

	@Override
	public void seal() {
		((ShortArrayList)_innerList).trim();
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
}
