package com.browseengine.bobo.server.protocol;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.apache.solr.common.params.SolrParams;

public class BoboHttpRequestParam extends SolrParams {
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private final HttpServletRequest _req;

  public BoboHttpRequestParam(HttpServletRequest req) {
    _req = req;
  }

  @Override
  public String get(String name) {
    return _req.getParameter(name);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterator<String> getParameterNamesIterator() {
    return _req.getParameterMap().keySet().iterator();
  }

  @Override
  public String[] getParams(String param) {
    return _req.getParameterValues(param);
  }
}
