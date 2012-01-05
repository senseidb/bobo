package com.browseengine.bobo.client;

import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;

public class BrowseRequestBuilder {
	private BrowseRequest _req;
	private String _qString;
	public BrowseRequestBuilder(){
		clear();
	}
	
	public BrowseRequestBuilder addSelection(String name, String val, boolean isNot){
		BrowseSelection sel = _req.getSelection(name);
		if (sel==null){
			sel = new BrowseSelection(name);
		}
		if (isNot){
			sel.addNotValue(val);
		}
		else{
			sel.addValue(val);
		}
		_req.addSelection(sel);
    return this;
	}
	
	public BrowseRequestBuilder clearSelection(String name){
		_req.removeSelection(name);
    return this;
	}
	
	public BrowseRequestBuilder applyFacetSpec(String name, int minHitCount, int maxCount, boolean expand, FacetSortSpec orderBy){
		FacetSpec fspec = new FacetSpec();
		fspec.setMinHitCount(minHitCount);
		fspec.setMaxCount(maxCount);
		fspec.setExpandSelection(expand);
		fspec.setOrderBy(orderBy);
		_req.setFacetSpec(name, fspec);
    return this;
	}
	
	public BrowseRequestBuilder applySort(SortField[] sorts){
		if (sorts==null){
			_req.clearSort();
		}
		else{
			_req.setSort(sorts);
		}
    return this;
	}
	
	public BrowseRequestBuilder clearFacetSpecs(){
		_req.getFacetSpecs().clear();
    return this;
	}
	public BrowseRequestBuilder clearFacetSpec(String name){
		_req.getFacetSpecs().remove(name);
    return this;
	}
	
	public BrowseRequestBuilder setOffset(int offset){
		_req.setOffset(offset);
    return this;
	}
	
	public BrowseRequestBuilder setCount(int count){
		_req.setCount(count);
    return this;
	}
	
	public BrowseRequestBuilder setQuery(String qString){
		_qString = qString;
    return this;
	}
	
	public BrowseRequestBuilder clear(){
		_req = new BrowseRequest();
		_req.setOffset(0);
		_req.setCount(5);
		_req.setFetchStoredFields(true);
		_qString = null;
    return this;
	}
	
	public BrowseRequestBuilder clearSelections(){
		_req.clearSelections();
    return this;
	}
	
	public BrowseRequest getRequest(){
		return _req;
	}
	
	public String getQueryString(){
		return _qString;
	}
}
