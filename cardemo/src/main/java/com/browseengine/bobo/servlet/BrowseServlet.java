/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  John Wang
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * send mail to owner@browseengine.com.
 */

package com.browseengine.bobo.servlet;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.json.JSONException;

import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.impl.QueryProducer;
import com.browseengine.bobo.server.protocol.BoboHttpRequestParam;
import com.browseengine.bobo.server.protocol.BrowseJSONSerializer;
import com.browseengine.bobo.service.BrowseService;
import com.browseengine.bobo.service.BrowseServiceFactory;
import com.browseengine.solr.BoboRequestBuilder;

public class BrowseServlet
	extends HttpServlet{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger=Logger.getLogger(BrowseServlet.class);
	
	private static class BoboDefaultQueryBuilder{

		BoboDefaultQueryBuilder(){
			
		}

		public Query parseQuery(String query, String defaultField) {
			try {
				return QueryProducer.convert(query,defaultField);
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		 private static Pattern sortSep = Pattern.compile(",");

		public Sort parseSort(String sortSpec) {
			if (sortSpec==null || sortSpec.length()==0) return null;

		    String[] parts = sortSep.split(sortSpec.trim());
		    if (parts.length == 0) return null;

		    SortField[] lst = new SortField[parts.length];
		    for( int i=0; i<parts.length; i++ ) {
		      String part = parts[i].trim();
		      boolean top=true;
		        
		      int idx = part.indexOf( ' ' );
		      if( idx > 0 ) {
		        String order = part.substring( idx+1 ).trim();
		    	if( "desc".equals( order ) || "top".equals(order) ) {
		    	  top = true;
		    	}
		    	else if ("asc".equals(order) || "bottom".equals(order)) {
		    	  top = false;
		    	}
		    	else {
		    	  throw new IllegalArgumentException("Unknown sort order: "+order);
		    	}
		    	part = part.substring( 0, idx ).trim();
		      }
		      else {
				throw new IllegalArgumentException("Missing sort order." );
		      }
		    	
		      if( "score".equals(part) ) {
		        if (top) {
		          // If thre is only one thing in the list, just do the regular thing...
		          if( parts.length == 1 ) {
		            return null; // do normal scoring...
		          }
		          lst[i] = SortField.FIELD_SCORE;
		        }
		        else {
		          lst[i] = new SortField(null, SortField.SCORE, true);
		        }
		      } 
		      else {
		        lst[i] = new SortField(part,SortField.STRING,top);
		      }
		    }
		    return new Sort(lst);
		}
		
		
	}
	
	private BrowseService _svc;
	public BrowseServlet() {
		super();
	}
	
	
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		_svc=getServiceInstance(config);
	}

	protected BrowseService getServiceInstance(ServletConfig config) throws ServletException{
	  
		String indexDir=config.getServletContext().getInitParameter("index.dir");
		
		if (null == indexDir || indexDir.length() == 0)
			throw new ServletException("No index directory configured");
	
		try {
			return BrowseServiceFactory.createBrowseService(new File(indexDir));
		} catch (BrowseException e) {
			throw new ServletException(e.getMessage(),e.getCause());
		}
	}
	
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		SolrParams params = new BoboHttpRequestParam(req);
		String qstring = params.get(CommonParams.Q);
		String df = params.get(CommonParams.DF);
		String sortString = params.get(CommonParams.SORT);
		BoboDefaultQueryBuilder qbuilder = new BoboDefaultQueryBuilder();
		Query query = qbuilder.parseQuery(qstring, df);
		Sort sort = qbuilder.parseSort(sortString);
		BrowseRequest br = null; 
		try {
			br=BoboRequestBuilder.buildRequest(params,query,sort);
			BrowseResult result=_svc.browse(br);
			res.setCharacterEncoding("UTF-8");
			Writer writer=res.getWriter();
			
				try{
				  String val=BrowseJSONSerializer.serialize(result);
				  writer.write(val);
				}
				catch(JSONException je){
					throw new IOException(je.getMessage());
				}
		} catch (BrowseException e) {
			throw new ServletException(e.getMessage(),e);
		}
	}
	
	@Override
	public void destroy() {
		super.destroy();
		try {
			_svc.close();
		} catch (BrowseException e) {
			logger.warn("Problem shutting down browse engine.",e);
		}
	}
}
