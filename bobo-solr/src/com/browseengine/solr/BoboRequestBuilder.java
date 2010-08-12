package com.browseengine.solr;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.params.SolrParams;

import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.BrowseSelection.ValueOperation;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;

public class BoboRequestBuilder {
	
	public static final String BOBO_PREFIX="bobo";
	public static final String BOBO_FIELD_SEL_PREFIX="selection";
	public static final String BOBO_FIELD_SEL_OP=BOBO_FIELD_SEL_PREFIX+".op";
	public static final String BOBO_FIELD_SEL_NOT=BOBO_FIELD_SEL_PREFIX+".not";
	public static final String BOBO_FACET_EXPAND="facet.expand"; 
	
	public static final Logger logger = Logger.getLogger(BoboRequestBuilder.class);
	
	public static void applyFacetExpand(SolrQuery params,String name,boolean expand){
		params.add("f."+BOBO_PREFIX+"."+name+"."+BOBO_FACET_EXPAND, String.valueOf(expand));
	}
	
	public static void applySelectionOperation(SolrQuery params,String name,ValueOperation op){
		String val;
		if (ValueOperation.ValueOperationAnd.equals(op)){
			val="and";
		}
		else{
			val = "or";
		}
		params.add("f."+BOBO_PREFIX+"."+name+"."+BOBO_FIELD_SEL_OP, val);
	}
	
	public static ValueOperation getSelectionOperation(SolrParams params,String name) throws BrowseException{
		String selop = getBoboParam(params,name,BOBO_FIELD_SEL_OP);
		if (selop!=null){
			if ("and".equals(selop)){
				return ValueOperation.ValueOperationAnd;
			}
			else if ("or".equals(selop)){
				return ValueOperation.ValueOperationOr;
			}
			else{
				throw new BrowseException(name+": selection operation: "+selop+" not supported");
			}
		}
		else{
			return ValueOperation.ValueOperationOr;
		}
	}
	
	public static boolean isFacetExpand(SolrParams params,String facetField){
		return getBoboParamBool(params,facetField,BOBO_FACET_EXPAND,false);
	}
	
	public static void applySelectionNotValues(SolrQuery params,String name,String... notvalues){
		params.add("f."+BOBO_PREFIX+"."+name+"."+BOBO_FIELD_SEL_NOT, notvalues);
	}
	
	public static String[] getSelectionNotValues(SolrParams params,String name){
		return getBoboParams(params,name,BOBO_FIELD_SEL_NOT);
	}
	
	public static void applySelectionProperties(SolrQuery params,String name,Map<String,String> props){
		if (props!=null && props.size()>0){
			Set<Entry<String,String>> entries = props.entrySet();
			String[] propvals = new String[entries.size()];
			int i = 0;
			for (Entry<String,String> entry : entries){
				String val = entry.getKey()+":"+entry.getValue();
				propvals[i++]=val;
			}
			params.add("f."+BOBO_PREFIX+"."+name+"."+BOBO_FIELD_SEL_PREFIX+".prop", propvals);
		}
	}
	
	public static Map<String,String> getSelectionProperties(SolrParams params,String name){
		return getBoboParamProps(params,name,BOBO_FIELD_SEL_PREFIX);
	}
	
	
	
	private static String[] getBoboParams(SolrParams solrParams,String field,String param){
		return solrParams.getFieldParams(BOBO_PREFIX+"."+field, param);
	}
	
	private static String getBoboParam(SolrParams solrParams,String field,String param){
		return solrParams.getFieldParam(BOBO_PREFIX+"."+field, param);
	}
	
	private static boolean getBoboParamBool(SolrParams solrParams,String field,String param,boolean defaultBool){
		return solrParams.getFieldBool(BOBO_PREFIX+"."+field, param,defaultBool);
	}
	
	private static Map<String,String> getBoboParamProps(SolrParams solrParams,String field,String name){
		HashMap<String,String> propMap = new HashMap<String,String>();
		String[] props = getBoboParams(solrParams,field,name+".prop");
		if (props!=null && props.length>0){
			for (String prop : props){
				String[] parts = prop.split(":");
				if (parts.length==2){
					propMap.put(parts[0], parts[1]);
				}
			}
		}
		return propMap;
	}
	
	
	private static void fillBoboSelections(BrowseRequest req,SolrParams params) throws BrowseException{
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
	    			
	    			sel.setSelectionOperation(getSelectionOperation(params,name));
	    			
	    			String[] selNot = getSelectionNotValues(params,name);
	    			if (selNot!=null && selNot.length>0){
	    				sel.setNotValues(selNot);
	    			}
	    			
	    			Map<String,String> propMaps = getSelectionProperties(params,name);
	    			if (propMaps!=null && propMaps.size()>0){
	    				sel.setSelectionProperties(propMaps);
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
	
	public static BrowseRequest buildRequest(SolrParams params,Query query,Sort sort) throws BrowseException{
	    int offset=params.getInt(CommonParams.START, 0);
	    int count=params.getInt(CommonParams.ROWS, 10);
	    
	    int defaultMinCount = params.getInt(FacetParams.FACET_MINCOUNT, 0);
	    
	    int defaultLimit = params.getInt(FacetParams.FACET_LIMIT, 100);
	    if (defaultLimit < 0) defaultLimit = Integer.MAX_VALUE;
	    
	    String[] fields = params.getParams(FacetParams.FACET_FIELD);
	    
	    String facetSortString = params.get(FacetParams.FACET_SORT);
	    
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
	    		fspec.setExpandSelection(isFacetExpand(params,facetField));
	    		FacetSpec.FacetSortSpec sortSpec = parseFacetSort(params.getFieldParam(facetField, FacetParams.FACET_SORT,null),defaultFacetSortSpec);
	    		fspec.setOrderBy(sortSpec);
	    	}	
	    }
	    return br;
	}
}
