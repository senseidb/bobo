package com.browseengine.bobo.util;

import java.util.Arrays;


public class StringArrayComparator implements Comparable<StringArrayComparator> {
	private String[] vals;
	public StringArrayComparator(String[] vals){
		this.vals = vals;
	}
	public int compareTo(StringArrayComparator node) {
		String[] o = node.vals;
		if (vals==o){
			return 0;
		}
		if (vals == null){
			return -1;
		}
		if (o == null){
			return 1;
		}
		for (int i = 0;i < vals.length; ++i){
			if (i>=o.length){
				return 1;
			}
			int compVal = vals[i].compareTo(o[i]);
			if (vals[i].startsWith("-") && o[i].startsWith("-") ) {
			  compVal *= -1;
			}
			if (compVal!=0) return compVal;
		}
		if (vals.length == o.length) return 0;
		return -1;
	}
	
	@Override
	public String toString(){
		return Arrays.toString(vals);
	}

}
