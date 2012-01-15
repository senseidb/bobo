package com.browseengine.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Weight;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexReader;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SortSpec;
import org.apache.solr.util.SolrPluginUtils;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;
import com.kamikaze.docidset.impl.AndDocIdSet;

public class BoboFacetComponent extends SearchComponent {

	private static final String THREAD_POOL_SIZE_PARAM = "thread_pool_size";

	private static final String MAX_SHARD_COUNT_PARAM = "max_shard_count";
	
    private ExecutorService _threadPool = null;
	
	private static Logger logger=Logger.getLogger(BoboFacetComponent.class);
	
	public BoboFacetComponent() {
		// TODO Auto-generated constructor stub
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

	@Override
	public String getDescription() {
		return "Handle Bobo Faceting";
	}

	@Override
	public String getSource() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSourceId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
	    if (rb.req.getParams().getBool(FacetParams.FACET,false)) {
	      rb.setNeedDocSet( true );
	      rb.doFacets = true;
	    }
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {
		rb.stage = ResponseBuilder.STAGE_START;
		SolrParams params = rb.req.getParams();
		// Set field flags
	    String fl = params.get(CommonParams.FL);
	    int fieldFlags = 0;
	    if (fl != null) {
	      fieldFlags |= SolrPluginUtils.setReturnFields(fl, rb.rsp);
	    }
	    rb.setFieldFlags( fieldFlags );

	    String defType = params.get(QueryParsing.DEFTYPE);
	    defType = defType==null ? QParserPlugin.DEFAULT_QTYPE : defType;

	    String qString = params.get( CommonParams.Q );
	    if (qString == null || qString.length()==0){
	    	qString="*:*";
	    }
	    if (rb.getQueryString() == null) {
	      rb.setQueryString( qString);
	    }

	    try {
	      QParser parser = QParser.getParser(rb.getQueryString(), defType, rb.req);
	      rb.setQuery( parser.getQuery() );
	      rb.setSortSpec( parser.getSort(true) );
	      rb.setQparser(parser);

	      
	      String[] fqs = params.getParams(CommonParams.FQ);
	      if (fqs!=null && fqs.length!=0) {
	        List<Query> filters = rb.getFilters();
	        if (filters==null) {
	          filters = new ArrayList<Query>();
	          rb.setFilters( filters );
	        }
	        for (String fq : fqs) {
	          if (fq != null && fq.trim().length()!=0) {
	            QParser fqp = QParser.getParser(fq, null, rb.req);
	            filters.add(fqp.getQuery());
	          }
	        }
	      }  
	    } catch (ParseException e) {
	      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
	    }
		
		Query query = rb.getQuery();
	    SortSpec sortspec = rb.getSortSpec();
	    Sort sort = null;
	    if (sortspec!=null){
	    	sort = sortspec.getSort();
	    }

	    SolrParams solrParams = rb.req.getParams();

		String shardsVal = solrParams.get(ShardParams.SHARDS, null);
		
	    BrowseRequest br = null;
	    try{
	    	br=BoboRequestBuilder.buildRequest(solrParams,query,sort);
	    }
	    catch(Exception e){
	    	throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, e);
	    }
	    BrowseResult res = null;
	    if (shardsVal == null && !solrParams.getBool(ShardParams.IS_SHARD, false))
		{

			SolrIndexSearcher searcher=rb.req.getSearcher();
			
			SolrIndexReader solrReader = searcher.getReader();
			BoboIndexReader reader = (BoboIndexReader)solrReader.getWrappedReader();
			
			if (reader instanceof BoboIndexReader){
			    try {
				    List<Query> filters = rb.getFilters();
				    if (filters!=null){
				    	final ArrayList<DocIdSet> docsets = new ArrayList<DocIdSet>(filters.size());
				        for (Query filter : filters){
				        	Weight weight = filter.createWeight(rb.req.getSearcher());
				        	final Scorer scorer = weight.scorer(reader, false, true);
				        	docsets.add(new DocIdSet(){
								@Override
								public DocIdSetIterator iterator() throws IOException {
									return scorer;
								}
				        		
				        	});
				        }
				        
				        if (docsets.size()>0){
				        	br.setFilter(
				        		new Filter(){
								@Override
								public DocIdSet getDocIdSet(IndexReader reader)
										throws IOException {
									return new AndDocIdSet(docsets);
								}
				        	});
				        }
				    }
			        
				    Set<String> facetNames = reader.getFacetNames();
				    Set<String> returnFields = rb.rsp.getReturnFields();
				    Set<String> storedFields = new HashSet<String>();
				    if (returnFields!=null){
				      for (String fld : returnFields){
				    	if (!facetNames.contains(fld)){
				    		storedFields.add(fld);
				    	}
				      }
				      br.setFetchStoredFields(!storedFields.isEmpty());
				    }
				    
				    
	                BoboBrowser browser = new BoboBrowser(reader);
	                
					res=browser.browse(br);
					
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
	    
	    SolrDocumentList docList = new SolrDocumentList();
	    
	    
	    docList.setNumFound(res.getNumHits());
	    docList.setStart(br.getOffset());
	    
	    rb.stage = ResponseBuilder.STAGE_GET_FIELDS;
	    boolean returnScores = (rb.getFieldFlags() & SolrIndexSearcher.GET_SCORES) != 0;
	    
	    BrowseHit[] hits = res.getHits();
	    if (hits!=null){
	      for (BrowseHit hit : hits){
	    	SolrDocument doc = convert(hit,rb.rsp.getReturnFields());
	    	if (doc!=null){
	    		if (returnScores){
	    			doc.addField("score", hit.getScore());
	    		}
	    		docList.add(doc);
	    	}
	      }
	    }
	    
	    rb.rsp.add("response", docList);
	    
	    if (rb.doFacets) {
		  fillResponse(br,res,rb.rsp);
		}
	    
	    rb.stage = ResponseBuilder.STAGE_DONE;
	}
	
	private static SolrDocument convert(BrowseHit hit,Set<String> returnFields){
		SolrDocument doc = new SolrDocument();
		if (returnFields!=null){
		  for (String fld : returnFields){
			String[] fv = hit.getFields(fld);
			if (fv==null){
			  Document storedFields = hit.getStoredFields();
			  if (storedFields!=null){
				  fv = storedFields.getValues(fld);
			  }
			}
			if (fv!=null){
				doc.addField(fld, fv);
			}
		  }
		}
		return doc;
	}

	@Override
	public void finishStage(ResponseBuilder rb) {
	}

    private static void fillResponse(BrowseRequest req,BrowseResult res,SolrQueryResponse solrRsp){
		
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
		
		NamedList facetQueryList = new SimpleOrderedMap();
		
		facetResList.add("facet_queries", facetQueryList);
		solrRsp.add( "facet_counts", facetResList );
		
	}
	
}
