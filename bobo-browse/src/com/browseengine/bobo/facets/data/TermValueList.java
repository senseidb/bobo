package com.browseengine.bobo.facets.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *  This class behaves as List<String> with a few extensions:
 *  <ul>
 *   <li> Semi-immutable, e.g. once added, cannot be removed. </li>
 *   <li> Assumes sequence of values added are in sorted order </li>
 *   <li> {@link #indexOf(Object)} return value conforms to the contract of {@link Arrays#binarySearch(Object[], Object)}</li>
 *   <li> {@link #seal()} is introduce to trim the List size, similar to {@link ArrayList#trimToSize()}, once it is called, no add should be performed.</li>
 *   </u>
 */
public abstract class TermValueList implements List<String>{
	
	protected abstract List<?> buildPrimitiveList(int capacity);
	public abstract String format(Object o);
	public abstract void seal();
	
	protected List<?> _innerList;
	
	protected TermValueList()
	{
		_innerList=buildPrimitiveList(-1);
	}
	
	protected TermValueList(int capacity)
	{
		_innerList=buildPrimitiveList(capacity);
	}
	
	public List<?> getInnerList(){
		return _innerList;
	}
	
	abstract public boolean add(String o);

	public void add(int index, String element)
	{
		throw new IllegalStateException("not supported");
	}

	public boolean addAll(Collection<? extends String> c)
	{
		throw new IllegalStateException("not supported");
	}

	public boolean addAll(int index, Collection<? extends String> c)
	{
		throw new IllegalStateException("not supported");
	}

	public void clear() {
		_innerList.clear();
	}

	public boolean contains(Object o){
		return indexOf(o)>=0;
	}

	public boolean containsAll(Collection<?> c)
	{
		throw new IllegalStateException("not supported");
	}

	public String get(int index) {
		return format(_innerList.get(index));
	}
	
	public Object getRawValue(int index){
		return _innerList.get(index);
	}

	abstract public int indexOf(Object o);

	public boolean isEmpty() {
		return _innerList.isEmpty();
	}

	public Iterator<String> iterator() {
		final Iterator<?> iter=_innerList.iterator();
		
		return new Iterator<String>()
		{
			public boolean hasNext() {
				return iter.hasNext();
			}

			public String next() {
				return format(iter.next());
			}

			public void remove() {
				iter.remove();
			}
		};
	}

	public int lastIndexOf(Object o)
	{
		return indexOf(o);
	}

	public ListIterator<String> listIterator()
	{
		throw new IllegalStateException("not supported");
	}

	public ListIterator<String> listIterator(int index)
	{
		throw new IllegalStateException("not supported");
	}

	public boolean remove(Object o)
	{
		throw new IllegalStateException("not supported");
	}

	public String remove(int index) {
		throw new IllegalStateException("not supported");
	}

	public boolean removeAll(Collection<?> c)
	{
		throw new IllegalStateException("not supported");
	}

	public boolean retainAll(Collection<?> c)
	{
		throw new IllegalStateException("not supported");
	}

	public String set(int index, String element)
	{
		throw new IllegalStateException("not supported");
	}

	public int size() {
		return _innerList.size();
	}

	public List<String> subList(int fromIndex, int toIndex) {
		throw new IllegalStateException("not supported");
	}

	public Object[] toArray() {
		Object[] array=_innerList.toArray();
		Object[] retArray=new Object[array.length];
		for (int i=0;i<array.length;++i)
		{
			retArray[i]=format(array[i]);
		}
		return retArray;
	}

	public <T> T[] toArray(T[] a) {
		List<String> l = subList(0,size());
		return l.toArray(a);
	}
	
	public static void main(String[] args) {
		int numIter = 20000;
		TermIntList list = new TermIntList();
		for (int i=0;i<numIter;++i){
			list.add(String.valueOf(i));
		}
		long start = System.currentTimeMillis();
		List<?> rawList = list.getInnerList();
		for (int i=0;i<numIter;++i){
			rawList.get(i);
		}
		long end = System.currentTimeMillis();
		System.out.println("took: "+(end-start));
	}
}
