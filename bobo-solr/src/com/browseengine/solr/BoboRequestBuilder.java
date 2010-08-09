package com.browseengine.solr;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.SolrParams;

import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;

public class BoboRequestBuilder {
	
	public static final String SEL_PREFIX="bobo.sel.";
	public static final String BOBO_PREFIX="bobo";
	
	public static final String FACET_EXPAND = BOBO_PREFIX + ".expand";
	
	public static final Logger logger = Logger.getLogger(BoboRequestBuilder.class);
	
	private static void fillBoboSelections(BrowseRequest req,SolrParams params){
		/*Iterator<String> names=params.getParameterNamesIterator();
		HashMap<String,BrowseSelection> selMap=new HashMap<String,BrowseSelection>();
		
		while(names.hasNext()){
			
			String paramName=names.next();
			if (paramName.startsWith(SEL_PREFIX)){
				try{
					int index=SEL_PREFIX.length();
					int to=paramName.indexOf('.', index);
					String name,type;
					if (to>index){
						name=paramName.substring(index, to);
						index=paramName.lastIndexOf('.');
						type=paramName.substring(index+1);
					}
					else{
						name=paramName.substring(index);
						type="";
					}
					
					BrowseSelection sel=selMap.get(name);
					if (sel==null){
						sel=new BrowseSelection(name);
						selMap.put(name, sel);
					}
					if ("val".equals(type)){
						String[] vals=params.getParams(paramName);
						if (vals!=null && vals.length>0){
						  for (String val : vals)
						  {
							sel.addValue(val);
						  }
						}
					}
					else if ("notval".equals(type)){
						String val=params.get(paramName, null);
						if (val!=null && val.length()>0){
							sel.addNotValue(val);
						}
					}
					else if ("operation".equals(type)){
						String order=params.get(paramName, "or");
						if ("or".equals(order)){
							sel.setSelectionOperation(BrowseSelection.ValueOperation.ValueOperationOr);
						}
						else if ("and".equals(order)){
							sel.setSelectionOperation(BrowseSelection.ValueOperation.ValueOperationAnd);
						}
					}
					else if ("depth".equals(type)){
						String depth=params.get(paramName,null);
						if (depth!=null){
						  sel.setSelectionProperty(PathFacetHandler.SEL_PROP_NAME_DEPTH, depth);
						}
					}
					else if ("strict".equals(type)){
						String strict=params.get(paramName,null);
						if (strict!=null){
						  sel.setSelectionProperty(PathFacetHandler.SEL_PROP_NAME_STRICT, strict);
						}
					}
				}
				catch(Exception e){
					logger.error(e.getMessage(),e);
				}
			}
		}
		
		Iterator<BrowseSelection> iter=selMap.values().iterator();
		while(iter.hasNext()){
			req.addSelection(iter.next());
		}*/
		
		String[] facetQueries = params.getParams(FacetParams.FACET_QUERY);
	    if (facetQueries!=null && facetQueries.length!=0) {
		    HashMap<String,BrowseSelection> selMap = new HashMap<String,BrowseSelection>();
	    	for (String facetQuery : facetQueries){
	    		String[] parts = facetQuery.split(":");
	    		String name = parts[0];
	    		String valval = parts[1];
	    		String[] vals = valval.split(",");
	    		if (vals.length>0){
	    			BrowseSelection sel = selMap.get(name);
	    			if (sel==null){
	    				sel = new BrowseSelection(name);
	    				selMap.put(name, sel);
	    			}
	    			for (String val : vals){
	    			  sel.addValue(val);
	    			}
	    		}
	    	}
	    	if (selMap.size()>0){
	    		Collection<BrowseSelection> sels = selMap.values();
	    		for (BrowseSelection sel : sels){
	    		  req.addSelection(sel);
	    		}
	    	}
	    }
	}
	
	private static HashSet<String> UnsupportedSolrFacetParams = new HashSet<String>();
	
	static{
		UnsupportedSolrFacetParams.add(FacetParams.FACET_PREFIX);
		UnsupportedSolrFacetParams.add(FacetParams.FACET_QUERY);
		UnsupportedSolrFacetParams.add(FacetParams.FACET_METHOD);
		UnsupportedSolrFacetParams.add(FacetParams.FACET_OFFSET);
		UnsupportedSolrFacetParams.add(FacetParams.FACET_MISSING);
	}
	
	private final static Pattern splitList=Pattern.compile(",| ");
	  
    /** Split a value that may contain a comma, space of bar separated list. */
    public static String[] split(String value){
      return splitList.split(value.trim(), 0);
    }
	  
	private static boolean parseReturnedFields(String fl,Set<String> set){
		boolean doScore = false;
		if (fl != null) {
	      String[] flst = split(fl);
	      if (flst.length > 0 && !(flst.length==1 && flst[0].length()==0)) {
	        for (String fname : flst) {
	          if("score".equalsIgnoreCase(fname)){
	            doScore = true;
	          }
	          else{
	        	  set.add(fname);
	          }
	        }
	      }
	    }
		return doScore;
	}
	
	private static FacetSpec.FacetSortSpec parseFacetSort(String facetSortString,FacetSortSpec defaultSort){
		FacetSortSpec defaultFacetSortSpec;
		   
		if (FacetParams.FACET_SORT_COUNT_LEGACY.equals(facetSortString) || FacetParams.FACET_SORT_COUNT.equals(facetSortString)){
	    	defaultFacetSortSpec = FacetSpec.FacetSortSpec.OrderHitsDesc;
	    }
	    else if (FacetParams.FACET_SORT_INDEX_LEGACY.equals(facetSortString)|| FacetParams.FACET_SORT_INDEX.equals(facetSortString)){
	    	defaultFacetSortSpec = FacetSpec.FacetSortSpec.OrderValueAsc;
	    }
	    else{
	    	defaultFacetSortSpec = defaultSort;
	    }
		return defaultFacetSortSpec;
	}
	
	public static BrowseRequest buildRequest(SolrParams params,Query query,Sort sort){
	    int offset=params.getInt(CommonParams.START, 0);
	    int count=params.getInt(CommonParams.ROWS, 10);
	    
	    int defaultMinCount = params.getInt(FacetParams.FACET_MINCOUNT, 0);
	    
	    int defaultLimit = params.getInt(FacetParams.FACET_LIMIT, 100);
	    if (defaultLimit < 0) defaultLimit = Integer.MAX_VALUE;
	    
	    String[] fields = params.getParams(FacetParams.FACET_FIELD);
	    
	    String facetSortString = params.get(FacetParams.FACET_SORT);
	    
	    boolean defaultExpand = params.getBool(FACET_EXPAND, false);
	    
	    FacetSpec.FacetSortSpec defaultFacetSortSpec = parseFacetSort(facetSortString,FacetSpec.FacetSortSpec.OrderHitsDesc);
	    
	    
	    BrowseRequest br=new BrowseRequest();
	    br.setOffset(offset);
	    br.setCount(count);
	    br.setQuery(query);
	    
	    if (sort!=null){
		    SortField[] sortFields=sort.getSort();
		    if (sortFields!=null && sortFields.length>0){
		    	br.setSort(sortFields);
		    }
	    }

    	fillBoboSelections(br, params);
    	
	    if (params.getBool(FacetParams.FACET, false) && fields!=null){
	    	// filling facets
	    	for (String facetField : fields){
	    		FacetSpec fspec = new FacetSpec();
	    		br.setFacetSpec(facetField, fspec);
	    		
	    		fspec.setMinHitCount(params.getFieldInt(facetField, FacetParams.FACET_MINCOUNT,defaultMinCount));
	    		fspec.setMaxCount(params.getFieldInt(facetField, FacetParams.FACET_LIMIT,defaultLimit));
	    		fspec.setExpandSelection(params.getFieldBool(facetField, FACET_EXPAND, defaultExpand));
	    		FacetSpec.FacetSortSpec sortSpec = parseFacetSort(params.getFieldParam(facetField, FacetParams.FACET_SORT,null),defaultFacetSortSpec);
	    		fspec.setOrderBy(sortSpec);
	    	}	
	    }
	    return br;
	}
}
