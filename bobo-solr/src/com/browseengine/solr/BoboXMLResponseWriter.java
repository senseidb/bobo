package com.browseengine.solr;

import java.io.IOException;
import java.io.Writer;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.QueryResponseWriter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;

import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.util.XStreamDispenser;
import com.thoughtworks.xstream.XStream;

public class BoboXMLResponseWriter implements QueryResponseWriter {

	
	public String getContentType(SolrQueryRequest request,
			SolrQueryResponse response) {
		return CONTENT_TYPE_XML_UTF8;
	}

	public void init(NamedList arg0) {
		// TODO Auto-generated method stub
		
	}

	public void write(Writer writer, SolrQueryRequest request, SolrQueryResponse response)
			throws IOException {
		NamedList vals=response.getValues();
		
		BrowseResult res=(BrowseResult)vals.get(BoboRequestHandler.BOBORESULT);
		if (res!=null){
			XStream xstream=XStreamDispenser.getXMLXStream();
			String val=xstream.toXML(res);
			writer.write(val);
		}
	}
}
