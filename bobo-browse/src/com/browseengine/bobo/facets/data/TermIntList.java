package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;
import java.util.List;

public class TermIntList extends TermNumberList {
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
}
