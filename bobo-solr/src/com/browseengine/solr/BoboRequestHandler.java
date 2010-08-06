package com.browseengine.solr;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexReader;
import org.apache.solr.search.SolrIndexSearcher;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.server.protocol.BoboParams;
import com.browseengine.bobo.server.protocol.BoboRequestBuilder;

public class BoboRequestHandler implements SolrRequestHandler {
	
	private static final String VERSION="2.0.4";
	private static final String NAME="Bobo-Browse";
	
	static final String BOBORESULT="boboresult";
	
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
	
	
	public void handleRequest(SolrQueryRequest req, SolrQueryResponse rsp) {
		
		SolrParams solrParams = req.getParams();
		IndexSchema schema = req.getSchema();
		String shardsVal = solrParams.get(ShardParams.SHARDS, null);
		String q = solrParams.get( CommonParams.Q );
		String df = solrParams.get(CommonParams.DF);
		Query query = QueryParsing.parseQuery(q, df, schema);
		Sort sort = QueryParsing.parseSort(solrParams.get(CommonParams.SORT), schema);
		BrowseRequest br=BoboRequestBuilder.buildRequest(solrParams,query,sort);
		logger.info("browse request: "+br);
		
		BrowseResult res = null;
		if (shardsVal == null && !solrParams.getBool(ShardParams.IS_SHARD, false))
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

					fillResponse(br,res,rsp);
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
			res = DispatchUtil.broadcast(_threadPool, solrParams, br, shards, 5);
		}
		
	}
	
	public static void fillResponse(BrowseRequest req,BrowseResult res,SolrQueryResponse solrRsp){
		
		NamedList facetFieldList = new SimpleOrderedMap();
		Map<String,FacetAccessible> facetMap = res.getFacetMap();
		
		Set<Entry<String,FacetAccessible>> entries = facetMap.entrySet();
		for (Entry<String,FacetAccessible> entry : entries){
			
			NamedList facetList = new NamedList();
			facetFieldList.add(entry.getKey(), facetList);
			FacetAccessible facetAccessbile = entry.getValue();
			List<BrowseFacet> facets = facetAccessbile.getFacets();
			for (BrowseFacet facet : facets){
				facetList.add(facet.getValue(),facet.getFacetValueHitCount());
			}
		}
		
		NamedList facetResList = new SimpleOrderedMap();
		facetResList.add("facet_fields", facetFieldList);
		solrRsp.add( "facet_counts", facetResList );
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
