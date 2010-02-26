package com.browseengine.bobo.servlet;

import java.io.IOException;
import java.io.Writer;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.json.JSONException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.impl.QueryProducer;
import com.browseengine.bobo.protobuf.BrowseProtobufConverter;
import com.browseengine.bobo.server.protocol.BoboHttpRequestParam;
import com.browseengine.bobo.server.protocol.BoboQueryBuilder;
import com.browseengine.bobo.server.protocol.BoboRequestBuilder;
import com.browseengine.bobo.server.protocol.BrowseJSONSerializer;
import com.browseengine.bobo.service.BrowseService;
import com.browseengine.bobo.util.XStreamDispenser;
import com.thoughtworks.xstream.XStream;

public class BoboAppController extends AbstractController {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger logger=Logger.getLogger(BoboAppController.class);
	
	private static class BoboDefaultQueryBuilder extends BoboQueryBuilder{

		BoboDefaultQueryBuilder(){
			
		}

		@Override
		public Query parseQuery(String query, String defaultField) {
			try {
				return QueryProducer.convert(query,defaultField);
			} catch (ParseException e) {
				e.printStackTrace();
				return null;
			}
		}
		
		 private static Pattern sortSep = Pattern.compile(",");

		@Override
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
	
	private final BrowseService _svc;
	
	public BoboAppController(BrowseService svc){
		_svc = svc;
	}
	
	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest req,
			HttpServletResponse res) throws Exception {
		
		BrowseRequest br=BoboRequestBuilder.buildRequest(new BoboHttpRequestParam(req),new BoboDefaultQueryBuilder());
		try {
			logger.info("REQ: "+BrowseProtobufConverter.toProtoBufString(br));
			BrowseResult result=_svc.browse(br);
			res.setCharacterEncoding("UTF-8");
			Writer writer=res.getWriter();
			
			String outputFormat=req.getParameter("output");
			if ("json".equals(outputFormat)){
				try{
				  String val=BrowseJSONSerializer.serialize(result);
				  writer.write(val);
				}
				catch(JSONException je){
					throw new IOException(je.getMessage());
				}
			}
			else{
				XStream xstream=XStreamDispenser.getXMLXStream();
				writer.write(xstream.toXML(result));
			}
			return null;
		} catch (BrowseException e) {
			throw new ServletException(e.getMessage(),e);
		}
	}
}
