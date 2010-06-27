package com.browseengine.bobo.api;

import java.util.Map;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BrowseSelection.ValueOperation;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;

public class BoboRequestBuilder {
  private BrowseRequest _req;
  
  public BoboRequestBuilder(){
	_req = new BrowseRequest();
  }
  
  public BoboRequestBuilder select(String field,String... vals){
	return select(field,ValueOperation.ValueOperationOr,vals);
  }
  

  public BoboRequestBuilder select(String field,BrowseSelection.ValueOperation op,String... vals){
	return select(field,op,null,vals);
  }
  
  public BoboRequestBuilder select(String field,BrowseSelection.ValueOperation op,Map<String,String> props,String... vals){
	_req.removeSelection(field);
	BrowseSelection sel = new BrowseSelection(field);
	sel.setValues(vals);
	sel.setSelectionOperation(op);
	sel.setSelectionProperties(props);
	return this;
  }
  
  public BoboRequestBuilder orderBy(String field,boolean desc){
	return orderBy(field,desc,SortField.CUSTOM);
  }
  
  public BoboRequestBuilder orderBy(String field,boolean desc,int type){
	_req.addSortField(new SortField(field,type,desc));
	return this;
  }
  
  public BoboRequestBuilder setPage(int offset,int count){
	_req.setOffset(offset);
	_req.setCount(count);
	return this;
  }
  
  public BoboRequestBuilder setFilter(Filter filter){
	_req.setFilter(filter);
	return this;
  }
  
  public BoboRequestBuilder setFetchStoredFields(boolean fetch){
	_req.setFetchStoredFields(fetch);
	return this;
  }
  
  public BoboRequestBuilder setShowExplanation(boolean showExplanation){
	_req.setShowExplanation(showExplanation);
	return this;
  }
  
  public BoboRequestBuilder setGroupBy(String field){
	  GroupByParamBuilder facetSpecBuilder = new GroupByParamBuilder();
	_req.setFacetSpec(field, facetSpecBuilder.build());
	return this;
  }
  
  
  public BoboRequestBuilder setGroupBy(String field,FacetSpec fspec){
	_req.setFacetSpec(field, fspec);
	return this;
  }
  
  public GroupByParamBuilder newGroupByParamBuilder(){
	  return new GroupByParamBuilder();
  }
  
  public BoboRequestBuilder clear(){
	_req = new BrowseRequest();
	return this;
  }
  
  public static class GroupByParamBuilder{
	private FacetSpec _fspec;
	public static final int DEFAULT_MAX_COUNT = 10;
	public static final int DEFAULT_MIN_HIT_COUNT = 1;
	public static final FacetSpec.FacetSortSpec DEFAULT_FACET_SORT = FacetSortSpec.OrderHitsDesc;
	
	private GroupByParamBuilder(){
	  _fspec = new FacetSpec();
	  _fspec.setMaxCount(DEFAULT_MAX_COUNT);
	  _fspec.setMinHitCount(DEFAULT_MIN_HIT_COUNT);
	  _fspec.setOrderBy(DEFAULT_FACET_SORT);
	}
	
	public GroupByParamBuilder setExpandSelection(boolean expandSelection){
	  _fspec.setExpandSelection(expandSelection);
	  return this;
	}
	
	public GroupByParamBuilder setMinHitCount(int minHitCount){
	  _fspec.setMinHitCount(minHitCount);
	  return this;
	}
	
	public GroupByParamBuilder setMaxCount(int maxCount){
	  _fspec.setMaxCount(maxCount);
	  return this;
	}
	
	public GroupByParamBuilder setFacetSort(FacetSortSpec sort){
	  _fspec.setOrderBy(sort);
	  return this;
	}
	
	public GroupByParamBuilder setFacetSortComparator(ComparatorFactory comparatorFactory){
	  _fspec.setOrderBy(FacetSortSpec.OrderByCustom);
	  _fspec.setCustomComparatorFactory(comparatorFactory);
	  return this;
	}
	
	public FacetSpec build(){
	  return _fspec;
	}
  }
  
  public BrowseRequest build(){
	  return _req;
  }
}
