package com.browseengine.bobo.facets.data;


public interface TermListFactory<T>
{
	TermValueList<T> createTermList(int capacity);
	TermValueList<T> createTermList();
	Class<?> getType();
	
	public static TermListFactory<String> StringListFactory=new TermListFactory<String>()
	{
		public TermValueList<String> createTermList(int capacity)
		{
			return new TermStringList(capacity);
		}
		public TermValueList<String> createTermList()
		{
			return createTermList(-1);
		}
		public Class<?> getType()
		{
		  return String.class;
		}
	};
}
