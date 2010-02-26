package com.browseengine.bobo.gwt.svc;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BoboSortSpec implements IsSerializable {
	private String _field;
	private boolean _reverse;
	
	public String getField() {
		return _field;
	}
	public void setField(String field) {
		_field = field;
	}
	public boolean isReverse() {
		return _reverse;
	}
	public void setReverse(boolean reverse) {
		_reverse = reverse;
	}
	
	
}
