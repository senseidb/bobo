package com.browseengine.bobo.gwt.svc;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BoboSelection implements IsSerializable {
	private boolean _opAnd;
	protected List<String> values;
	protected List<String> notValues;
	
	private Map<String,String> _selectionProperties;

	public boolean isOpAnd() {
		return _opAnd;
	}

	public void setOpAnd(boolean opAnd) {
		_opAnd = opAnd;
	}

	public List<String> getValues() {
		return values;
	}
	
	public void addValue(String val){
		if (this.values==null){
			this.values = new LinkedList<String>();
		}
		this.values.add(val);
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public List<String> getNotValues() {
		return notValues;
	}

	public void setNotValues(List<String> notValues) {
		this.notValues = notValues;
	}

	public Map<String, String> getSelectionProperties() {
		return _selectionProperties;
	}

	public void setSelectionProperties(Map<String, String> selectionProperties) {
		_selectionProperties = selectionProperties;
	}
}
