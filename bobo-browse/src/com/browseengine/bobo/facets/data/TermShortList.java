package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.shorts.ShortArrayList;

import java.util.Arrays;
import java.util.List;

public class TermShortList extends TermNumberList {
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
}
