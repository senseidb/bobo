package com.browseengine.bobo.demo.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.browseengine.bobo.gwt.svc.BoboRequest;
import com.browseengine.bobo.gwt.svc.BoboResult;
import com.browseengine.bobo.gwt.svc.BoboSearchService;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;


@SuppressWarnings("serial")
public class BoboDemoServlet extends RemoteServiceServlet implements BoboSearchService{
	private static final Logger log = Logger.getLogger(BoboDemoServlet.class);
	
	private WebApplicationContext _appCtx;
	private BoboSearchService _searchSvc;


	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		ServletContext ctx = config.getServletContext();
		_appCtx = WebApplicationContextUtils.getRequiredWebApplicationContext(ctx);
		_searchSvc = (BoboSearchService) _appCtx.getBean("bobo-svc");
	}

	public BoboResult search(BoboRequest req) {
		try{
		  return _searchSvc.search(req);
		}
		catch(Exception e){
			log.error(e.getMessage(),e);
			return new BoboResult();
		}
	}
	
}
