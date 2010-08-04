package com.browseengine.bobo.facets.data;


public interface TermListFactory<T>
{
	TermValueList<T> createTermList();
	Class<?> getType();
	
	public static TermListFactory<String> StringListFactory=new TermListFactory<String>()
	{
		public TermValueList<String> createTermList()
		{
			return new TermStringList();
		}
		public Class<?> getType()
		{
		  return String.class;
		}
	};
}
