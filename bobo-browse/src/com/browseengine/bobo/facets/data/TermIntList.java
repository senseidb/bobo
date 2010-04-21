package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.browseengine.bobo.util.BoboSimpleDecimalFormat;

public class TermIntList extends TermNumberList {
  private static Logger                   log = Logger.getLogger(TermIntList.class);
  private boolean simpleFormat;
  private BoboSimpleDecimalFormat _simpleFormatter;
  private ArrayList<String> _innerTermList = new ArrayList<String>();
  private String zero = "0".intern();
  private IntArrayList _innerValList;
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
    _innerTermList.add(o==null||o.length()==0?zero:o.intern());
		return ((IntArrayList)_innerList).add(parse(o));
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
	public Object getRawValue(int index){
		return _innerValList.get(index);
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
	protected List<?> buildPrimitiveList(int capacity) {
	  _innerValList =  capacity>0 ? new IntArrayList(capacity) : new IntArrayList();
	  return _innerValList;
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
		_innerTermList.trimToSize();
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
    zero = format(0).intern();
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
