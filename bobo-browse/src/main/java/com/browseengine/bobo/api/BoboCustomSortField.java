package com.browseengine.bobo.api;

import org.apache.lucene.search.SortField;

import com.browseengine.bobo.sort.DocComparatorSource;

public class BoboCustomSortField extends SortField {

	private static final long serialVersionUID = 1L;

	private final DocComparatorSource _factory;
	
	public BoboCustomSortField(String field,boolean reverse,DocComparatorSource factory) {
		super(field, SortField.CUSTOM, reverse);
		_factory = factory;
	}
	
	public DocComparatorSource getCustomComparatorSource(){
		return _factory;
	}

}
