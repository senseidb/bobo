package com.browseengine.bobo.facets.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Internal data are stored in a long[] with values generated from {@link Date#getTime()}
 */
public class TermDateList extends TermLongList {
	private ThreadLocal<SimpleDateFormat> _dateFormatter = null;
	
	public TermDateList(String formatString)
	{
		super();
		setFormatString(formatString);
	}
	
	public TermDateList(int capacity,String formatString)
	{
		super(capacity,formatString);
		setFormatString(formatString);
	}
	
	public String getFormatString()
	{
		return _formatString;
	}
	
	protected void setFormatString(final String formatString)
	{
		_formatString=formatString;
		_dateFormatter = new ThreadLocal<SimpleDateFormat>() {
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
	
	@Override
	protected long parse(String o)
	{
		if (o==null || o.length() == 0)
		{
			return 0L;
		}
		else
		{
			try
			{
			  return _dateFormatter.get().parse(o).getTime();
			}
			catch(ParseException pe)
			{
				throw new RuntimeException(pe.getMessage(),pe);
			}
		}

	}

	@Override
	  public String get(int index)
	  {
		SimpleDateFormat formatter = _dateFormatter.get();
	    if (formatter == null)
	      return String.valueOf(_elements[index]);
	    return formatter.format(_elements[index]);
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
			SimpleDateFormat formatter=_dateFormatter.get();
			if (formatter==null) return String.valueOf(o);
			return _formatter.get().format(new Date(val.longValue()));
		}
	}

}
