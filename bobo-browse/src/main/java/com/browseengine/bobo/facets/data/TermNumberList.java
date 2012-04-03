package com.browseengine.bobo.facets.data;

import java.text.DecimalFormat;

public abstract class TermNumberList<T extends Number> extends TermValueList<T>
{

  private static final String DEFAULT_FORMATTING_STRING = "0000000000";
  protected ThreadLocal<DecimalFormat> _formatter = null;
  protected String _formatString = null;

  protected TermNumberList()
  {
    super();
    setFormatString(DEFAULT_FORMATTING_STRING);
  }

  protected TermNumberList(String formatString)
  {
    super();
    setFormatString(formatString);
  }

  protected TermNumberList(int capacity, String formatString)
  {
    super(capacity);
    setFormatString(formatString);
  }

  protected void setFormatString(String formatString)
  {
    _formatString = formatString;
    _formatter = new ThreadLocal<DecimalFormat>()
    {
      protected DecimalFormat initialValue()
      {
        if (_formatString != null)
        {
          return new DecimalFormat(_formatString);
        } else
        {
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
  public abstract double getDoubleValue(int index);
  @Override
  public String format(Object o)
  {
    if (o == null)
      return null;
    if (o instanceof String)
    {
      o = parseString((String) o);
    }
    if (_formatter == null)
    {
      return String.valueOf(o);
    } else
    {
      DecimalFormat formatter = _formatter.get();
      if (formatter == null)
        return String.valueOf(o);
      return formatter.format(o);
    }
  }
}
