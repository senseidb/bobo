package com.browseengine.solr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.SolrParams;

import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.server.protocol.BoboRequestBuilder;
import com.browseengine.bobo.util.ListMerger;
import com.browseengine.bobo.util.XStreamDispenser;
import com.browseengine.solr.BoboRequestHandler.BoboSolrParams;
import com.thoughtworks.xstream.XStream;

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
			offset = params.getInt(BoboRequestBuilder.START, 0);
			count = params.getInt(BoboRequestBuilder.COUNT, 0);
		}
		
		@Override
		public String get(String name) {
			if (BoboRequestBuilder.START.equals(name)){
				return "0";
			}
			else if (BoboRequestBuilder.COUNT.equals(name)){
				return String.valueOf(offset+count);
			}
			else if (BoboRequestHandler.SHARD_PARAM.equals(name)){
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
			if (BoboRequestHandler.SHARD_PARAM.equals(name)){
				return null;
			}
			return _params.getParams(name);
		}
	}
	
	public static BrowseResult broadcast(ExecutorService threadPool,BoboSolrParams boboSolrParams,BrowseRequest req,String[] baseURL,int maxRetry){
		long start = System.currentTimeMillis();
		Future<BrowseResult>[] futureList = (Future<BrowseResult>[]) new Future[baseURL.length];
        for (int i = 0; i < baseURL.length; i++)
		{
          SolrParams dispatchParams = new DispatchSolrParams(boboSolrParams._params);
          Callable<BrowseResult> callable = newCallable(new BoboSolrParams(dispatchParams),baseURL[i],maxRetry);
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
        
        ArrayList<BrowseHit> mergedList = ListMerger.mergeLists(req.getOffset(), req.getCount(), iteratorList.toArray(new Iterator[iteratorList.size()]), comparator);
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
	
	private static BrowseResult parseResponse(InputStream input, String charset) throws UnsupportedEncodingException{
		XStream parser = XStreamDispenser.getXMLXStream();
		Reader r = new InputStreamReader(input,charset);
		return (BrowseResult)(parser.fromXML(r));
	}
	
	private static BrowseResult doShardCall(BoboSolrParams boboSolrParams,String baseURL,int maxRetry) throws HttpException, IOException{
		String path = "/select";
		GetMethod method = null;
		try{
			method = new GetMethod("http://"+baseURL + path + ClientUtils.toQueryString( boboSolrParams._params, false ) );
			String charset = method.getResponseCharSet();
			InputStream responseStream = null;
			while(maxRetry-- > 0){
				try
				{
				  int status = client.executeMethod(method);
				  if (HttpStatus.SC_OK != status){
					  logger.error("status: "+status+", retry #: "+maxRetry);
					  continue;
				  }
				  responseStream = method.getResponseBodyAsStream();
				  break;
				}
				catch(Exception e){
				  logger.error(e.getMessage()+" retry #: "+maxRetry,e);
				}
			}
			
			if (responseStream == null){
				throw new IOException("unable to perform remote request, all retries have been exhausted");
			}
			// Read the contents
		    return parseResponse(responseStream, charset);
		}
		catch(IOException ioe ){
			ioe.printStackTrace();
			throw ioe;
		}
		catch(RuntimeException e){
			e.printStackTrace();
			throw e;
		}
		finally{
			if (method!=null){
				method.releaseConnection();
			}
		}
	}
	
	private static Callable<BrowseResult> newCallable(final BoboSolrParams boboSolrParams,final String baseURL,final int maxRetry){
		return new Callable<BrowseResult>(){

			public BrowseResult call() throws Exception {
				return doShardCall(boboSolrParams, baseURL, maxRetry);
			}
			
		};
	}
}
