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

package com.browseengine.bobo.impl;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Version;

public class QueryProducer{
	private static Logger logger=Logger.getLogger(QueryProducer.class);
	
	public static final String CONTENT_FIELD="contents";
	public static Query convert(String queryString,String defaultField) throws ParseException{
		if (queryString==null || queryString.length()==0){
			return null;
		}
		else{
			Analyzer analyzer=new StandardAnalyzer(Version.LUCENE_CURRENT);
			if (defaultField==null) defaultField="contents";
			return new QueryParser(Version.LUCENE_CURRENT,defaultField, analyzer).parse(queryString);
		}
	}

	final static SortField[] DEFAULT_SORT=new SortField[]{SortField.FIELD_SCORE};
	
//	public static SortField[] convertSort(SortField[] sortSpec,BoboIndexReader idxReader){
//		 SortField[] retVal=DEFAULT_SORT;
//		if (sortSpec!=null && sortSpec.length>0){
//			ArrayList<SortField> sortList=new ArrayList<SortField>(sortSpec.length+1);
//			boolean relevanceSortAdded=false;
//			for (int i=0;i<sortSpec.length;++i){
//			    if (SortField.FIELD_DOC.equals(sortSpec[i])){
//			      sortList.add(SortField.FIELD_DOC);
//			    }
//			    else if (SortField.FIELD_SCORE.equals(sortSpec[i])){
//			      sortList.add(SortField.FIELD_SCORE);
//			      relevanceSortAdded=true;
//			    }
//			    else{
//    			    String fieldname=sortSpec[i].getField();
//    			    if (fieldname!=null){
//    			      SortField sf=null;
//    			      final FacetHandler facetHandler=idxReader.getFacetHandler(fieldname);
//    			      if (facetHandler!=null){
//    			    	  sf=new SortField(fieldname.toLowerCase(),new SortComparatorSource(){
//
//							/**
//							 * 
//							 */
//							private static final long serialVersionUID = 1L;
//
//							public ScoreDocComparator newComparator(
//									IndexReader reader, String fieldname)
//									throws IOException {
//								return facetHandler.getScoreDocComparator();
//							}
//    			    		  
//    			    	  },sortSpec[i].getReverse());
//    			      }
//    			      else{
//    			    	  sf=sortSpec[i];
//    			      }
//    			      sortList.add(sf);
//    			    }
//			    }
//			}
//			if (!relevanceSortAdded){
//			  sortList.add(SortField.FIELD_SCORE);
//			}
//			retVal=sortList.toArray(new SortField[sortList.size()]);		
//		}
//		return retVal;
//	}
//	
//	public static DocIdSet buildBitSet( BrowseSelection[] selections,BoboIndexReader reader) throws IOException{
//		if (selections==null || selections.length == 0) return null;
//		DocIdSet finalBits=null;
//		FieldConfiguration fConf=reader.getFieldConfiguration();
//		FieldPlugin plugin;
//		DocIdSet finalNotBits=null;
//		
//        for(int i=0;i<selections.length;++i) {
//        	String fieldName=selections[i].getFieldName();
//        	plugin=fConf.getFieldPlugin(fieldName);
//        	
//        	if (plugin==null){
//        		throw new IOException("Undefined field: "+fieldName+" please check your field configuration.");
//        	}
//        	BoboFilter[] f=plugin.buildFilters(selections[i],false);        	
//        	DocIdSet bs=FieldPlugin.mergeSelectionBitset(reader, f, selections[i].getSelectionOperation());
//        	if (bs!=null){
//	        	if (finalBits==null){	        	
//	        			finalBits=bs;	        
//	        	}
//	        	else{
//	        		finalBits.and(bs);
//	        	}
//        	}        
//        	
//        	if (plugin.supportNotSelections()){
//	        	BoboFilter[] notF=plugin.buildFilters(selections[i],true);
//	        	DocIdSet notBS=FieldPlugin.mergeSelectionBitset(reader,notF, ValueOperation.ValueOperationOr);
//	        	
//	        	if (notBS!=null){
//	        		if (finalNotBits==null){
//	        			finalNotBits=notBS;
//	        		}
//	        		else{
//	        			finalNotBits.or(notBS);
//	        		}
//	        	}
//	        	/*
//	        	DocSet emptyVals=new TermFilter(new Term(fieldName,"")).getDocSet(reader);
//	        	if (emptyVals!=null && emptyVals.cardinality()>0 && finalNotBits!=null){
//	        		finalNotBits.or(emptyVals);
//	        	}*/
//        	}
//        }
//        
//        if (finalNotBits!=null && finalNotBits.cardinality()>0){		// we have "not" selections
//        	if (finalBits!=null){
//        		finalNotBits.flip(0, finalBits.length());
//        		finalBits.and(finalNotBits);
//        	}
//        	else{        		
//        		finalNotBits.flip(0,reader.maxDoc());
//        		finalBits=finalNotBits;
//        	}
//        }
//        
//        return finalBits;  
//    }

	public Query buildQuery(String query) throws ParseException{
		return convert(query, CONTENT_FIELD);
	}
}
