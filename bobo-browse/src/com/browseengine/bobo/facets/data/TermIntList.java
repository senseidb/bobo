package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import com.browseengine.bobo.util.BoboSimpleDecimalFormat;

public class TermIntList extends TermNumberList {
  private boolean simpleFormat;
  private BoboSimpleDecimalFormat _simpleFormatter;
	private static int parse(String s)
	{
		if (s==null || s.length() == 0)
		{
			return 0;
		}
		else
		{
			return Integer.parseInt(s);
		}
	}
	
	public TermIntList() {
		super();
	}

	public TermIntList(int capacity, String formatString) {
		super(capacity, formatString);
	}

	public TermIntList(String formatString) {
		super(formatString);
	}

	@Override
	public boolean add(String o) {
		return ((IntArrayList)_innerList).add(parse(o));
	}

	@Override
	protected List<?> buildPrimitiveList(int capacity) {
		return  capacity>0 ? new IntArrayList(capacity) : new IntArrayList();
	}
	
	@Override
	public int indexOf(Object o) {
		int val=parse((String)o);
		int[] elements=((IntArrayList)_innerList).elements();
		return Arrays.binarySearch(elements, val);
	}

	@Override
	public void seal() {
		((IntArrayList)_innerList).trim();
	}

	@Override
	protected Object parseString(String o) {
		return parse(o);
	}

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
    if (o instanceof Integer) return format((Integer) o); 
    if (o == null) return null;
    long number = 0;
    if (o instanceof String){
      number = parse((String)o);
    }
    return _simpleFormatter.format(number);
  }

  public String format(final Integer o) {
    return _simpleFormatter.format(o);
  }
}
