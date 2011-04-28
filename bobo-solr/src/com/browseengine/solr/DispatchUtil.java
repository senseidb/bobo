package com.browseengine.solr;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.MappedFacetAccessible;
import com.browseengine.bobo.util.ListMerger;

public class DispatchUtil {

	private static Logger logger=Logger.getLogger(DispatchUtil.class);

	 static HttpClient client;
	 
	  static int soTimeout = 0; //current default values
	  static int connectionTimeout = 0; //current default values

	  // these values can be made configurable
	static {
	    MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
	    mgr.getParams().setDefaultMaxConnectionsPerHost(20);
	    mgr.getParams().setMaxTotalConnections(10000);
	    mgr.getParams().setConnectionTimeout(connectionTimeout);
	    mgr.getParams().setSoTimeout(soTimeout);
	    // mgr.getParams().setStaleCheckingEnabled(false);
	    client = new HttpClient(mgr);    
	}
	
	private static class DispatchSolrParams extends SolrParams{
		private int offset;
		private int count;
		private SolrParams _params;
		
		DispatchSolrParams(SolrParams params){
			_params = params;
			offset = params.getInt(CommonParams.START, 0);
			count = params.getInt(CommonParams.ROWS, 0);
		}
		
		@Override
		public String get(String name) {
			if (CommonParams.START.equals(name)){
				return "0";
			}
			else if (CommonParams.ROWS.equals(name)){
				return String.valueOf(offset+count);
			}
			else if (ShardParams.SHARDS.equals(name)){
				return null;
			}
			else{
			  return _params.get(name);
			}
		}

		@Override
		public Iterator<String> getParameterNamesIterator() {
			return _params.getParameterNamesIterator();
		}

		@Override
		public String[] getParams(String name) {
			if (ShardParams.SHARDS.equals(name)){
				return null;
			}
			return _params.getParams(name);
		}
	}
	
	public static BrowseResult broadcast(ExecutorService threadPool,SolrParams boboSolrParams,BrowseRequest req,String[] baseURL,int maxRetry){
		long start = System.currentTimeMillis();
		Future<BrowseResult>[] futureList = (Future<BrowseResult>[]) new Future[baseURL.length];
        for (int i = 0; i < baseURL.length; i++)
		{
          SolrParams dispatchParams = new DispatchSolrParams(boboSolrParams);
          Callable<BrowseResult> callable = newCallable(dispatchParams,baseURL[i],maxRetry);
          futureList[i] = threadPool.submit(callable);
		}
        
		List<Map<String,FacetAccessible>> facetList=new ArrayList<Map<String,FacetAccessible>>(baseURL.length);
		
		ArrayList<Iterator<BrowseHit>> iteratorList = new ArrayList<Iterator<BrowseHit>>(baseURL.length);
		int numHits = 0;
		int totalDocs = 0;
        for (int i = 0; i < futureList.length; i++)
		{
			try { 
				BrowseResult res = futureList[i].get();
				BrowseHit[] hits = res.getHits();
				if (hits!=null){
				  for (BrowseHit hit : hits){
					hit.setDocid(hit.getDocid()+totalDocs);
				  }
				}
				iteratorList.add(Arrays.asList(res.getHits()).iterator());
				
				Map<String,FacetAccessible> facetMap = res.getFacetMap();
				if (facetMap!=null){
					facetList.add(facetMap);
				}
				//resultList.add(res); 
				numHits += res.getNumHits();
				totalDocs += res.getTotalDocs();
			}
			catch (InterruptedException e) { logger.error(e.getMessage(),e); }
			catch (ExecutionException e) { logger.error(e.getMessage(),e); }
		}
        
        Map<String,FacetAccessible> mergedFacetMap = ListMerger.mergeSimpleFacetContainers(facetList,req);
        Comparator<BrowseHit> comparator = new Comparator<BrowseHit>(){
        	public int compare(BrowseHit o1, BrowseHit o2) {
				Comparable c1=o1.getComparable();
				Comparable c2=o2.getComparable();
				if (c1==null || c2==null){
					return o2.getDocid() - o1.getDocid();
				}
				return c1.compareTo(c2);
			}
        	
        };
        
        List<BrowseHit> mergedList = req.getCount() > 0 ? ListMerger.mergeLists(req.getOffset(), req.getCount(), iteratorList.toArray(new Iterator[iteratorList.size()]), comparator) : Collections.EMPTY_LIST;
        BrowseHit[] hits = mergedList.toArray(new BrowseHit[mergedList.size()]);
        long end = System.currentTimeMillis();
        
        BrowseResult merged = new BrowseResult();
        merged.setHits(hits);
        merged.setNumHits(numHits);
        merged.setTotalDocs(totalDocs);
        merged.setTime(end-start);
        merged.addAll(mergedFacetMap);
        return merged;
	}
	
	private static BrowseResult parseResponse(QueryResponse res) throws UnsupportedEncodingException{
		BrowseResult result = new BrowseResult();
		
		result.setTime(res.getElapsedTime());
		List<FacetField> facetFields = res.getFacetFields();
		if (facetFields!=null){
		  Map<String,FacetAccessible> facetMap = new HashMap<String,FacetAccessible>();
		  for (FacetField ff : facetFields){
			  String fieldName = ff.getName();
			  List<Count> countList = ff.getValues();
			  if (countList!=null){
			    BrowseFacet[] facets = new BrowseFacet[countList.size()];
			    int i=0;
			    for (Count count : countList){
			    	facets[i++]=new BrowseFacet(count.getName(),(int)count.getCount());
			    }
			    facetMap.put(fieldName, new MappedFacetAccessible(facets));
			  }
		  }
		  result.addAll(facetMap);
		}
		
		SolrDocumentList solrDocs = res.getResults();
		if (solrDocs!=null){
			result.setNumHits((int)solrDocs.getNumFound());
			ArrayList<BrowseHit> hits = new ArrayList<BrowseHit>(solrDocs.size());
			for (SolrDocument doc : solrDocs){
				BrowseHit hit = new BrowseHit();
				Map<String,String[]> fieldMap = new HashMap<String,String[]>();
				Collection<String> fieldNames = doc.getFieldNames();
				for (String fn : fieldNames){
					Collection<String> fvals = doc.getFieldNames();
					fieldMap.put(fn, fvals.toArray(new String[fvals.size()]));
				}
				hit.setFieldValues(fieldMap);
				hits.add(hit);
			}
			result.setHits(hits.toArray(new BrowseHit[hits.size()]));
		}
		
		return result;
	}
	
	private static BrowseResult doShardCall(SolrServer solrSvr,SolrParams params,String baseURL,int maxRetry) throws SolrException, SolrServerException, IOException{
		return parseResponse(solrSvr.query(params));
	}
	
	private static Callable<BrowseResult> newCallable(final SolrParams req,final String baseURL,final int maxRetry){
		return new Callable<BrowseResult>(){

			public BrowseResult call() throws Exception {
				CommonsHttpSolrServer solrSvr = new CommonsHttpSolrServer(baseURL,client);
				return doShardCall(solrSvr,req, baseURL, maxRetry);
			}
			
		};
	}
}
