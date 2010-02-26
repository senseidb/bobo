package com.browseengine.bobo.gwt.widgets;


public class FacetValueSelectionEvent {
	private final AbstractFacetView _src;
	private final String _facetValue;
	
	public FacetValueSelectionEvent(AbstractFacetView src,String facetValue){
		_src = src;
		_facetValue = facetValue;
	}
	
	public AbstractFacetView getSource(){
		return _src;
	}
	
	public String getFacetValue(){
		return _facetValue;
	}
}
