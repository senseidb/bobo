package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.Arrays;
import java.util.List;

public class TermLongList extends TermNumberList {
	private static long parse(String s)
	{
		if (s==null || s.length() == 0)
		{
			return 0L;
		}
		else
		{
			return Long.parseLong(s);
		}
	}
	
	public TermLongList() {
		super();
	}

	public TermLongList(int capacity, String formatString) {
		super(capacity, formatString);
	}

	public TermLongList(String formatString) {
		super(formatString);
	}
	
	@Override
	public boolean add(String o) {
		return ((LongArrayList)_innerList).add(parse(o));
	}

	@Override
	protected List<?> buildPrimitiveList(int capacity) {
		return  capacity>0 ? new LongArrayList(capacity) : new LongArrayList();
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
	
	@Override
	protected Object parseString(String o) {
		return parse(o);
	}

}
