package com.browseengine.bobo.gwt.svc;

import java.util.List;
import java.util.Map;

import com.browseengine.bobo.gwt.widgets.FacetValue;
import com.google.gwt.user.client.rpc.IsSerializable;

public class BoboResult implements IsSerializable {
	private int _numHits;
	private int _totalDocs;
	private long _time;
	
	private Map<String,List<FacetValue>> _facetResults;
	private List<BoboHit> _hits;

	public int getNumHits() {
		return _numHits;
	}

	public void setNumHits(int numHits) {
		_numHits = numHits;
	}

	public int getTotalDocs() {
		return _totalDocs;
	}

	public void setTotalDocs(int totalDocs) {
		_totalDocs = totalDocs;
	}

	public long getTime() {
		return _time;
	}

	public void setTime(long time) {
		_time = time;
	}

	public Map<String, List<FacetValue>> getFacetResults() {
		return _facetResults;
	}

	public void setFacetResults(Map<String, List<FacetValue>> facetResults) {
		_facetResults = facetResults;
	}

	public List<BoboHit> getHits() {
		return _hits;
	}

	public void setHits(List<BoboHit> hits) {
		_hits = hits;
	}

}
