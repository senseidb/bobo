package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.browseengine.bobo.util.BoboSimpleDecimalFormat;

public class TermIntList extends TermNumberList<Integer> {
  private static Logger                   log = Logger.getLogger(TermIntList.class);
  private boolean simpleFormat;
  private ThreadLocal<BoboSimpleDecimalFormat> _simpleFormatter;
  private ArrayList<String> _innerTermList = new ArrayList<String>();
  private String zero = "0".intern();
  private int[] _elements = null;
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
	public int getPrimitiveValue(int index)
	{
	  if (index<_elements.length)
	    return _elements[index];
	  else return -1;
	}
	@Override
	public Iterator<String> iterator()
	{
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
          _type = Integer.class;
         return capacity>0 ? new IntArrayList(capacity) : new IntArrayList();
	}
	
	@Override
	public int indexOf(Object o) {
		int val=parse((String)o);
		int[] elements=((IntArrayList)_innerList).elements();
		return Arrays.binarySearch(elements, val);
	}

  /* (non-Javadoc)
   * @see com.browseengine.bobo.facets.data.TermValueList#indexOfWithType(java.lang.Object)
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
	public void seal() {
		((IntArrayList)_innerList).trim();
		_innerTermList.trimToSize();
		_elements = ((IntArrayList)_innerList).elements();
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
      _formatString = formatString;
      _simpleFormatter = new ThreadLocal<BoboSimpleDecimalFormat>(){
        protected BoboSimpleDecimalFormat initialValue() {
          if (_formatString!=null){
            return BoboSimpleDecimalFormat.getInstance(_formatString.length());
          }
          else{
            return null;
          }
          
        }
      };
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
    return _simpleFormatter.get().format(number);
  }

  public String format(final Integer o) {
    return _simpleFormatter.get().format(o);
  }

  public String format(final int o) {
    return _simpleFormatter.get().format(o);
  }

  @Override
  public boolean containsWithType(Integer val)
  {
    return Arrays.binarySearch(_elements, val)>=0;
  }

  public boolean containsWithType(int val)
  {
    return Arrays.binarySearch(_elements, val)>=0;
  }
}
