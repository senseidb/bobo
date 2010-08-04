package com.browseengine.bobo.gwt.svc;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BoboFacetSpec implements IsSerializable {
	private boolean _orderByHits;
	private int _max;
	private boolean _expandSelection;
	private int _minCount;
	
	public boolean isOrderByHits() {
		return _orderByHits;
	}
	public void setOrderByHits(boolean orderByHits) {
		_orderByHits = orderByHits;
	}
	public int getMax() {
		return _max;
	}
	public void setMax(int max) {
		_max = max;
	}
	public boolean isExpandSelection() {
		return _expandSelection;
	}
	public void setExpandSelection(boolean expandSelection) {
		_expandSelection = expandSelection;
	}
	public int getMinCount() {
		return _minCount;
	}
	public void setMinCount(int minCount) {
		_minCount = minCount;
	}
}
