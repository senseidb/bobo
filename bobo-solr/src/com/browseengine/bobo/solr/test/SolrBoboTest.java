package com.browseengine.bobo.solr.test;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

public class SolrBoboTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		String url = "http://localhost:8888/cars/bobo-solr";
		SolrServer solrSvr = new CommonsHttpSolrServer(url);
		SolrQuery query = new SolrQuery();
		
		query.setQuery("cool");
		query.setFacet(true);
		query.setStart(0);
		query.setRows(10);
		query.setSortField("color", ORDER.desc);
		
		QueryResponse res = solrSvr.query(query);
		
		SolrDocumentList results = res.getResults();
		long numFound = results.getNumFound();
		System.out.println("num hits: "+numFound);
		List<FacetField> facetFieldList = res.getFacetFields();
	}

}
