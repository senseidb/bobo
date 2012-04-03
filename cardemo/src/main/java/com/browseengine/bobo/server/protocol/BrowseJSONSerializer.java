/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  John Wang
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * send mail to owner@browseengine.com.
 */

package com.browseengine.bobo.server.protocol;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;

public class BrowseJSONSerializer {

	public static final short Selection_Type_Simple=0;
	public static final short Selection_Type_Path=1;
	public static final short Selection_Type_Range=2;
	
	public static final short Operation_Type_Or=0;
	public static final short Operation_Type_And=1;
	
	public BrowseJSONSerializer() {
		super();
	}

	public static String serialize(BrowseRequest req){
		return null;
	}
	
	/**
	 * TODO: need to add support for multiple values.
	 * @param doc
	 * @return
	 * @throws JSONException 
	 */
	public static JSONObject serializeValues(Map<String,String[]> values) throws JSONException{
		JSONObject obj=new JSONObject();
		Iterator<String> iter=values.keySet().iterator();
		while(iter.hasNext())
		{
			String name=iter.next();
			String[] vals = values.get(name);
			if (vals.length>0)
			{
				obj.put(name, vals[0]);
			}
		}		
		return obj;
	}
	
	public static JSONObject serializeHits(BrowseHit hit) throws JSONException{
		JSONObject obj=new JSONObject();
		obj.put("doc",serializeValues(hit.getFieldValues()));
		obj.put("docid", hit.getDocid());
		obj.put("score", hit.getScore());
		return obj;
	}
	
	public static String serialize(BrowseResult result) throws JSONException{
		JSONObject obj=new JSONObject();
		if (result!=null){
			obj.put("time",((double)result.getTime())/1000.0);			
			obj.put("hitCount",result.getNumHits());
			obj.put("totalDocs",result.getTotalDocs());
			
			// serialize choices
			JSONObject choices=new JSONObject();			
			Set<Entry<String,FacetAccessible>> facetAccessors=result.getFacetMap().entrySet();
			for (Entry<String,FacetAccessible> entry : facetAccessors){
				JSONObject choiceObject=new JSONObject();				
				JSONArray choiceValArray=new JSONArray();
				
				choiceObject.put("choicelist",choiceValArray);
				int k=0;
				
				String name = entry.getKey();
				FacetAccessible facets = entry.getValue();
				
				List<BrowseFacet> facetList = facets.getFacets();
				for (BrowseFacet facet : facetList)
				{
					JSONObject choice=new JSONObject();
					choice.put("val",facet.getValue());
					choice.put("hits",facet.getHitCount());
					choiceValArray.put(k++,choice);
				}
				choices.put(name,choiceObject);
			}
			obj.put("choices",choices);
			
			JSONArray hitsArray=new JSONArray();
			BrowseHit[] hits=result.getHits();
			if (hits!=null && hits.length>0){
				for (int i=0;i<hits.length;++i){
					hitsArray.put(i,serializeHits(hits[i]));
				}				
			}
			obj.put("hits",hitsArray);			
			// serialize documents
		}
		return obj.toString();
	}
	
	/*private static String serializeFieldConfiguration(FieldConfiguration fConf){
		JSONObject obj=new JSONObject();
		if (fConf!=null){
			String[] fieldNames=fConf.getFieldNames();
			JSONArray fieldNameArray=new JSONArray();			
			for (int i=0;i<fieldNames.length;++i){				
				String field=fieldNames[i];			
				fieldNameArray=fieldNameArray.put(field);
				Properties props=fConf.getFieldProperties(field);				
				JSONObject fieldData=new JSONObject();
				
				Iterator keys=props.keySet().iterator();
				while(keys.hasNext()){
					String name=(String)keys.next();
					String val=props.getProperty(name);
					fieldData.put(name,val);
				}
				obj.put(field,fieldData);
			}
			obj.put("fieldnames",fieldNameArray);
		}
		return obj.toString();
	}*/
	
	/*
	
	public static BrowseRequest buildBrowseRequest(HttpServletRequest req){
		BrowseRequest br=new BrowseRequest();
		return br;
	}
	
	public static BrowseRequest buildBrowseRequest(String reqString) throws ParseException{
		
		BrowseRequest br=new BrowseRequest();
		JSONObject reqObj=null;
		try {
			reqObj=new JSONObject(reqString);
		} catch (java.text.ParseException e) {
			throw new ParseException(e.getMessage());
		}
		JSONObject serObj=reqObj.getJSONObject("browsereq");
		int numPerPage=serObj.getInt("numPerPage");
		int offset=serObj.getInt("offset");
		String queryString=serObj.getString("queryString");
		
		br.setCount(numPerPage);
		br.setOffset(offset);
		
		br.setQuery(QueryProducer.convert(queryString,QueryProducer.CONTENT_FIELD));
		Iterator keys=null;
		if (!reqObj.isNull("outputspec")){
			JSONObject outputSpecs=reqObj.getJSONObject("outputspec");					
			keys=outputSpecs.keys();
			
			while(keys.hasNext()){
				String key=(String)keys.next();
				JSONObject outputSpec=outputSpecs.getJSONObject(key);
				OutputSpec o=new OutputSpec();
				o.setOrderBy((short)(outputSpec.getInt("order")));
				o.setMaxCount(outputSpec.getInt("max"));
				o.setExpandSelection(outputSpec.getBoolean("expandSelection"));
				br.setOutputSpec(key,o);
			}
		}
		
		JSONObject selections=serObj.getJSONObject("selections");
		keys=selections.keys();
		
		while(keys.hasNext()){
			String key=(String)keys.next();
			JSONObject selectionObj=selections.getJSONObject(key);
			String type=selectionObj.getString("type");
			short operation=(short)selectionObj.getInt("operation");			
			
			JSONArray selArray=selectionObj.getJSONArray("values");
			int count=selArray.length();
			

			
			FieldPlugin plugin;
			try {
				plugin = (FieldPlugin) FieldRegistry.getInstance().getFieldPlugin(type).newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(),e);
			}
			
			ArrayList<String> list=new ArrayList<String>();
			
			if ("simple".equals(type)){
				for (int i=0;i<count;++i){
					String strVal=selArray.getString(i);
					if (strVal!=null){
						strVal=strVal.trim();
						if (strVal.length()>0){
							list.add(strVal);
						}
					}								
				}		
			}
			else if ("path".equals(type)){
				if (count>0){
					// only take the first one
					String pathVal=selArray.getString(0);
					list.add(pathVal);
				}	
			}
			else if("range".equals(type)){				
				if (count>0){
					// only take the first one
					String rangeVal=selArray.getString(0);
					String[] parts=rangeVal.split("\\|");
					if (parts==null || parts.length!=2){
						if (parts.length==1){
							list.add(parts[0]);
						}
						else{
							throw new ParseException("Unable to parse range: "+rangeVal);
						}
					}
					else{
						if (parts[0]!=null){
							list.add(parts[0]);
						}
						if (parts[1]!=null){
							list.add(parts[1]);
						}
					}
				}		
			}				
				
			String[] vals=list.toArray(new String[list.size()]);

			BrowseSelection sel=plugin.buildSelection(key, vals);
			if (sel!=null){
				sel.setDepth(selectionObj.getInt("depth"));
				sel.setStrict(selectionObj.getBoolean("strict"));
				br.addSelection(sel);
				
				if (operation==Operation_Type_And){
					sel.setSelectionOperation(ValueOperation.ValueOperationAnd);
				}
				else if (operation==Operation_Type_Or){
					sel.setSelectionOperation(ValueOperation.ValueOperationOr);
				}
			}
		}
		
		JSONArray sortSpecs=serObj.getJSONArray("sortOrder");
		for (int i=0;i<sortSpecs.length();++i){
			JSONObject sortSpec=sortSpecs.getJSONObject(i);
			String key=sortSpec.getString("field");
			boolean reverse=sortSpec.getBoolean("reverse");
			SortField sf=new SortField(key,reverse);
			br.addSortField(sf);
		}				
		return br;
	}*/
}
