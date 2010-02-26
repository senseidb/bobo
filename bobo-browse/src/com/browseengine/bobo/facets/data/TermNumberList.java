package com.browseengine.bobo.facets.data;

import java.text.DecimalFormat;


public abstract class TermNumberList extends TermValueList {

	private ThreadLocal<DecimalFormat> _formatter = null;
	protected String _formatString = null;
	
	protected TermNumberList()
	{
		super();
	}
	
	protected TermNumberList(String formatString)
	{
		super();
		setFormatString(formatString);
	}
	
	protected TermNumberList(int capacity,String formatString)
	{
		super(capacity);
		setFormatString(formatString);
	}
	
	private void setFormatString(String formatString)
	{
		_formatString=formatString;
		_formatter = new ThreadLocal<DecimalFormat>() {
		      protected DecimalFormat initialValue() {
		        if (_formatString!=null){
		          return new DecimalFormat(_formatString);
		        }
		        else{
		          return null;
		        }
		        
		      }   
		    };
	}
	
	public String getFormatString()
	{
		return _formatString;
	}
	
	protected abstract Object parseString(String o);

	@Override
	public String format(Object o) {
		if (o == null) return null;
		if (o instanceof String){
			o = parseString((String)o);
		}
		if (_formatter == null)
		{
			return String.valueOf(o);
		}
		else
		{
			DecimalFormat formatter=_formatter.get();
			if (formatter==null) return String.valueOf(o);
			return _formatter.get().format(o);
		}
	}
}
