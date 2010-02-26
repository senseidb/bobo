package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.chars.CharArrayList;

import java.util.Arrays;
import java.util.List;

public class TermCharList extends TermValueList {

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
		return  capacity>0 ? new CharArrayList(capacity) : new CharArrayList();
	}

	@Override
	public int indexOf(Object o) {
		char val=parse((String)o);
		char[] elements=((CharArrayList)_innerList).elements();
		return Arrays.binarySearch(elements, val);
	}

	@Override
	public void seal() {
		((CharArrayList)_innerList).trim();
	}

	@Override
	public String format(Object o) {
		return String.valueOf(o);
	}
}
