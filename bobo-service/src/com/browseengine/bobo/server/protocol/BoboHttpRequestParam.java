package com.browseengine.bobo.server.protocol;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

public class BoboHttpRequestParam extends BoboParams {
	private HttpServletRequest _req;
	public BoboHttpRequestParam(HttpServletRequest req) {
		_req=req;
	}

	@Override
	public Iterator<String> getParamNames() {
		return _req.getParameterMap().keySet().iterator();
	}

	@Override
	public String get(String name) {
		return _req.getParameter(name);
	}

    @Override
    public String[] getStrings(String name)
    {
       return _req.getParameterValues(name);
    }
}
