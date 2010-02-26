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

package com.browseengine.bobo.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Payload;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboCustomSortField;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.Browsable;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FieldValueAccessor;
import com.browseengine.bobo.api.MultiBoboBrowser;
import com.browseengine.bobo.api.BrowseSelection.ValueOperation;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.client.GeoExample;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandler.TermCountSize;
import com.browseengine.bobo.facets.data.PredefinedTermListFactory;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.impl.CompactMultiValueFacetHandler;
import com.browseengine.bobo.facets.impl.FacetHitcountComparatorFactory;
import com.browseengine.bobo.facets.impl.FacetValueComparatorFactory;
import com.browseengine.bobo.facets.impl.FilteredRangeFacetHandler;
import com.browseengine.bobo.facets.impl.MultiValueFacetHandler;
import com.browseengine.bobo.facets.impl.PathFacetHandler;
import com.browseengine.bobo.facets.impl.RangeFacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;
import com.browseengine.bobo.facets.impl.SimpleGroupbyFacetHandler;
import com.browseengine.bobo.index.BoboIndexer;
import com.browseengine.bobo.index.digest.DataDigester;
import com.browseengine.bobo.query.scoring.FacetTermQuery;
import com.browseengine.bobo.sort.DocComparator;
import com.browseengine.bobo.sort.DocComparatorSource;

public class BoboTestCase extends TestCase {
	private Directory _indexDir;
	private List<FacetHandler<?>> _fconf;
	static final private Term tagSizePayloadTerm = new Term("tagSizePayload", "size");
	
	private static class TestDataDigester extends DataDigester {
		private List<FacetHandler<?>> _fconf;
		private Document[] _data;
		TestDataDigester(List<FacetHandler<?>> fConf,Document[] data){
			super();
			_fconf=fConf;
			_data=data;
		}
		@Override
		public void digest(DataHandler handler) throws IOException {
			for (int i=0;i<_data.length;++i){
				handler.handleDocument(_data[i]);
			}
		}
	}
	
	public BoboTestCase(String testName){
		super(testName);
		_fconf=buildFieldConf();
		_indexDir=createIndex();
	}
	

	private BoboIndexReader newIndexReader() throws IOException{
		return newIndexReader(true);
	}
	
	private BoboIndexReader newIndexReader(boolean readonly) throws IOException{
	  IndexReader srcReader=IndexReader.open(_indexDir,readonly);
      try{
        BoboIndexReader reader= BoboIndexReader.getInstance(srcReader,_fconf);
        return reader;
      }
      catch(IOException ioe){
        if (srcReader!=null){
          srcReader.close();
        }
        throw ioe;
      }
	}

	private BoboBrowser newBrowser() throws IOException{
	  return new BoboBrowser(newIndexReader());
	}
		
	public static Field buildMetaField(String name,String val)
	{
	  Field f = new Field(name,val,Field.Store.NO,Index.NOT_ANALYZED_NO_NORMS);
	  f.setOmitTermFreqAndPositions(true);
	  return f;
	}
	
	static class MetaTokenStream extends TokenStream {
        private boolean returnToken = false;

        private PayloadAttribute payloadAttr;
        private TermAttribute termAttr;
        MetaTokenStream(Term term,int size) {
          byte[] buffer = new byte[4];
          buffer[0] = (byte) (size);
          buffer[1] = (byte) (size >> 8);
          buffer[2] = (byte) (size >> 16);
          buffer[3] = (byte) (size >> 24);
          payloadAttr = (PayloadAttribute)addAttribute(PayloadAttribute.class);
          payloadAttr.setPayload(new Payload(buffer));
          termAttr = (TermAttribute)addAttribute(TermAttribute.class);
          termAttr.setTermBuffer(term.text());
          returnToken = true;
        }

        @Override
		public boolean incrementToken() throws IOException {
        	if (returnToken) {
                returnToken = false;
                return true;
              } else {
                return false;
              }
		}  
    }
	
    public static Field buildMetaSizePayloadField(final Term term, final int size)
    {
      Field f = new Field(term.field(), new MetaTokenStream(term,size));
      return f;
    }
	
	
	public static Document[] buildData(){
		ArrayList<Document> dataList=new ArrayList<Document>();
		
		Document d1=new Document();
		d1.add(buildMetaField("id","1"));
		d1.add(buildMetaField("shape","square"));
		d1.add(buildMetaField("color","red"));
		d1.add(buildMetaField("size","4"));
		d1.add(buildMetaField("location","toy/lego/block/"));
		d1.add(buildMetaField("tag","rabbit"));
		d1.add(buildMetaField("tag","pet"));
        d1.add(buildMetaField("tag","animal")); 
        d1.add(buildMetaSizePayloadField(tagSizePayloadTerm,3));
		d1.add(buildMetaField("number","0010"));
		d1.add(buildMetaField("date","2000/01/01"));
		d1.add(buildMetaField("name","ken"));
		d1.add(buildMetaField("char","k"));
		d1.add(buildMetaField("multinum","001"));
		d1.add(buildMetaField("multinum","003"));
		d1.add(buildMetaField("compactnum","001"));
		d1.add(buildMetaField("compactnum","003"));
		d1.add(buildMetaField("numendorsers","000003"));
		d1.add(buildMetaField("path","a-b"));
		d1.add(buildMetaField("multipath","a-b"));
		d1.add(buildMetaField("custom","000003"));
		d1.add(buildMetaField("latitude", "60"));
		d1.add(buildMetaField("longitude", "120"));
		
		Document d2=new Document();
		d2.add(buildMetaField("id","2"));
		d2.add(buildMetaField("shape","rectangle"));
		d2.add(buildMetaField("color","red"));
		d2.add(buildMetaField("size","2"));
		d2.add(buildMetaField("location","toy/lego/block/"));
		d2.add(buildMetaField("tag","dog"));
		d2.add(buildMetaField("tag","pet"));
		d2.add(buildMetaField("tag","poodle"));
        d2.add(buildMetaSizePayloadField(tagSizePayloadTerm,3));
		d2.add(buildMetaField("number","0011"));
		d2.add(buildMetaField("date","2003/02/14"));
		d2.add(buildMetaField("name","igor"));
		d2.add(buildMetaField("char","i"));
		d2.add(buildMetaField("multinum","002"));
		d2.add(buildMetaField("multinum","004"));
		d2.add(buildMetaField("compactnum","002"));
		d2.add(buildMetaField("compactnum","004"));
		d2.add(buildMetaField("numendorsers","000010"));
		d2.add(buildMetaField("path","a-c-d"));
		d2.add(buildMetaField("multipath","a-c-d"));
		d2.add(buildMetaField("multipath","a-b"));
		d2.add(buildMetaField("custom","000010"));
		d2.add(buildMetaField("latitude", "50"));
		d2.add(buildMetaField("longitude", "110"));
		
		Document d3=new Document();
		d3.add(buildMetaField("id","3"));
		d3.add(buildMetaField("shape","circle"));
		d3.add(buildMetaField("color","green"));
		d3.add(buildMetaField("size","3"));
		d3.add(buildMetaField("location","toy/lego/"));
		d3.add(buildMetaField("tag","rabbit"));
		d3.add(buildMetaField("tag","cartoon"));
		d3.add(buildMetaField("tag","funny"));	
        d3.add(buildMetaSizePayloadField(tagSizePayloadTerm,3));
		d3.add(buildMetaField("number","0230"));
		d3.add(buildMetaField("date","2001/12/25"));
		d3.add(buildMetaField("name","john"));
		d3.add(buildMetaField("char","j"));
		d3.add(buildMetaField("multinum","007"));
		d3.add(buildMetaField("multinum","012"));
		d3.add(buildMetaField("compactnum","007"));
		d3.add(buildMetaField("compactnum","012"));
		d3.add(buildMetaField("numendorsers","000015"));
		d3.add(buildMetaField("path","a-e"));
		d3.add(buildMetaField("multipath","a-e"));
		d3.add(buildMetaField("multipath","a-b"));
		d3.add(buildMetaField("custom","000015"));
		d3.add(buildMetaField("latitude", "35"));
		d3.add(buildMetaField("longitude", "70"));
		
		Document d4=new Document();
		d4.add(buildMetaField("id","4"));
		d4.add(buildMetaField("shape","circle"));
		d4.add(buildMetaField("color","blue"));
		d4.add(buildMetaField("size","1"));
		d4.add(buildMetaField("location","toy/"));
		d4.add(buildMetaField("tag","store"));
		d4.add(buildMetaField("tag","pet"));
		d4.add(buildMetaField("tag","animal"));		
        d4.add(buildMetaSizePayloadField(tagSizePayloadTerm,3));
		d4.add(buildMetaField("number","0913"));
		d4.add(buildMetaField("date","2004/11/24"));
		d4.add(buildMetaField("name","cathy"));
		d4.add(buildMetaField("char","c"));
		d4.add(buildMetaField("multinum","007"));
		d4.add(buildMetaField("multinum","007"));
		d4.add(buildMetaField("compactnum","007"));
		d4.add(buildMetaField("numendorsers","000019"));
		d4.add(buildMetaField("path","a-c"));
		d4.add(buildMetaField("multipath","a-c"));
		d4.add(buildMetaField("multipath","a-b"));
		d4.add(buildMetaField("custom","000019"));
		d4.add(buildMetaField("latitude", "30"));
		d4.add(buildMetaField("longitude", "75"));
		
		Document d5=new Document();
		d5.add(buildMetaField("id","5"));
		d5.add(buildMetaField("shape","square"));
		d5.add(buildMetaField("color","blue"));
		d5.add(buildMetaField("size","5"));
		d5.add(buildMetaField("location","toy/lego/"));
		d5.add(buildMetaField("tag","cartoon"));
		d5.add(buildMetaField("tag","funny"));
		d5.add(buildMetaField("tag","disney"));	
        d5.add(buildMetaSizePayloadField(tagSizePayloadTerm,3));
		d5.add(buildMetaField("number","1013"));
		d5.add(buildMetaField("date","2002/03/08"));
		d5.add(buildMetaField("name","mike"));
		d5.add(buildMetaField("char","m"));
		d5.add(buildMetaField("multinum","001"));
		d5.add(buildMetaField("multinum","001"));
		d5.add(buildMetaField("compactnum","001"));
		d5.add(buildMetaField("compactnum","001"));
		d5.add(buildMetaField("numendorsers","000002"));
		d5.add(buildMetaField("path","a-e-f"));
		d5.add(buildMetaField("multipath","a-e-f"));
		d5.add(buildMetaField("multipath","a-b"));
		d5.add(buildMetaField("custom","000002"));
		d5.add(buildMetaField("latitude", "60"));
		d5.add(buildMetaField("longitude", "120"));
		
		Document d6=new Document();
		d6.add(buildMetaField("id","6"));
		d6.add(buildMetaField("shape","rectangle"));
		d6.add(buildMetaField("color","green"));
		d6.add(buildMetaField("size","6"));
		d6.add(buildMetaField("location","toy/lego/block/"));
		d6.add(buildMetaField("tag","funny"));
		d6.add(buildMetaField("tag","humor"));
		d6.add(buildMetaField("tag","joke"));		
        d6.add(buildMetaSizePayloadField(tagSizePayloadTerm,3));
		d6.add(buildMetaField("number","2130"));
		d6.add(buildMetaField("date","2007/08/01"));
		d6.add(buildMetaField("name","doug"));
		d6.add(buildMetaField("char","d"));
		d6.add(buildMetaField("multinum","001"));
		d6.add(buildMetaField("multinum","002"));
		d6.add(buildMetaField("multinum","003"));
		d6.add(buildMetaField("compactnum","001"));
		d6.add(buildMetaField("compactnum","002"));
		d6.add(buildMetaField("compactnum","003"));
		d6.add(buildMetaField("numendorsers","000009"));
		d6.add(buildMetaField("path","a-c-d"));
		d6.add(buildMetaField("multipath","a-c-d"));
		d6.add(buildMetaField("multipath","a-b"));
		d6.add(buildMetaField("custom","000009"));
		d6.add(buildMetaField("latitude", "80"));
		d6.add(buildMetaField("longitude", "-90"));
		
		Document d7=new Document();
		d7.add(buildMetaField("id","7"));
		d7.add(buildMetaField("shape","square"));
		d7.add(buildMetaField("color","red"));
		d7.add(buildMetaField("size","7"));
		d7.add(buildMetaField("location","toy/lego/"));
		d7.add(buildMetaField("tag","humane"));
		d7.add(buildMetaField("tag","dog"));
		d7.add(buildMetaField("tag","rabbit"));	
        d7.add(buildMetaSizePayloadField(tagSizePayloadTerm,3));
		d7.add(buildMetaField("number","0005"));
		d7.add(buildMetaField("date","2006/06/01"));
		d7.add(buildMetaField("name","abe"));
		d7.add(buildMetaField("char","a"));
		d7.add(buildMetaField("multinum","008"));
		d7.add(buildMetaField("multinum","003"));
		d7.add(buildMetaField("compactnum","008"));
		d7.add(buildMetaField("compactnum","003"));
		d7.add(buildMetaField("numendorsers","000013"));
		d7.add(buildMetaField("path","a-c"));
		d7.add(buildMetaField("multipath","a-c"));
		d7.add(buildMetaField("multipath","a-b"));
		d7.add(buildMetaField("custom","000013"));
		d7.add(buildMetaField("latitude", "70"));
		d7.add(buildMetaField("longitude", "-60"));
		
		Document d8 = new Document();
		d8.add(buildMetaField("latitude", "35"));
		d8.add(buildMetaField("longitude", "120"));
		
		dataList.add(d1);
		dataList.add(d2);
		dataList.add(d3);
		dataList.add(d4);
		dataList.add(d5);
		dataList.add(d6);
		dataList.add(d7);
		dataList.add(d7);
		
		
		return dataList.toArray(new Document[dataList.size()]);
	}
	
	private Directory createIndex(){
		RAMDirectory idxDir=new RAMDirectory();
		
		try {
			Document[] data=buildData();
			
			TestDataDigester testDigester=new TestDataDigester(_fconf,data);
			BoboIndexer indexer=new BoboIndexer(testDigester,idxDir);
			indexer.index();
			IndexReader r = IndexReader.open(idxDir,false);
			r.deleteDocument(r.maxDoc() - 1);
			//r.flush();
			r.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return idxDir;
		
	}
	
	public static List<FacetHandler<?>> buildFieldConf(){
		List<FacetHandler<?>> facetHandlers = new ArrayList<FacetHandler<?>>();
		facetHandlers.add(new SimpleFacetHandler("id"));
		SimpleFacetHandler colorHandler = new SimpleFacetHandler("color");
		colorHandler.setTermCountSize(TermCountSize.small);
		facetHandlers.add(colorHandler);

		SimpleFacetHandler shapeHandler = new SimpleFacetHandler("shape");
		colorHandler.setTermCountSize(TermCountSize.medium);
		facetHandlers.add(new SimpleFacetHandler("shape"));
		facetHandlers.add(new RangeFacetHandler("size", Arrays.asList(new String[]{"[* TO 4]", "[5 TO 8]", "[9 TO *]"})));
		String[] ranges = new String[]{"[000000 TO 000005]", "[000006 TO 000010]", "[000011 TO 000020]"};
		facetHandlers.add(new RangeFacetHandler("numendorsers", new PredefinedTermListFactory(Integer.class, "000000"), Arrays.asList(ranges)));
		
		PredefinedTermListFactory numTermFactory = new PredefinedTermListFactory(Integer.class, "0000");

		facetHandlers.add(new PathFacetHandler("location"));
		
		PathFacetHandler pathHandler = new PathFacetHandler("path");
		pathHandler.setSeparator("-");
		facetHandlers.add(pathHandler);
		

		PathFacetHandler multipathHandler = new PathFacetHandler("multipath",true);
		multipathHandler.setSeparator("-");
		facetHandlers.add(multipathHandler);
		
		facetHandlers.add(new SimpleFacetHandler("number", numTermFactory));
		facetHandlers.add(new RangeFacetHandler("date", new PredefinedTermListFactory(Date.class, "yyyy/MM/dd"), Arrays.asList(new String[]{"[2000/01/01 TO 2003/05/05]", "[2003/05/06 TO 2005/04/04]"})));
		facetHandlers.add(new SimpleFacetHandler("char", (TermListFactory)null));
		facetHandlers.add(new MultiValueFacetHandler("tag", (String)null, (TermListFactory)null, tagSizePayloadTerm));
		facetHandlers.add(new MultiValueFacetHandler("multinum", new PredefinedTermListFactory(Integer.class, "000")));
		facetHandlers.add(new CompactMultiValueFacetHandler("compactnum", new PredefinedTermListFactory(Integer.class, "000")));
		facetHandlers.add(new SimpleFacetHandler("storenum", new PredefinedTermListFactory(Long.class, null)));
		/* New FacetHandler for geographic locations. Depends on two RangeFacetHandlers on latitude and longitude */
		facetHandlers.add(new RangeFacetHandler("latitude", Arrays.asList(new String[]{"[* TO 30]", "[35 TO 60]", "[70 TO 120]"})));
		facetHandlers.add(new RangeFacetHandler("longitude", Arrays.asList(new String[]{"[* TO 30]", "[35 TO 60]", "[70 TO 120]"})));
		facetHandlers.add(new GeoExample("distance", "latitude", "longitude"));
		
		LinkedHashSet<String> dependsNames=new LinkedHashSet<String>();
		dependsNames.add("color");
		dependsNames.add("shape");
		dependsNames.add("number");
		facetHandlers.add(new SimpleGroupbyFacetHandler("groupby", dependsNames));
	    		
		return facetHandlers;
	}
	
	private static boolean check(BrowseResult res,int numHits,HashMap<String,List<BrowseFacet>> choiceMap,String[] ids){
		boolean match=false;
		if (numHits==res.getNumHits()){
		    if (choiceMap!=null){
    			Set<Entry<String,FacetAccessible>> entries=res.getFacetMap().entrySet();
    			
    			if (entries.size() == choiceMap.size()){
    				for (Entry<String,FacetAccessible> entry : entries){
    					String name = entry.getKey();
    					FacetAccessible c1 = entry.getValue();
    					List<BrowseFacet> l1 = c1.getFacets();
    					List<BrowseFacet> l2 =choiceMap.get(name);
    					
    					if (l1.size() == l2.size())
    					{
    						Iterator<BrowseFacet> iter1 = l1.iterator();
    						Iterator<BrowseFacet> iter2 = l2.iterator();
    						while(iter1.hasNext())
    						{
    							if (!iter1.next().equals(iter2.next()))
    							{
    								return false;
    							}
    						}
    						match = true;
    					}
    					else
    					{
    						return false;
    					}
    				}
    			}
    			else
    			{
    			  return false;
    			}
		    }
			if (ids!=null)
			{
			  BrowseHit[] hits=res.getHits();
			  try{
			      if (hits.length!=ids.length) return false;
    			  for (int i=0;i<hits.length;++i)
    			  {
    			    String id=hits[i].getField("id");
    			    if (!ids[i].equals(id)) return false;
    			  }
			  }
			  catch(Exception e)
			  {
			    return false;
			  }
			}
			match=true; 
		}
		return match;
	}
	
	
	
	
	private void doTest(BrowseResult result,BrowseRequest req,int numHits,HashMap<String,List<BrowseFacet>> choiceMap,String[] ids){
			if (!check(result,numHits,choiceMap,ids)){
				StringBuilder buffer=new StringBuilder();
				buffer.append("Test: ").append(getName()).append("\n");
				buffer.append("Result check failed: \n");
				buffer.append("expected: \n");
				buffer.append(numHits).append(" hits\n");
				buffer.append(choiceMap).append('\n');
				buffer.append(Arrays.toString(ids)).append('\n');
				buffer.append("gotten: \n");
				buffer.append(result.getNumHits()).append(" hits\n");
				

				Map<String,FacetAccessible> map=result.getFacetMap();
				
				Set<Entry<String,FacetAccessible>> entries = map.entrySet();
				
				buffer.append("{");
				for (Entry<String,FacetAccessible> entry : entries)
				{
					String name = entry.getKey();
					FacetAccessible facetAccessor = entry.getValue();
					buffer.append("name=").append(name).append(",");
					buffer.append("facets=").append(facetAccessor.getFacets()).append(";");
				}
				buffer.append("}").append('\n');
				
				BrowseHit[] hits=result.getHits();
				for (int i=0;i<hits.length;++i){
					if (i!=0){
						buffer.append('\n');
					}
					buffer.append(hits[i]);
				}
				fail(buffer.toString());
			}
	}
	
	public static String toString(Map<String,FacetAccessible> map) {
		StringBuilder buffer=new StringBuilder();
		Set<Entry<String,FacetAccessible>> entries = map.entrySet();
		
		buffer.append("{");
		for (Entry<String,FacetAccessible> entry : entries)
		{
			String name = entry.getKey();
			FacetAccessible facetAccessor = entry.getValue();
			buffer.append("name=").append(name).append(",");
			buffer.append("facets=").append(facetAccessor.getFacets()).append(";");
		}
		buffer.append("}").append('\n');
		return buffer.toString();
	}
	
	public void testExpandSelection()
	{
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);

        BrowseSelection sel=new BrowseSelection("color");
        sel.addValue("red");
        br.addSelection(sel); 
        
		
		FacetSpec output=new FacetSpec();
		output.setExpandSelection(true);
		br.setFacetSpec("color", output);
		br.setFacetSpec("shape", output);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
        answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("blue",2),new BrowseFacet("green",2),new BrowseFacet("red",3)}));
        answer.put("shape", Arrays.asList(new BrowseFacet[]{new BrowseFacet("rectangle",1),new BrowseFacet("square",2)}));
        
        doTest(br,3,answer,new String[]{"1","2","7"});

        sel=new BrowseSelection("shape");
        sel.addValue("square");
        br.addSelection(sel); 
		
		answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("blue",1),new BrowseFacet("red",2)}));
		answer.put("shape", Arrays.asList(new BrowseFacet[]{new BrowseFacet("rectangle",1),new BrowseFacet("square",2)}));
		
		doTest(br,2,answer,new String[]{"1","7"});
	}
	
	public void testPath() throws Exception{
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);

        BrowseSelection sel=new BrowseSelection("path");
        sel.addValue("a");
        Properties prop = sel.getSelectionProperties();
        PathFacetHandler.setDepth(prop, 1);
        br.addSelection(sel); 
		
		FacetSpec pathSpec=new FacetSpec();
		pathSpec.setOrderBy(FacetSortSpec.OrderValueAsc);
		br.setFacetSpec("path", pathSpec);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("path", Arrays.asList(new BrowseFacet[]{new BrowseFacet("a-b",1),new BrowseFacet("a-c",4),new BrowseFacet("a-e",2)}));
		doTest(br,7,answer,null);
		
		pathSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
		answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("path", Arrays.asList(new BrowseFacet[]{new BrowseFacet("a-c",4),new BrowseFacet("a-e",2),new BrowseFacet("a-b",1)}));
		doTest(br,7,answer,null);
		
		pathSpec.setMaxCount(2);
		answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("path", Arrays.asList(new BrowseFacet[]{new BrowseFacet("a-c",4),new BrowseFacet("a-e",2)}));
		doTest(br,7,answer,null);
	}

	public void testGeo() throws Exception{
		// testing facet counts for two distance facets - <30,70,5>, <60,120,1>
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);

        BrowseSelection sel=new BrowseSelection("distance");
        sel.addValue("<30,70,5>");
        sel.addValue("<60,120,1>");
        br.addSelection(sel); 
		
		FacetSpec geoSpec=new FacetSpec();
		geoSpec.setOrderBy(FacetSortSpec.OrderValueAsc);
		br.setFacetSpec("distance", geoSpec);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("distance", Arrays.asList(new BrowseFacet[]{new BrowseFacet("<30,70,5>",2),new BrowseFacet("<60,120,1>",2)}));
		doTest(br,4,answer,null);

		// testing for selection of facet <60,120,1> and verifying that 2 documents match this facet.
		BrowseRequest br2 = new BrowseRequest();
		br2.setCount(10);
		br2.setOffset(0);	

		BrowseSelection sel2 = new BrowseSelection("distance");
		sel2.addValue("<60,120,1>");
		HashMap map = new HashMap<String, Float>();
		map.put("<60,120,1>", 3.0f);
		FacetTermQuery geoQ = new FacetTermQuery(sel2,map);
		
		System.out.println("Testing GeoFacet Filters");
		BoboBrowser b = newBrowser();
		Explanation expl = b.explain(geoQ, 0);
		System.out.println(expl);
		
		br2.setQuery(geoQ);
		doTest(br2,2,null,new String[]{"1","5"});
		expl = b.explain(geoQ, 1);
	    System.out.println(expl);
	    
	    // facet query for color "red" and getting facet counts for the distance facet.
		BrowseRequest br3 = new BrowseRequest();
		br3.setCount(10);
		br3.setOffset(0);	

		BrowseSelection sel3 = new BrowseSelection("color");
		sel3.addValue("red");
		HashMap map3 = new HashMap<String, Float>();
		map3.put("red", 3.0f);
		FacetTermQuery colorQ = new FacetTermQuery(sel3,map3);

		BoboBrowser b2 = newBrowser();
		Explanation expl2 = b.explain(colorQ, 0);
		System.out.println(expl2);

		br3.setFacetSpec("distance", geoSpec);
		geoSpec.setMinHitCount(0);
		br3.setQuery(colorQ);             // query is color=red
		br3.addSelection(sel);			  // count facets <30,70,5> and <60,120,1>
		answer.clear();
		answer.put("distance", Arrays.asList(new BrowseFacet[]{new BrowseFacet("<30,70,5>", 0), new BrowseFacet("<60,120,1>",1)}));		
		doTest(br3, 1 , answer, null);

	}
	
	public void testMultiPath() throws Exception{
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);

        BrowseSelection sel=new BrowseSelection("multipath");
        sel.addValue("a");
        Properties prop = sel.getSelectionProperties();
        PathFacetHandler.setDepth(prop, 1);
        br.addSelection(sel); 
		
		FacetSpec pathSpec=new FacetSpec();
		pathSpec.setMaxCount(3);
		
		pathSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
		br.setFacetSpec("multipath", pathSpec);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
		answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("multipath", Arrays.asList(new BrowseFacet[]{new BrowseFacet("a-b",7),new BrowseFacet("a-c",4),new BrowseFacet("a-e",2)}));
		doTest(br,7,answer,null);
	}
	
	public void testMultiSelectedPaths() throws Exception{
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);

        BrowseSelection sel=new BrowseSelection("path");
        sel.addValue("a-c");
        sel.addValue("a-e");
        Properties prop = sel.getSelectionProperties();
        PathFacetHandler.setDepth(prop, 1);
        PathFacetHandler.setStrict(prop, true);
        br.addSelection(sel); 
		
		FacetSpec pathSpec=new FacetSpec();
		pathSpec.setMaxCount(3);
		
		pathSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
		br.setFacetSpec("path", pathSpec);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
		answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("path", Arrays.asList(new BrowseFacet[]{new BrowseFacet("a-c-d",2),new BrowseFacet("a-e-f",1)}));
		doTest(br,3,answer,null);
		
		pathSpec.setOrderBy(FacetSortSpec.OrderByCustom);
		pathSpec.setCustomComparatorFactory(new ComparatorFactory(){

			public Comparator<Integer> newComparator(
					FieldValueAccessor fieldValueAccessor, final int[] counts) {
				return new Comparator<Integer>(){

					public int compare(Integer f1, Integer f2) {
						int val = counts[f2] - counts[f1];
						if (val==0)
				        {
				            val=f2-f1;
				        }
				        return val;
					}
					
				};
			}

			public Comparator<BrowseFacet> newComparator() {
				return new Comparator<BrowseFacet>(){
					public int compare(BrowseFacet f1, BrowseFacet f2) {
						int val = f2.getHitCount() - f1.getHitCount();
						if (val==0)
				        {
				            val=f1.getValue().compareTo(f2.getValue());
				        }
				        return val;
					}	
				};
			}
			
		});
		
		answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("path", Arrays.asList(new BrowseFacet[]{new BrowseFacet("a-c-d",2),new BrowseFacet("a-e-f",1)}));
		doTest(br,3,answer,null);
	}
	
	public void testTagRollup(){
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);

        BrowseSelection sel=new BrowseSelection("location");
        Properties prop = sel.getSelectionProperties();
        PathFacetHandler.setDepth(prop, 1);
        PathFacetHandler.setStrict(prop, true);
        sel.addValue("toy/lego");
        br.addSelection(sel); 
		
		FacetSpec locationOutput=new FacetSpec();
		
		br.setFacetSpec("location", locationOutput);
		
		FacetSpec tagOutput=new FacetSpec();
		tagOutput.setMaxCount(50);
		tagOutput.setOrderBy(FacetSortSpec.OrderHitsDesc);
		
		br.setFacetSpec("tag", tagOutput);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("location", Arrays.asList(new BrowseFacet[]{new BrowseFacet("toy/lego/block",3)}));
		answer.put("tag", Arrays.asList(new BrowseFacet[]{new BrowseFacet("pet",2),new BrowseFacet("animal",1),new BrowseFacet("dog",1),new BrowseFacet("funny",1),new BrowseFacet("humor",1),new BrowseFacet("joke",1),new BrowseFacet("poodle",1),new BrowseFacet("rabbit",1)}));
		doTest(br,3,answer,null);
	}
	
	public void testChar(){
	  BrowseRequest br=new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);
      
      BrowseSelection sel=new BrowseSelection("char");
      sel.addValue("j");
      br.addSelection(sel);
      doTest(br,1,null,new String[]{"3"});
      
      br=new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);
      
      sel=new BrowseSelection("color");
      sel.addValue("red");
      br.addSelection(sel);
      
      FacetSpec charOutput=new FacetSpec();
      charOutput.setMaxCount(50);
      charOutput.setOrderBy(FacetSortSpec.OrderHitsDesc);
      

      br.setFacetSpec("char", charOutput);
      br.addSortField(new SortField("date",SortField.CUSTOM,true));
      
      HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
      answer.put("char", Arrays.asList(new BrowseFacet[]{new BrowseFacet("a",1),new BrowseFacet("i",1),new BrowseFacet("k",1)}));
      
      doTest(br,3,answer,new String[]{"7","2","1"});
	}
	
	public void testDate(){
	  BrowseRequest br=new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);
      
      BrowseSelection sel=new BrowseSelection("date");
      sel.addValue("[2001/01/01 TO 2005/01/01]");
      br.addSelection(sel);
      
      FacetSpec ospec=new FacetSpec();
      ospec.setExpandSelection(false);
      br.setFacetSpec("color", ospec);
     
      br.addSortField(new SortField("date",SortField.CUSTOM,true));
      
      HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
      answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("blue",2),new BrowseFacet("green",1),new BrowseFacet("red",1)}));
      doTest(br,4,answer,new String[]{"4","2","5","3"});
	}
	
	public void testDate2(){
      BrowseRequest br=new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);
      
      BrowseSelection sel=new BrowseSelection("date");
      sel.addValue("[2005/01/01 TO *]");
      br.addSelection(sel);
      
      FacetSpec ospec=new FacetSpec();
      ospec.setExpandSelection(false);
      br.setFacetSpec("color", ospec);
     
      br.addSortField(new SortField("date",SortField.CUSTOM,true));
      
      HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
      answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("green",1),new BrowseFacet("red",1)}));
      doTest(br,2,answer,new String[]{"6","7"});
    }
	
	public void testDate3(){
      BrowseRequest br=new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);
      
      BrowseSelection sel=new BrowseSelection("date");
      sel.addValue("[* TO 2002/01/01]");
      br.addSelection(sel);
      
      FacetSpec ospec=new FacetSpec();
      ospec.setExpandSelection(false);
      br.setFacetSpec("color", ospec);
     
      br.addSortField(new SortField("date",SortField.CUSTOM,true));
      
      HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
      answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("green",1),new BrowseFacet("red",1)}));
      doTest(br,2,answer,new String[]{"3","1"});
    }
	
	private BrowseResult doTest(BrowseRequest req,int numHits,HashMap<String,List<BrowseFacet>> choiceMap,String[] ids){
	  return doTest((BoboBrowser)null,req,numHits,choiceMap,ids);
    }
	
	private BrowseResult doTest(BoboBrowser boboBrowser,BrowseRequest req,int numHits,HashMap<String,List<BrowseFacet>> choiceMap,String[] ids) {
	  	BrowseResult result;
	  	try {
	        if (boboBrowser==null) {
	  		boboBrowser=newBrowser();
	  	  }
	        result = boboBrowser.browse(req);
	        doTest(result,req,numHits,choiceMap,ids);
	        return result;
	  	} catch (BrowseException e) {
	  		e.printStackTrace();
	  		fail(e.getMessage());
	  	}
	  	catch(IOException ioe){
	  	  fail(ioe.getMessage());
	  	}
	  	finally{
	  	  if (boboBrowser!=null){
	  	    try {
	  			boboBrowser.close();
	  		} catch (IOException e) {
	  			fail(e.getMessage());
	  		}
	  	  }
	  	}
	  	return null;
	  }
	
	public void testLuceneSort() throws IOException
	{
	  
	  IndexReader srcReader=IndexReader.open(_indexDir,true);
      try{
        List<FacetHandler<?>> facetHandlers = new ArrayList<FacetHandler<?>>();
        facetHandlers.add(new SimpleFacetHandler("id"));
        
        BoboIndexReader reader= BoboIndexReader.getInstance(srcReader,facetHandlers);       // not facet handlers to help
        BoboBrowser browser = new BoboBrowser(reader);
        
        BrowseRequest browseRequest = new BrowseRequest();
        browseRequest.setCount(10);
        browseRequest.setOffset(0);
        browseRequest.addSortField(new SortField("date",SortField.STRING));
        

        doTest(browser,browseRequest,7,null,new String[]{"1","3","5","2","4","7","6"});
        
      }
      catch(IOException ioe){
        if (srcReader!=null){
          srcReader.close();
        }
        throw ioe;
      }
	}
	
	public void testFacetSort()
	{

      BrowseRequest br=new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);
      
      FacetSpec colorSpec = new FacetSpec();
      colorSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
      br.setFacetSpec("color", colorSpec);
      

      FacetSpec shapeSpec = new FacetSpec();
      shapeSpec.setOrderBy(FacetSortSpec.OrderValueAsc);
      br.setFacetSpec("shape", shapeSpec);
      
      HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
      answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("red",3),new BrowseFacet("blue",2),new BrowseFacet("green",2)}));
      answer.put("shape", Arrays.asList(new BrowseFacet[]{new BrowseFacet("circle",2),new BrowseFacet("rectangle",2),new BrowseFacet("square",3)}));
      
      doTest(br,7,answer,null);
      
      Comparator<BrowseFacet> valComp = new FacetValueComparatorFactory().newComparator();
      
      int v = valComp.compare(new BrowseFacet("red",3), new BrowseFacet("blue",2));
      assertTrue(v>0);
      
      valComp = new FacetHitcountComparatorFactory().newComparator();
      v = valComp.compare(new BrowseFacet("red",3), new BrowseFacet("blue",2));
      assertTrue(v<0);
      
      v = valComp.compare(new BrowseFacet("red",3), new BrowseFacet("blue",3));
      assertTrue(v>0);
	}
	
	public void testMultiDate()
	{
	  BrowseRequest br=new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);
      
      BrowseSelection sel=new BrowseSelection("date");
      sel.addValue("[2000/01/01 TO 2002/07/07]");
      sel.addValue("[2003/01/01 TO 2005/01/01]");
      br.addSelection(sel);

      br.addSortField(new SortField("date",SortField.CUSTOM,false));

      doTest(br,5,null,new String[]{"1","3","5","2","4"});
	}

    public void testDate4(){
      BrowseRequest br=new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);
      
      BrowseSelection sel=new BrowseSelection("date");
      sel.addValue("[* TO *]");
      br.addSelection(sel);
      
      FacetSpec ospec=new FacetSpec();
      ospec.setExpandSelection(false);
      br.setFacetSpec("color", ospec);
     
      br.addSortField(new SortField("date",SortField.CUSTOM,false));
      
      doTest(br,7,null,new String[]{"1","3","5","2","4","7","6"});
    }
	
	public void testSort(){
	  // no sel
	  BrowseRequest br=new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);
      
      br.setSort(new SortField[]{new SortField("number",SortField.CUSTOM,true)});
      doTest(br,7,null,new String[]{"6","5","4","3","2","1","7"});
      br.setSort(new SortField[]{new SortField("name",SortField.STRING,false)});
      doTest(br,7,null,new String[]{"7","4","6","2","3","1","5"});
      
      BrowseSelection sel=new BrowseSelection("color");
      sel.addValue("red");
      br.addSelection(sel);
      br.setSort(new SortField[]{new SortField("number",SortField.CUSTOM,true)});
      doTest(br,3,null,new String[]{"2","1","7"});
      br.setSort(new SortField[]{new SortField("name",SortField.STRING,false)});
      doTest(br,3,null,new String[]{"7","2","1"});
      
      sel.addValue("blue");
      br.setQuery(new TermQuery(new Term("shape","square")));
      br.setSort(new SortField[]{new SortField("number",SortField.CUSTOM,true)});
      doTest(br,3,null,new String[]{"5","1","7"});
      br.setSort(new SortField[]{new SortField("name",SortField.STRING,false)});
      doTest(br,3,null,new String[]{"7","1","5"});
	}
	
	public void testCustomSort(){
		
		final class CustomSortComparatorSource extends DocComparatorSource{
			@Override
			public DocComparator getComparator(IndexReader reader, int docbase)
					throws IOException {
				return new CustomSortDocComparator();
			}

			final class CustomSortDocComparator extends DocComparator{

				public int compare(ScoreDoc doc1, ScoreDoc doc2) {
					int id1 = Math.abs(doc1.doc - 4);
					int id2 = Math.abs(doc2.doc - 4);
					int val = id1 - id2;
					if (val==0){
						return doc1.doc - doc2.doc;
					}
					return val;
				}
				
				public Comparable value(ScoreDoc doc) {
					return new Integer(Math.abs(doc.doc-4));
				}

			}
			
		}
		// no sel
		BrowseRequest br=new BrowseRequest();
	    br.setCount(10);
	    br.setOffset(0);
	      
	    br.setSort(new SortField[]{new BoboCustomSortField("custom",false,new CustomSortComparatorSource())});
	    doTest(br,7,null,new String[]{"5","4","6","3","7","2","1"});
	    
	    
	}

	public void testDefaultBrowse(){
	  BrowseRequest br=new BrowseRequest();
      br.setCount(3);
      br.setOffset(0);
      
      FacetSpec spec = new FacetSpec();
      spec.setMaxCount(2);
      spec.setOrderBy(FacetSortSpec.OrderHitsDesc);
      br.setFacetSpec("color", spec);
      

      br.setSort(new SortField[]{new SortField("number",SortField.CUSTOM,false)});
      
      HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
      answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("red",3),new BrowseFacet("blue",2)}));
      
      doTest(br,7,answer,new String[]{"7","1","2"});
	}
	
	
	public void testMinHit(){
		BrowseRequest br=new BrowseRequest();
	    br.setCount(3);
	    br.setOffset(0);
	    
	    BrowseSelection sel = new BrowseSelection("shape");
	    sel.addValue("square");
	    br.addSelection(sel);
	      
	    FacetSpec spec = new FacetSpec();
	    spec.setMinHitCount(0);
	    spec.setOrderBy(FacetSortSpec.OrderHitsDesc);
	    br.setFacetSpec("color", spec);
	      	      
	    HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
	    answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("red",2),new BrowseFacet("blue",1),new BrowseFacet("green",0)}));
	      
	    doTest(br,3,answer,null);
	}
	
	public void testRandomAccessFacet() throws Exception
	{
	  BrowseRequest br=new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);
      br.setFacetSpec("number",new FacetSpec());
      
      BoboBrowser browser = newBrowser();
      
      BrowseResult res=browser.browse(br);
      FacetAccessible facetAccessor = res.getFacetAccessor("number");
      BrowseFacet facet = facetAccessor.getFacet("5");
      
      assertEquals(facet.getValue(), "0005");
      assertEquals(facet.getHitCount(), 1);
	}
	
	public void testQueryWithScore() throws Exception{
		BrowseRequest br=new BrowseRequest();
		  QueryParser parser=new QueryParser(Version.LUCENE_CURRENT,"color",new StandardAnalyzer(Version.LUCENE_CURRENT));
		  br.setQuery(parser.parse("color:red OR shape:square"));
	      br.setCount(10);
	      br.setOffset(0);
	      
	      br.setSort(new SortField[]{SortField.FIELD_SCORE});
	      doTest(br,4,null,new String[]{"1","7","2","5"});
	      
	}

  public void testBrowseWithQuery(){
		try{
		  BrowseRequest br=new BrowseRequest();
		  QueryParser parser=new QueryParser(Version.LUCENE_CURRENT,"shape",new StandardAnalyzer(Version.LUCENE_CURRENT));
		  br.setQuery(parser.parse("square OR circle"));
	      br.setCount(10);
	      br.setOffset(0);
	      
	      BrowseSelection sel=new BrowseSelection("color");
	      sel.addValue("red");
	      br.addSelection(sel);
	      
	      
	      br.setSort(new SortField[]{new SortField("number",SortField.CUSTOM,false)});
	      doTest(br,2,null,new String[]{"7","1"});
	      

	      FacetSpec ospec=new FacetSpec();
	      ospec.setExpandSelection(true);
	      br.setFacetSpec("color", ospec);
	      HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
	      answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("blue",2),new BrowseFacet("green",1),new BrowseFacet("red",2)}));
	      doTest(br,2,answer,new String[]{"7","1"});
	      
	      br.clearSelections();
	      answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("blue",2),new BrowseFacet("green",1),new BrowseFacet("red",2)}));
	      doTest(br,5,answer,new String[]{"7","1","3","4","5"});
	      
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testBrowseCompactMultiVal(){
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
	    br.setOffset(0);
	    BrowseSelection sel=new BrowseSelection("compactnum");
	    sel.addValue("001");
	    sel.addValue("003");
	    sel.addValue("007");
	    br.addSelection(sel);
	    
	    FacetSpec ospec=new FacetSpec();
	    br.setFacetSpec("compactnum", ospec);
	    
	    br.setSort(new SortField[]{new SortField("compactnum",SortField.CUSTOM,true)});
	    
	    HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
	     
	    answer.put("compactnum", Arrays.asList(new BrowseFacet[]{new BrowseFacet("001",3),new BrowseFacet("002",1),new BrowseFacet("003",3),new BrowseFacet("007",2),new BrowseFacet("008",1),new BrowseFacet("012",1)}));
	      
	    doTest(br,6,answer,new String[]{"3","7","4","6","1","5"});
	    
	    
	    br=new BrowseRequest();
		br.setCount(10);
	    br.setOffset(0);
	    sel=new BrowseSelection("compactnum");
	    sel.addValue("001");
	    sel.addValue("002");
	    sel.addValue("003");
	    br.addSelection(sel);
	    sel.setSelectionOperation(ValueOperation.ValueOperationAnd);
	    doTest(br,1,null,new String[]{"6"});
	    
	    br=new BrowseRequest();
		br.setCount(10);
	    br.setOffset(0);
	    sel=new BrowseSelection("compactnum");
	    sel.addValue("001");
	    sel.addValue("003");
	    sel.addValue("008");
	    sel.setSelectionOperation(ValueOperation.ValueOperationOr);
	    br.addSelection(sel);
	    
	    sel=new BrowseSelection("color");
	    sel.addValue("red");
	    br.addSelection(sel);
	    
	    ospec=new FacetSpec();
	    br.setFacetSpec("color", ospec);
	    
	    ospec=new FacetSpec();
	    br.setFacetSpec("compactnum",ospec);
	    answer=new HashMap<String,List<BrowseFacet>>();
	         
	        answer.put("compactnum", Arrays.asList(new BrowseFacet[]{new BrowseFacet("001",1),new BrowseFacet("003",2),new BrowseFacet("008",1)}));
	        answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("red",2)}));
	        doTest(br,2,answer,new String[]{"1","7"});
	        
	    doTest(br,2,answer,new String[]{"1","7"});
	}
	
	public void testBrowseMultiVal(){
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
	    br.setOffset(0);
	    BrowseSelection sel=new BrowseSelection("multinum");
	    sel.addValue("001");
	    sel.addValue("003");
	    sel.addValue("007");
	    br.addSelection(sel);
	    

	    FacetSpec ospec=new FacetSpec();
	    br.setFacetSpec("multinum", ospec);
	    br.setSort(new SortField[]{new SortField("multinum",SortField.CUSTOM,true)});
	    HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();

		answer.put("multinum", Arrays.asList(new BrowseFacet[]{new BrowseFacet("001",3),new BrowseFacet("002",1),new BrowseFacet("003",3),new BrowseFacet("007",2),new BrowseFacet("008",1),new BrowseFacet("012",1)}));
      
	    doTest(br,6,answer,new String[]{"3","4","7","1","6","5"});
	    
	    
		
		br=new BrowseRequest();
		br.setCount(10);
	    br.setOffset(0);
	    sel=new BrowseSelection("multinum");
	    sel.addValue("001");
	    sel.addValue("002");
	    sel.addValue("003");
	    br.addSelection(sel);
	    sel.setSelectionOperation(ValueOperation.ValueOperationAnd);
	    doTest(br,1,null,new String[]{"6"});
	    
	    br=new BrowseRequest();
		br.setCount(10);
	    br.setOffset(0);
	    sel=new BrowseSelection("multinum");
	    sel.addValue("001");
	    sel.addValue("003");
	    sel.addValue("008");
	    sel.setSelectionOperation(ValueOperation.ValueOperationOr);
	    br.addSelection(sel);
	    
	    sel=new BrowseSelection("color");
	    sel.addValue("red");
	    br.addSelection(sel);
	    
	    ospec=new FacetSpec();
	    br.setFacetSpec("color", ospec);
	    	    
	    ospec=new FacetSpec();
	    br.setFacetSpec("multinum",ospec);
	    answer=new HashMap<String,List<BrowseFacet>>();
	     
	    answer.put("multinum", Arrays.asList(new BrowseFacet[]{new BrowseFacet("001",1),new BrowseFacet("003",2),new BrowseFacet("008",1)}));
	    answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("red",2)}));
	    doTest(br,2,answer,new String[]{"1","7"});
	    
	}

  public void testBrowseWithDeletes()
  {
    BoboIndexReader reader = null;

    BrowseRequest br = new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);

    BrowseSelection sel = new BrowseSelection("color");
    sel.addValue("red");
    br.addSelection(sel);
    HashMap<String, List<BrowseFacet>> answer = new HashMap<String, List<BrowseFacet>>();

    doTest(br, 3, answer, new String[] {"1", "2", "7"});

    try
    {
      reader = newIndexReader(false);
      reader.deleteDocuments(new Term("id", "1"));
      reader.deleteDocuments(new Term("id", "2"));
      
      br = new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);

      sel = new BrowseSelection("color");
      sel.addValue("red");
      br.addSelection(sel);
      answer = new HashMap<String, List<BrowseFacet>>();

      doTest(new BoboBrowser(reader), br, 1, answer, null);
    }
    catch (IOException ioe)
    {
      fail(ioe.getMessage());
    }
    finally
    {
      if (reader != null)
      {
        try
        {
          reader.close();
        }
        catch (IOException e)
        {
          fail(e.getMessage());
        }
      }
    }
    
    br = new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);

    sel = new BrowseSelection("color");
    sel.addValue("red");
    br.addSelection(sel);
    answer = new HashMap<String, List<BrowseFacet>>();


    doTest(br, 1, answer, null);
  }

	
	public void testNotSupport()
	{
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);
		
		BrowseSelection sel=new BrowseSelection("color");
		sel.addNotValue("red");
		br.addSelection(sel);
		
		FacetSpec simpleOutput=new FacetSpec();
		br.setFacetSpec("shape", simpleOutput);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("shape", Arrays.asList(new BrowseFacet[]{new BrowseFacet("circle",2),new BrowseFacet("rectangle",1),new BrowseFacet("square",1)}));

		doTest(br,4,answer,new String[]{"3","4","5","6"});
		
		sel.addNotValue("green");
		
		answer.put("shape", Arrays.asList(new BrowseFacet[]{new BrowseFacet("circle",1),new BrowseFacet("square",1)}));

        doTest(br,2,answer,new String[]{"4","5"});
		
        br=new BrowseRequest();
        br.setCount(10);
        br.setOffset(0);
        sel=new BrowseSelection("compactnum");
        sel.addNotValue("3");
        sel.addNotValue("4");
        sel.addValue("1");
        sel.addValue("2");
        sel.addValue("7");
        
        br.addSelection(sel);
        doTest(br,3,null,new String[]{"3","4","5"});
        
        br=new BrowseRequest();
        br.setCount(10);
        br.setOffset(0);
        sel=new BrowseSelection("multinum");
        sel.addNotValue("3");
        sel.addNotValue("4");
        sel.addValue("1");
        sel.addValue("2");
        sel.addValue("7");
        
        br.addSelection(sel);
        
        doTest(br,3,null,new String[]{"3","4","5"});
        
        
	}
	
	public void testMissedSelection()
	{
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);
		BrowseSelection sel=new BrowseSelection("location");
		sel.addValue("something/stupid");
		br.addSelection(sel);
		doTest(br,0,null,null);
	}
	
	public void testDateRange() {
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);
		
		FacetSpec simpleOutput=new FacetSpec();
		simpleOutput.setExpandSelection(true);
		br.setFacetSpec("date", simpleOutput);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("date",  Arrays.asList(new BrowseFacet[]{new BrowseFacet("[2000/01/01 TO 2003/05/05]", 4), new BrowseFacet("[2003/05/06 TO 2005/04/04]",1)}));
		BrowseResult result = doTest(br,7,answer,null);
	}
	
	/**
	 * Verifies the range facet numbers are returned correctly (as they were passed in)
	 */
	public void testNumEndorsers() {
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);
		
		FacetSpec simpleOutput=new FacetSpec();
		simpleOutput.setExpandSelection(true);
		br.setFacetSpec("numendorsers", simpleOutput);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("numendorsers",  Arrays.asList(new BrowseFacet[]{new BrowseFacet("[000000 TO 000005]", 2), new BrowseFacet("[000006 TO 000010]",2), new BrowseFacet("[000011 TO 000020]",3)}));
		BrowseResult result = doTest(br,7,answer,null);
	}
	
	public void testBrowse(){
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);
		
		BrowseSelection sel=new BrowseSelection("color");
		sel.addValue("red");
		br.addSelection(sel);
		
		sel=new BrowseSelection("location");
		sel.addValue("toy/lego");
		
		Properties prop = sel.getSelectionProperties();
		PathFacetHandler.setDepth(prop, 1);
		br.addSelection(sel);
		
		sel=new BrowseSelection("size");
		sel.addValue("[* TO 4]");
		
	    sel=new BrowseSelection("tag");
		sel.addValue("rabbit");
		br.addSelection(sel);
		
		FacetSpec output=new FacetSpec();
		output.setMaxCount(5);
		
		FacetSpec simpleOutput=new FacetSpec();
		simpleOutput.setExpandSelection(true);
		
		
		br.setFacetSpec("color", simpleOutput);
		br.setFacetSpec("size", output);
		br.setFacetSpec("shape", simpleOutput);
		br.setFacetSpec("location", output);
		
		FacetSpec tagOutput=new FacetSpec();
		tagOutput.setMaxCount(5);
		tagOutput.setOrderBy(FacetSortSpec.OrderHitsDesc);
		
		br.setFacetSpec("tag", tagOutput);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("color",  Arrays.asList(new BrowseFacet[]{new BrowseFacet("green",1),new BrowseFacet("red",2)}));
		answer.put("size", Arrays.asList(new BrowseFacet[]{new BrowseFacet("[* TO 4]",1),new BrowseFacet("[5 TO 8]",1)}));
		answer.put("shape", Arrays.asList(new BrowseFacet[]{new BrowseFacet("square",2)}));
		answer.put("location", Arrays.asList(new BrowseFacet[]{new BrowseFacet("toy/lego/",1),new BrowseFacet("toy/lego/block",1)}));
		answer.put("tag", Arrays.asList(new BrowseFacet[]{new BrowseFacet("rabbit",2),new BrowseFacet("animal",1),new BrowseFacet("dog",1),new BrowseFacet("humane",1),new BrowseFacet("pet",1)}));
		doTest(br,2,answer,null);
		
	}
	
	/**
	 * Tests the MultiBoboBrowser functionality by creating a BoboBrowser and 
	 * submitting the same browserequest 2 times generating 2 BrowseResults.  
	 * The 2 BoboBrowsers are instantiated with the MultiBoboBrowser and the browse method is called.
	 * The BrowseResult generated is submitted to the doTest method which compares the result
	 */
	public void testMultiBrowser() throws Exception {
	  BrowseRequest browseRequest = new BrowseRequest();
      browseRequest.setCount(10);
      browseRequest.setOffset(0);
      browseRequest.addSortField(new SortField("date",SortField.CUSTOM));
      
      BrowseSelection colorSel = new BrowseSelection("color");
      colorSel.addValue("red");
      browseRequest.addSelection(colorSel);
      
      BrowseSelection tageSel = new BrowseSelection("tag");
      tageSel.addValue("rabbit");
      browseRequest.addSelection(tageSel);
      
      FacetSpec colorFacetSpec = new FacetSpec();
      colorFacetSpec.setExpandSelection(true);
      colorFacetSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
      
      FacetSpec tagFacetSpec = new FacetSpec();
              
      browseRequest.setFacetSpec("color", colorFacetSpec);
      browseRequest.setFacetSpec("tag", tagFacetSpec);
      
      FacetSpec shapeSpec = new FacetSpec();
      shapeSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
      browseRequest.setFacetSpec("shape", shapeSpec);
      
      FacetSpec dateSpec=new FacetSpec();
      dateSpec.setExpandSelection(true);
      browseRequest.setFacetSpec("date", dateSpec);
      
      BoboBrowser boboBrowser = newBrowser();
      
      browseRequest.setSort(new SortField[]{new SortField("compactnum",SortField.CUSTOM,true)});
      
      MultiBoboBrowser multiBoboBrowser = new MultiBoboBrowser(new Browsable[] {boboBrowser, boboBrowser});
      BrowseResult mergedResult = multiBoboBrowser.browse(browseRequest);
      
      HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
      answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("red",4),new BrowseFacet("green",2)}));
      answer.put("tag", Arrays.asList(new BrowseFacet[]{new BrowseFacet("animal",2),new BrowseFacet("dog",2),new BrowseFacet("humane",2),new BrowseFacet("pet",2),new BrowseFacet("rabbit",4)}));
      answer.put("shape", Arrays.asList(new BrowseFacet[]{new BrowseFacet("square",4)}));
      answer.put("date",  Arrays.asList(new BrowseFacet[]{new BrowseFacet("[2000/01/01 TO 2003/05/05]", 2)}));
      
      doTest(mergedResult, browseRequest, 4, answer, new String[]{"7","7","1","1"});
      
      browseRequest.setSort(new SortField[]{new SortField("multinum",SortField.CUSTOM,true)});
      mergedResult = multiBoboBrowser.browse(browseRequest);
      doTest(mergedResult, browseRequest, 4, answer, new String[]{"7","7","1","1"});
	}
	
	public void testFacetQuery() throws Exception{
		BrowseSelection sel = new BrowseSelection("color");
		sel.addValue("red");
		sel.addValue("blue");
		HashMap map = new HashMap<String, Float>();
		map.put("red", 3.0f);
		map.put("blue", 2.0f);
		FacetTermQuery colorQ = new FacetTermQuery(sel,map);
		
		BrowseSelection sel2 = new BrowseSelection("tag");
		sel2.addValue("rabbit");
		sel2.addValue("dog");
		HashMap map2 = new HashMap<String, Float>();
		map2.put("rabbit", 100.0f);
		map2.put("dog", 50.0f);
		FacetTermQuery tagQ = new FacetTermQuery(sel2,map2);
		
		
		BrowseRequest br = new BrowseRequest();
		br.setQuery(colorQ);
		br.setOffset(0);
		br.setCount(10);
		
		doTest(br,5,null,new String[]{"1","2","7","4","5"});
		
		BoboBrowser b = newBrowser();
		Explanation expl = b.explain(colorQ, 0);
		System.out.println(expl);
		
		br.setQuery(tagQ);
		doTest(br,4,null,new String[]{"7","1","3","2"});
		expl = b.explain(tagQ, 6);
	    System.out.println(expl);
		
	}
	
	public void testRuntimeFilteredDateRange() throws Exception{
		BoboBrowser browser = newBrowser();
		String[] ranges = new String[]{"[2001/01/01 TO 2001/12/30]","[2007/01/01 TO 2007/12/30]"};
		FilteredRangeFacetHandler handler = new FilteredRangeFacetHandler("filtered_date", "date",Arrays.asList(ranges));
		browser.setFacetHandler(handler);
		
		BrowseRequest req = new BrowseRequest();
		req.setFacetSpec("filtered_date", new FacetSpec());
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
	    answer.put("filtered_date", Arrays.asList(new BrowseFacet[]{new BrowseFacet("[2001/01/01 TO 2001/12/30]",1),new BrowseFacet("[2007/01/01 TO 2007/12/30]",1)}));
	      
		doTest(browser,req,7,answer,null);
	}
	
	public void testCustomFacetSort() throws Exception{
		BrowseRequest req = new BrowseRequest();
		FacetSpec numberSpec = new FacetSpec();
		numberSpec.setCustomComparatorFactory(new ComparatorFactory() {
			
			public Comparator<Integer> newComparator(final FieldValueAccessor fieldValueAccessor,
					final int[] counts) {
				
				return new Comparator<Integer>(){

					public int compare(Integer v1, Integer v2) {
						Integer size1 = (Integer)fieldValueAccessor.getRawValue(v1);
						Integer size2 = (Integer)fieldValueAccessor.getRawValue(v2);
						
						int val = size1-size2;
						if (val == 0){
							val = counts[v1]-counts[v2];
						}
						return val;
					}
					
				};
			}

			public Comparator<BrowseFacet> newComparator() {
				return new Comparator<BrowseFacet>(){
					public int compare(BrowseFacet o1, BrowseFacet o2) {
						int v1 = Integer.parseInt(o1.getValue());
						int v2 = Integer.parseInt(o2.getValue());
						int val = v2-v1;
						if (val == 0){
							val = o2.getHitCount()-o1.getHitCount();
						}
						return val;
					}
				};
			}
		});
		numberSpec.setOrderBy(FacetSortSpec.OrderByCustom);
		numberSpec.setMaxCount(3);
		req.setFacetSpec("number", numberSpec);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
	    answer.put("number", Arrays.asList(new BrowseFacet[]{new BrowseFacet("2130",1),new BrowseFacet("1013",1),new BrowseFacet("0913",1)})); 
	    
		doTest(req,7,answer,null);
		
		numberSpec.setOrderBy(FacetSortSpec.OrderValueAsc);
		answer.put("number", Arrays.asList(new BrowseFacet[]{new BrowseFacet("0005",1),new BrowseFacet("0010",1),new BrowseFacet("0011",1)})); 
	    
		doTest(req,7,answer,null);
	}
	
	public void testSimpleGroupbyFacetHandler() throws Exception{
		BrowseRequest req = new BrowseRequest();
		FacetSpec fspec = new FacetSpec();
		req.setFacetSpec("groupby", fspec);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
	    answer.put("groupby", Arrays.asList(new BrowseFacet[]{new BrowseFacet("red,rectangle,0011",1),new BrowseFacet("red,square,0005",1),new BrowseFacet("red,square,0010",1)})); 
	    
	    BrowseSelection sel=new BrowseSelection("groupby");
	    sel.addValue("red");
	    req.addSelection(sel);

		doTest(req,3,answer,null);
		
	    sel.setValues(new String[]{"red,square"});
	    answer.put("groupby", Arrays.asList(new BrowseFacet[]{new BrowseFacet("red,square,0005",1),new BrowseFacet("red,square,0010",1)})); 
	    
		doTest(req,2,answer,null);
		
		sel.setValues(new String[]{"red,square,0005"});
	    answer.put("groupby", Arrays.asList(new BrowseFacet[]{new BrowseFacet("red,square,0005",1)})); 
	    
		doTest(req,1,answer,null);

		req.removeSelection("groupby");
		fspec.setMaxCount(2);
		answer.put("groupby", Arrays.asList(new BrowseFacet[]{new BrowseFacet("blue,circle,0913",1),new BrowseFacet("blue,square,1013",1)})); 
	    
		doTest(req,7,answer,null);

	}
	
	public static void main(String[] args)throws Exception {
		BoboTestCase test=new BoboTestCase("testSimpleGroupbyFacetHandler");
		test.setUp();
		test.testSimpleGroupbyFacetHandler();
		test.tearDown();
	}
}
