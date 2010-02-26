package com.browseengine.bobo.gwt.svc;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BoboHit implements IsSerializable {
	private int _docid;
	private float _score;
	private Map<String,String[]> _fields;
	
	public int getDocid() {
		return _docid;
	}
	public void setDocid(int docid) {
		_docid = docid;
	}
	public float getScore() {
		return _score;
	}
	public void setScore(float score) {
		_score = score;
	}
	public Map<String, String[]> getFields() {
		return _fields;
	}
	public void setFields(Map<String, String[]> fields) {
		_fields = fields;
	}
}
