package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Internal data are stored in a long[] with values generated from {@link Date#getTime()}
 */
public class TermDateList extends TermValueList {

	private ThreadLocal<SimpleDateFormat> _formatter = null;
	private String _formatString;
	
	public TermDateList(String formatString)
	{
		super();
		setFormatString(formatString);
	}
	
	public TermDateList(int capacity,String formatString)
	{
		super(capacity);
		setFormatString(formatString);
	}
	
	public String getFormatString()
	{
		return _formatString;
	}
	
	private void setFormatString(final String formatString)
	{
		_formatString=formatString;
		_formatter = new ThreadLocal<SimpleDateFormat>() {
		      protected SimpleDateFormat initialValue() {
		        if (formatString!=null){
		          return new SimpleDateFormat(formatString);
		        }
		        else{
		          return null;
		        }
		        
		      }   
		    };
	}
	
	private long parse(String o)
	{
		if (o==null || o.length() == 0)
		{
			return 0L;
		}
		else
		{
			try
			{
			  return _formatter.get().parse(o).getTime();
			}
			catch(ParseException pe)
			{
				throw new RuntimeException(pe.getMessage(),pe);
			}
		}
		
		
	}
	
	@Override
	public boolean add(String o) {
		return ((LongArrayList)_innerList).add(parse(o));
	}

	@Override
	protected List<?> buildPrimitiveList(int capacity) {
		return capacity>0 ? new LongArrayList(capacity) : new LongArrayList();
	}

	@Override
	public String format(Object o) {
		Long val;
		if (o instanceof String){
			val = parse((String)o);
		}
		else{
			val = (Long)o;
		}
		if (_formatter == null)
		{
			return String.valueOf(o);
		}
		else
		{
			SimpleDateFormat formatter=_formatter.get();
			if (formatter==null) return String.valueOf(o);
			return _formatter.get().format(new Date(val.longValue()));
		}
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

}
