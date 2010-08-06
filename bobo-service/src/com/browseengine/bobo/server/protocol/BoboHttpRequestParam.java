package com.browseengine.bobo.server.protocol;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.params.SolrParams;

public class BoboHttpRequestParam extends SolrParams {
	private HttpServletRequest _req;
	public BoboHttpRequestParam(HttpServletRequest req) {
		_req=req;
	}

	@Override
	public String get(String name) {
		return _req.getParameter(name);
	}

	@Override
	public Iterator<String> getParameterNamesIterator() {
		return _req.getParameterMap().keySet().iterator();
	}

	@Override
	public String[] getParams(String param) {
		return _req.getParameterValues(param);
	}
}
