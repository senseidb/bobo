package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.chars.CharArrayList;

import java.util.Arrays;
import java.util.List;

public class TermCharList extends TermValueList<Character> {

  private char[] _elements = null;
	private static char parse(String s)
	{
		return s==null ? (char)0 : s.charAt(0);
	}
	
	public TermCharList() {
		super();
	}

	public TermCharList(int capacity) {
		super(capacity);
	}

	@Override
	public boolean add(String o) {
		return ((CharArrayList)_innerList).add(parse(o));
	}

	@Override
	protected List<?> buildPrimitiveList(int capacity) {
	  _type = Character.class;
		return  capacity>0 ? new CharArrayList(capacity) : new CharArrayList();
	}

  @Override
  public boolean containsWithType(Character val)
  {
    return Arrays.binarySearch(_elements, val)>=0;
  }

  public boolean containsWithType(char val)
  {
    return Arrays.binarySearch(_elements, val)>=0;
  }

  @Override
	public int indexOf(Object o)
  {
    char val;
    if (o instanceof String)
      val = parse((String)o);
    else
      val = (Character)o;
		char[] elements=((CharArrayList)_innerList).elements();
		return Arrays.binarySearch(elements, val);
	}

  @Override
  public int indexOfWithType(Character val)
  {
    return Arrays.binarySearch(_elements, val);
  }

  public int indexOfWithType(char val)
  {
    return Arrays.binarySearch(_elements, val);
  }

  @Override
	public void seal() {
		((CharArrayList)_innerList).trim();
		_elements = ((CharArrayList)_innerList).elements();
	}

	@Override
	public String format(Object o) {
		return String.valueOf(o);
	}
}
