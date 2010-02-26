package com.browseengine.bobo.server.protocol;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;

public class BoboRequestBuilder {
	
	public static final String OSPEC_PREFIX="bobo.groupby.";
	public static final String SEL_PREFIX="bobo.sel.";
	public static final String BOBO_PREFIX="bobo.";
	
	public static final String QUERY="q";
	public static final String DEFAULT_FIELD="df";
	
	public static final String START="start";
	public static final String COUNT="rows";
	
	public static final String SORT="sort";
	public static final Logger logger = Logger.getLogger(BoboRequestBuilder.class);
	
	private static void fillBoboRequest(BrowseRequest req,BoboParams params){
		Iterator<String> names=params.getParamNames();
		HashMap<String,BrowseSelection> selMap=new HashMap<String,BrowseSelection>();
		
		while(names.hasNext()){
			
			String paramName=names.next();
			if (paramName.startsWith(BOBO_PREFIX)){
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
							String[] vals=params.getStrings(paramName);
							if (vals!=null && vals.length>0){
							  for (String val : vals)
							  {
								sel.addValue(val);
							  }
							}
						}
						else if ("notval".equals(type)){
							String val=params.getString(paramName, null);
							if (val!=null && val.length()>0){
								sel.addNotValue(val);
							}
						}
						else if ("operation".equals(type)){
							String order=params.getString(paramName, "or");
							if ("or".equals(order)){
								sel.setSelectionOperation(BrowseSelection.ValueOperation.ValueOperationOr);
							}
							else if ("and".equals(order)){
								sel.setSelectionOperation(BrowseSelection.ValueOperation.ValueOperationAnd);
							}
						}
						else if ("depth".equals(type)){
							sel.setDepth(params.getInt(paramName, 0));
						}
						else if ("strict".equals(type)){
							sel.setStrict(params.getBool(paramName, false));
						}
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
				else if (paramName.startsWith(OSPEC_PREFIX)){
					try{
						int index=OSPEC_PREFIX.length();
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
						FacetSpec facetSpec=req.getFacetSpec(name);
						if (facetSpec==null){
							facetSpec=new FacetSpec();
							req.setFacetSpec(name, facetSpec);
						}
						if ("expand".equals(type)){
							facetSpec.setExpandSelection(params.getBool(paramName, false));
						}
						else if ("max".equals(type)){
							facetSpec.setMaxCount(params.getInt(paramName, 0));
						}
						else if ("orderby".equals(type)){
							String order=params.getString(paramName, "value");
							if ("hits".equals(order)){
								facetSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
							}
							else if ("value".equals(order)){
								facetSpec.setOrderBy(FacetSortSpec.OrderValueAsc);
							}
						}
						else if ("mincount".equals(type)){
							facetSpec.setMinHitCount(params.getInt(paramName, 1));
						}
					}
					catch(Exception e){
						logger.error(e.getMessage(),e);
					}
				}
			}
		}
		
		Iterator<BrowseSelection> iter=selMap.values().iterator();
		while(iter.hasNext()){
			req.addSelection(iter.next());
		}
	}
	
	public static BrowseRequest buildRequest(BoboParams params,BoboQueryBuilder qb){

	    int offset=params.getInt(START, 0);
	  
	    int count=params.getInt(COUNT, 10);
	    
	      
		Sort sort = qb.parseSort(params.getString(SORT));

	    // parse the query from the 'q' parameter (sort has been striped)
	    Query query = null;
	    
	    String qString = params.get(QUERY);
	    if (qString != null && qString.length() > 0){
	    	query = qb.parseQuery(qString, params.getString(DEFAULT_FIELD));
	    }
	    
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
	    
	    fillBoboRequest(br, params);
	    return br;
	}
}
