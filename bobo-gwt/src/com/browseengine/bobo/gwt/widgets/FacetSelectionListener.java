package com.browseengine.bobo.gwt.widgets;

public interface FacetSelectionListener {
	public void handleSelectedEvent(FacetValueSelectionEvent event);
	public void handleUnSelectedEvent(FacetValueSelectionEvent event);
	public void handleClearSelections(AbstractFacetView view);
}
