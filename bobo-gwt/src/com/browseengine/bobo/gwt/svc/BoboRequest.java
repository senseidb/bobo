package com.browseengine.bobo.gwt.svc;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BoboRequest implements IsSerializable {
	private String _query;
	private int _offset;
	private int _count;
	
	private Map<String,BoboSelection> _selections;
	private List<BoboSortSpec> _sortSpecs;
	private Map<String,BoboFacetSpec> _facetSpecMap;
	
	public String getQuery() {
		return _query;
	}
	public void setQuery(String query) {
		_query = query;
	}
	public int getOffset() {
		return _offset;
	}
	public void setOffset(int offset) {
		_offset = offset;
	}
	public int getCount() {
		return _count;
	}
	public void setCount(int count) {
		_count = count;
	}
	public Map<String, BoboSelection> getSelections() {
		return _selections;
	}
	public void setSelections(Map<String, BoboSelection> selections) {
		_selections = selections;
	}
	public List<BoboSortSpec> getSortSpecs() {
		return _sortSpecs;
	}
	public void setSortSpecs(List<BoboSortSpec> sortSpecs) {
		_sortSpecs = sortSpecs;
	}
	public Map<String, BoboFacetSpec> getFacetSpecMap() {
		return _facetSpecMap;
	}
	public void setFacetSpecMap(Map<String, BoboFacetSpec> facetSpecMap) {
		_facetSpecMap = facetSpecMap;
	}
	
}
