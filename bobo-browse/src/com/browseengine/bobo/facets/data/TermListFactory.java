package com.browseengine.bobo.facets.data;


public interface TermListFactory {
	TermValueList createTermList();
	
	public static TermListFactory StringListFactory=new TermListFactory()
	{
		public TermValueList createTermList() {
			return new TermStringList();
		}	
	};
}
