package com.browseengine.solr;

import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexReader;
import org.apache.solr.search.SolrIndexSearcher;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.server.protocol.BoboParams;
import com.browseengine.bobo.server.protocol.BoboQueryBuilder;
import com.browseengine.bobo.server.protocol.BoboRequestBuilder;

public class BoboRequestHandler implements SolrRequestHandler {
	
	private static final String VERSION="2.0.4";
	private static final String NAME="Bobo-Browse";
	
	static final String BOBORESULT="boboresult";
	public static final String SHARD_PARAM = "shards";
	
	private static final String THREAD_POOL_SIZE_PARAM = "thread_pool_size";

	private static final String MAX_SHARD_COUNT_PARAM = "max_shard_count";
	
    private ExecutorService _threadPool = null;
	
	private static Logger logger=Logger.getLogger(BoboRequestHandler.class);
	
	public static class BoboSolrParams extends BoboParams{
		SolrParams _params;
		BoboSolrParams(SolrParams params){
			_params=params;
		}
		
		@Override
		public String get(String name) {
			return _params.get(name);
		}

		@Override
		public Iterator<String> getParamNames() {
			return _params.getParameterNamesIterator();
		}

        @Override
        public String[] getStrings(String name)
        {
          return _params.getParams(name);
        }
	}
	
	public static class BoboSolrQueryBuilder extends BoboQueryBuilder{
		SolrQueryRequest _req;
		BoboSolrQueryBuilder(SolrQueryRequest req){
			_req=req;
		}

		@Override
		public Query parseQuery(String query, String defaultField) {
			return QueryParsing.parseQuery(query, defaultField, _req.getParams(), _req.getSchema());
		}

		@Override
		public Sort parseSort(String sortStr) {
			Sort sort=null;
			if( sortStr != null ) {
		        sort = QueryParsing.parseSort(sortStr, _req.getSchema());
		    }
			return sort;
		}
	}
	
	public void handleRequest(SolrQueryRequest req, SolrQueryResponse rsp) {
		
		BoboSolrParams boboSolrParams = new BoboSolrParams(req.getParams());
		
		String shardsVal = boboSolrParams.getString(SHARD_PARAM, null);
		BrowseRequest br=BoboRequestBuilder.buildRequest(boboSolrParams,new BoboSolrQueryBuilder(req));
		logger.info("browse request: "+br);
		
		BrowseResult res = null;
		if (shardsVal == null)
		{
			SolrIndexSearcher searcher=req.getSearcher();
			
			SolrIndexReader solrReader = searcher.getReader();
			BoboIndexReader reader = (BoboIndexReader)solrReader.getWrappedReader();
			
			if (reader instanceof BoboIndexReader){
			    try {
	                BoboBrowser browser = new BoboBrowser(reader);
	                
					res=browser.browse(br);
					 /*
					if(HighlightingUtils.isHighlightingEnabled(req) && query != null) {
						NamedList sumData = HighlightingUtils.doHighlighting(
						        results.docList, query.rewrite(req.getSearcher().getReader()), req, new String[]{defaultField});
						      if(sumData != null)
						        rsp.add("highlighting", sumData);
					}
					*/
					
				} catch (Exception e) {
					logger.error(e.getMessage(),e);
					throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,e.getMessage(),e);
				}
			   
			}
			else{
		        throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,"invalid reader, please make sure BoboIndexReaderFactory is set.");
			}
		}
		else{
			// multi sharded request
			String[] shards = shardsVal.split(",");
			res = DispatchUtil.broadcast(_threadPool, boboSolrParams, br, shards, 5);
		}
		rsp.add(BOBORESULT, res);
		
	}
	

	public void init(NamedList params) {
		int threadPoolSize;
		try{
			threadPoolSize = Integer.parseInt((String)params.get(THREAD_POOL_SIZE_PARAM));
		}
		catch(Exception e){
			threadPoolSize = 100;
		}
		
		int shardCount;
		try{
			shardCount = Integer.parseInt((String)params.get(MAX_SHARD_COUNT_PARAM));
		}
		catch(Exception e){
			shardCount = 10;
		}
		
		_threadPool = Executors.newFixedThreadPool(threadPoolSize * shardCount);
	}

	public Category getCategory() {
		return Category.QUERYHANDLER;
	}

	public String getDescription() {
		return "Bobo browse facetted search implementation";
	}

	public URL[] getDocs() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getName() {
		return NAME;
	}

	public String getSource() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getSourceId() {
		// TODO Auto-generated method stub
		return null;
	}

	public NamedList getStatistics() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getVersion() {
		return VERSION;
	}

}
