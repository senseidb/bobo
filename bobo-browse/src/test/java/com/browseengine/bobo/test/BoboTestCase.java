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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Payload;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
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
import com.browseengine.bobo.api.BrowseHit.TermFrequencyVector;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.BrowseSelection.ValueOperation;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.api.FieldValueAccessor;
import com.browseengine.bobo.api.MultiBoboBrowser;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandler.TermCountSize;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.FacetDataFetcher;
import com.browseengine.bobo.facets.data.PredefinedTermListFactory;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.impl.BucketFacetHandler;
import com.browseengine.bobo.facets.impl.ComboFacetHandler;
import com.browseengine.bobo.facets.impl.CompactMultiValueFacetHandler;
import com.browseengine.bobo.facets.impl.DynamicTimeRangeFacetHandler;
import com.browseengine.bobo.facets.impl.FacetHitcountComparatorFactory;
import com.browseengine.bobo.facets.impl.FacetValueComparatorFactory;
import com.browseengine.bobo.facets.impl.FilteredRangeFacetHandler;
import com.browseengine.bobo.facets.impl.GeoFacetHandler;
import com.browseengine.bobo.facets.impl.GeoSimpleFacetHandler;
import com.browseengine.bobo.facets.impl.HistogramFacetHandler;
import com.browseengine.bobo.facets.impl.MultiValueFacetHandler;
import com.browseengine.bobo.facets.impl.MultiValueWithWeightFacetHandler;
import com.browseengine.bobo.facets.impl.PathFacetHandler;
import com.browseengine.bobo.facets.impl.RangeFacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;
import com.browseengine.bobo.facets.impl.SimpleGroupbyFacetHandler;
import com.browseengine.bobo.facets.impl.VirtualSimpleFacetHandler;
import com.browseengine.bobo.index.BoboIndexer;
import com.browseengine.bobo.index.digest.DataDigester;
import com.browseengine.bobo.query.FacetBasedBoostScorerBuilder;
import com.browseengine.bobo.query.RecencyBoostScorerBuilder;
import com.browseengine.bobo.query.ScoreAdjusterQuery;
import com.browseengine.bobo.query.scoring.FacetTermQuery;
import com.browseengine.bobo.sort.DocComparator;
import com.browseengine.bobo.sort.DocComparatorSource;
import com.browseengine.bobo.util.IntBoundedPriorityQueue.IntComparator;

public class BoboTestCase extends TestCase {
  static Logger log = Logger.getLogger(BoboTestCase.class);
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
    String confdir = System.getProperty("conf.dir");
    if (confdir == null) confdir ="./resource";
    org.apache.log4j.PropertyConfigurator.configure(confdir+"/log4j.properties");
		_fconf=buildFieldConf();
		_indexDir=createIndex();
	}
	

	private BoboIndexReader newIndexReader() throws IOException{
		return newIndexReader(true);
	}
	
	private BoboIndexReader newIndexReader(boolean readonly) throws IOException{
	  IndexReader srcReader=IndexReader.open(_indexDir,readonly);
      try{
        BoboIndexReader reader= BoboIndexReader.getInstance(srcReader,_fconf, null);
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
	
	static final class MetaTokenStream extends TokenStream {
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
		d1.add(buildMetaField("multiwithweight","cool:200"));
		d1.add(buildMetaField("multiwithweight","good:100"));
		d1.add(buildMetaField("compactnum","001"));
		d1.add(buildMetaField("compactnum","003"));
		d1.add(buildMetaField("numendorsers","000003"));
		d1.add(buildMetaField("path","a-b"));
		d1.add(buildMetaField("multipath","a-b"));
		d1.add(buildMetaField("custom","000003"));
		d1.add(buildMetaField("latitude", "60"));
		d1.add(buildMetaField("longitude", "120"));
		d1.add(buildMetaField("salary", "04500"));
		
		Field sf = new Field("testStored","stored",Store.YES,Index.NOT_ANALYZED_NO_NORMS);
		sf.setOmitTermFreqAndPositions(true);
		d1.add(sf);
		

        Field tvf = new Field("tv","bobo bobo lucene lucene lucene test",Store.NO,Index.ANALYZED,TermVector.YES);
        
        d1.add(tvf);
		
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
		d2.add(buildMetaField("multiwithweight","cool:300"));
		d2.add(buildMetaField("multiwithweight","good:200"));
		d2.add(buildMetaField("compactnum","002"));
		d2.add(buildMetaField("compactnum","004"));
		d2.add(buildMetaField("numendorsers","000010"));
		d2.add(buildMetaField("path","a-c-d"));
		d2.add(buildMetaField("multipath","a-c-d"));
		d2.add(buildMetaField("multipath","a-b"));
		d2.add(buildMetaField("custom","000010"));
		d2.add(buildMetaField("latitude", "50"));
		d2.add(buildMetaField("longitude", "110"));
		d2.add(buildMetaField("salary", "08500"));
		
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
		d3.add(buildMetaField("multiwithweight","cool:200"));
		d3.add(buildMetaField("compactnum","007"));
		d3.add(buildMetaField("compactnum","012"));
		d3.add(buildMetaField("numendorsers","000015"));
		d3.add(buildMetaField("path","a-e"));
		d3.add(buildMetaField("multipath","a-e"));
		d3.add(buildMetaField("multipath","a-b"));
		d3.add(buildMetaField("custom","000015"));
		d3.add(buildMetaField("latitude", "35"));
		d3.add(buildMetaField("longitude", "70"));
		d3.add(buildMetaField("salary", "06500"));
		
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
		d4.add(buildMetaField("salary", "11200"));
		
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
		d5.add(buildMetaField("salary", "10500"));
		
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
		d6.add(buildMetaField("salary", "08900"));
		
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
		d7.add(buildMetaField("salary", "28500"));
		
		Document d8 = new Document();
		d8.add(buildMetaField("latitude", "35"));
		d8.add(buildMetaField("longitude", "120"));
		d8.add(buildMetaField("salary", "00120"));
		
		dataList.add(d1);
		dataList.add(d2);
		dataList.add(d3);
		dataList.add(d4);
		dataList.add(d5);
		dataList.add(d6);
		dataList.add(d7);
		dataList.add(d8);
		
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
		shapeHandler.setTermCountSize(TermCountSize.medium);
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
    facetHandlers.add(new VirtualSimpleFacetHandler("virtual", numTermFactory, new FacetDataFetcher()
    {
      public Object fetch(BoboIndexReader reader, int doc)
      {
        FacetDataCache sourceCache = (FacetDataCache)reader.getFacetData("number");
        if (sourceCache == null)
          return null;

        return sourceCache.valArray.getRawValue(sourceCache.orderArray.get(doc));
      }

      public void cleanup(BoboIndexReader reader)
      {
        // do nothing here.
      }
    }, new HashSet<String>(Arrays.asList(new String[]{"number"}))));
		facetHandlers.add(new SimpleFacetHandler("testStored"));
		
		

		facetHandlers.add(new SimpleFacetHandler("name"));
		facetHandlers.add(new RangeFacetHandler("date", new PredefinedTermListFactory(Date.class, "yyyy/MM/dd"), Arrays.asList(new String[]{"[2000/01/01 TO 2003/05/05]", "[2003/05/06 TO 2005/04/04]"})));
		facetHandlers.add(new SimpleFacetHandler("char", (TermListFactory)null));
		facetHandlers.add(new MultiValueFacetHandler("tag", (String)null, (TermListFactory)null, tagSizePayloadTerm));
		facetHandlers.add(new MultiValueFacetHandler("multinum", new PredefinedTermListFactory(Integer.class, "000")));
		facetHandlers.add(new MultiValueWithWeightFacetHandler("multiwithweight"));
		facetHandlers.add(new CompactMultiValueFacetHandler("compactnum", new PredefinedTermListFactory(Integer.class, "000")));
		facetHandlers.add(new SimpleFacetHandler("storenum", new PredefinedTermListFactory(Long.class, null)));
		/* New FacetHandler for geographic locations. Depends on two RangeFacetHandlers on latitude and longitude */
		facetHandlers.add(new RangeFacetHandler("latitude", Arrays.asList(new String[]{"[* TO 30]", "[35 TO 60]", "[70 TO 120]"})));
		facetHandlers.add(new RangeFacetHandler("longitude", Arrays.asList(new String[]{"[* TO 30]", "[35 TO 60]", "[70 TO 120]"})));
		facetHandlers.add(new GeoSimpleFacetHandler("distance", "latitude", "longitude"));
		facetHandlers.add(new GeoFacetHandler("correctDistance", "latitude", "longitude"));
		/* Underlying time facet for DynamicTimeRangeFacetHandler */
		facetHandlers.add(new RangeFacetHandler("timeinmillis", new PredefinedTermListFactory(Long.class, DynamicTimeRangeFacetHandler.NUMBER_FORMAT),null));
		
		String[] predefinedSalaryRanges = new String[4];
		predefinedSalaryRanges[0] = new String("[04000 TO 05999]");
		predefinedSalaryRanges[1] = new String("[06000 TO 07999]");
		predefinedSalaryRanges[2] = new String("[08000 TO 09999]");
		predefinedSalaryRanges[3] = new String("[10000 TO *]");
		RangeFacetHandler dependedRangeFacet = new RangeFacetHandler("salary", Arrays.asList(predefinedSalaryRanges));
        facetHandlers.add(dependedRangeFacet);
    
		String[][] predefinedBuckets = new String[4][];
        predefinedBuckets[0] =  new String[]{"ken","igor","abe"};
        predefinedBuckets[1] =  new String[]{"ken","john","mike"};
        predefinedBuckets[2] =  new String[]{"john","cathy"};
        predefinedBuckets[3] =  new String[]{"doug"};
        
        Map<String,String[]> predefinedGroups = new HashMap<String,String[]>();
        predefinedGroups.put("g1", predefinedBuckets[0]);
        predefinedGroups.put("g2", predefinedBuckets[1]);
        predefinedGroups.put("g3", predefinedBuckets[2]);
        predefinedGroups.put("g4", predefinedBuckets[3]);
        
		facetHandlers.add(new BucketFacetHandler("groups", predefinedGroups, "name"));
		
		
		String[][] predefinedBuckets2 = new String[3][];
		predefinedBuckets2[0] =  new String[]{"2","3"};
		predefinedBuckets2[1] =  new String[]{"1","4"};
		predefinedBuckets2[2] =  new String[]{"7","8"};
        
        Map<String,String[]> predefinedNumberSets = new HashMap<String,String[]>();
        predefinedNumberSets.put("s1", predefinedBuckets2[0]);
        predefinedNumberSets.put("s2", predefinedBuckets2[1]);
        predefinedNumberSets.put("s3", predefinedBuckets2[2]);
        
		facetHandlers.add(new BucketFacetHandler("sets", predefinedNumberSets, "multinum"));
		
		
		// histogram
		
		HistogramFacetHandler<Integer> histoHandler = new HistogramFacetHandler<Integer>("numberhisto", "number", new Integer(0), new Integer(5000), new Integer(100));
		
		facetHandlers.add(histoHandler);
		
		LinkedHashSet<String> dependsNames=new LinkedHashSet<String>();
		dependsNames.add("color");
		dependsNames.add("shape");
		dependsNames.add("number");
		facetHandlers.add(new SimpleGroupbyFacetHandler("groupby", dependsNames));
		

		ComboFacetHandler colorShape = new ComboFacetHandler("colorShape",new HashSet(Arrays.asList(new String[]{"color","shape"})));
		ComboFacetHandler colorShapeMultinum = new ComboFacetHandler("colorShapeMultinum",new HashSet(Arrays.asList(new String[]{"color","shape","multinum"})));
		
		facetHandlers.add(colorShape);
		facetHandlers.add(colorShapeMultinum);
	    		
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
    						  BrowseFacet bf1 = iter1.next();
    						  BrowseFacet bf2 = iter2.next();
    							//if (!iter1.next().equals(iter2.next()))
    						  if(!bf1.equals(bf2))
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
	
	private static boolean checkFacet(BrowseResult res,int numHits, String facetName, HashMap<String,List<BrowseFacet>> choiceMap, String[] ids){
    boolean match=false;
    if (numHits==res.getNumHits()){
        if (choiceMap!=null){
          Set<Entry<String,FacetAccessible>> entries=res.getFacetMap().entrySet();
          
          if (res.getFacetMap().containsKey(facetName))
          {
            FacetAccessible c1 = res.getFacetMap().get(facetName);
            List<BrowseFacet> l1 = c1.getFacets();
            List<BrowseFacet> l2 =choiceMap.get(facetName);

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
	
	
	/**
	 * check results
	 * @param result
	 * @param req
	 * @param numHits
	 * @param choiceMap
	 * @param ids
	 */
	private void doTest(BrowseResult result,BrowseRequest req,int numHits,HashMap<String,List<BrowseFacet>> choiceMap,String[] ids){
			if (!check(result,numHits,choiceMap,ids))
	    //  if (!checkBucket(result,numHits,"salaryBucket", choiceMap,ids))
	      {
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

	public void testStoredFacetField() throws Exception{
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);

        BrowseSelection colorSel=new BrowseSelection("testStored");
        colorSel.addValue("stored");
        br.addSelection(colorSel); 
        br.setFetchStoredFields(true);
        
        BrowseResult result = null;
        BoboBrowser boboBrowser=null;
	  	try {
	  		boboBrowser=newBrowser();
	  	  
	        result = boboBrowser.browse(br);
	        assertEquals(1,result.getNumHits());
	        BrowseHit hit = result.getHits()[0];
	        Document storedFields = hit.getStoredFields();
	        assertNotNull(storedFields);
	        
	        String[] values = storedFields.getValues("testStored");
	        assertNotNull(values);
	        assertEquals(1, values.length);
	        assertTrue("stored".equals(values[0]));
	        
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
	  	      if(result!=null) result.close();
	  			boboBrowser.close();
	  		} catch (IOException e) {
	  			fail(e.getMessage());
	  		}
	  	  }
	  	}
        
	}
	
	public void testStoredField() throws Exception{
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);

        BrowseSelection colorSel=new BrowseSelection("color");
        colorSel.addValue("red");
        br.addSelection(colorSel); 

        BrowseSelection shapeSel=new BrowseSelection("shape");
        shapeSel.addValue("square");
        br.addSelection(shapeSel);
        
        BrowseSelection sizeSel=new BrowseSelection("size");
        sizeSel.addValue("[4 TO 4]");
        br.addSelection(sizeSel);

        BrowseResult result = null;
        BoboBrowser boboBrowser=null;
	  	try {
	  		boboBrowser=newBrowser();
	  	  
	        result = boboBrowser.browse(br);
	        assertEquals(1,result.getNumHits());
	        BrowseHit hit = result.getHits()[0];
	        assertNull(hit.getStoredFields());
	        
	        br.setFetchStoredFields(true);
	        result = boboBrowser.browse(br);
	        assertEquals(1,result.getNumHits());
	        hit = result.getHits()[0];
	        Document storedFields = hit.getStoredFields();
	        assertNotNull(storedFields);
	        
	        String stored = storedFields.get("testStored");
	        assertTrue("stored".equals(stored));
	        
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
	  	      if(result!=null) result.close();
	  			boboBrowser.close();
	  		} catch (IOException e) {
	  			fail(e.getMessage());
	  		}
	  	  }
	  	}
        
	}
	
	public void testRetrieveTermVector() throws Exception{
      BrowseRequest br=new BrowseRequest();
      br.setCount(10);
      br.setOffset(0);

      BrowseSelection colorSel=new BrowseSelection("color");
      colorSel.addValue("red");
      br.addSelection(colorSel); 

      BrowseSelection shapeSel=new BrowseSelection("shape");
      shapeSel.addValue("square");
      br.addSelection(shapeSel);
      
      BrowseSelection sizeSel=new BrowseSelection("size");
      sizeSel.addValue("[4 TO 4]");
      br.addSelection(sizeSel);
      
      br.setTermVectorsToFetch(new HashSet<String>(Arrays.asList(new String[]{"tv"})));

      BrowseResult result = null;
      BoboBrowser boboBrowser=null;
      try {
          boboBrowser=newBrowser();
        
          result = boboBrowser.browse(br);
          assertEquals(1,result.getNumHits());
          BrowseHit hit = result.getHits()[0];
          assertNull(hit.getStoredFields());
          
          br.setFetchStoredFields(true);
          result = boboBrowser.browse(br);
          assertEquals(1,result.getNumHits());
          hit = result.getHits()[0];
          Map<String,TermFrequencyVector> tvMap = hit.getTermFreqMap();
          assertNotNull(tvMap);
          
          assertEquals(1, tvMap.size());
          
          TermFrequencyVector tv = tvMap.get("tv");
          assertNotNull(tv);
          
          assertEquals("bobo", tv.terms[0]);
          assertEquals(2, tv.freqs[0]);
          
          assertEquals("lucene", tv.terms[1]);
          assertEquals(3, tv.freqs[1]);

          assertEquals("test", tv.terms[2]);
          assertEquals(1, tv.freqs[2]);
          
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
            if(result!=null) result.close();
              boboBrowser.close();
          } catch (IOException e) {
              fail(e.getMessage());
          }
        }
      }
      
  }
	
	public void testRawDataRetrieval() throws Exception{
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);
		br.setSort(new SortField[]{new SortField("date",SortField.CUSTOM,false)});
		BrowseResult result = null;
        BoboBrowser boboBrowser=null;
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd");
	  	try {
	  		boboBrowser=newBrowser();
	  	  
	        result = boboBrowser.browse(br);
	        assertEquals(7,result.getNumHits());
	        BrowseHit hit = result.getHits()[0];
	        assertEquals(0,hit.getDocid());
	        Object lowDate = hit.getRawField("date");
	        Date date = dateFormatter.parse("2000/01/01");
	        assertTrue(lowDate.equals(date.getTime()));
	        
	        hit = result.getHits()[6];
	        assertEquals(5,hit.getDocid());
	        Object highDate = hit.getRawField("date");
	        date = dateFormatter.parse("2007/08/01");
	        assertTrue(highDate.equals(date.getTime()));
	        
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
	  	      if(result!=null) result.close();
	  			boboBrowser.close();
	  		} catch (IOException e) {
	  			fail(e.getMessage());
	  		}
	  	  }
	  	}
		
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
	
	public void testComboFacetHandlerSelectionOnly(){
		
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);
		
		BrowseSelection sel=new BrowseSelection("colorShape");
		sel.addValue("color:green");
		sel.addValue("shape:rectangle");
		sel.addValue("shape:square");
		sel.setSelectionOperation(ValueOperation.ValueOperationOr);
		br.addSelection(sel);
		
		doTest(br,6,null,new String[]{"1","2","3","5","6","7"});
		
		br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);
		
		sel=new BrowseSelection("colorShape");
		sel.addValue("color:green");
		sel.addValue("shape:rectangle");
		sel.setSelectionOperation(ValueOperation.ValueOperationAnd);
		br.addSelection(sel);
		
		doTest(br,1,null,new String[]{"6"});
		
		br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);
		
		sel=new BrowseSelection("colorShapeMultinum");
		sel.addValue("color:red");
		sel.addValue("shape:square");
		sel.setSelectionOperation(ValueOperation.ValueOperationOr);
		sel.addNotValue("multinum:001");
		sel.addNotValue("multinum:003");
		br.addSelection(sel);
		
		doTest(br,1,null,new String[]{"2"});
		
		br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);
		
		sel=new BrowseSelection("colorShapeMultinum");
		sel.addValue("color:red");
		sel.addValue("shape:square");
		sel.setSelectionOperation(ValueOperation.ValueOperationOr);
		sel.addNotValue("multinum:003");
		br.addSelection(sel);
		
		doTest(br,2,null,new String[]{"2","5"});
		
		
	}

	/**
	 * This tests GeoSimpleFacetHandler
	 * @throws Exception
	 */
	public void testSimpleGeo() throws Exception{
		// testing facet counts for two distance facets - <30,70,5>, <60,120,1>
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);

        BrowseSelection sel=new BrowseSelection("distance");
        sel.addValue("30,70:5");
        sel.addValue("60,120:1");
        br.addSelection(sel); 
		
		FacetSpec geoSpec=new FacetSpec();
		geoSpec.setOrderBy(FacetSortSpec.OrderValueAsc);
		br.setFacetSpec("distance", geoSpec);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("distance", Arrays.asList(new BrowseFacet[]{new BrowseFacet("30,70:5",2),new BrowseFacet("60,120:1",2)}));
		doTest(br,4,answer,null);

		// testing for selection of facet <60,120,1> and verifying that 2 documents match this facet.
		BrowseRequest br2 = new BrowseRequest();
		br2.setCount(10);
		br2.setOffset(0);	

		BrowseSelection sel2 = new BrowseSelection("distance");
		sel2.addValue("60,120:1");
		HashMap<String, Float> map = new HashMap<String, Float>();
		map.put("0,120:1", 3.0f);
		FacetTermQuery geoQ = new FacetTermQuery(sel2,map);
		
		BoboBrowser b = newBrowser();
		Explanation expl = b.explain(geoQ, 0);
		
		br2.setQuery(geoQ);
		doTest(br2,2,null,new String[]{"1","5"});
		expl = b.explain(geoQ, 1);
	    
	    // facet query for color "red" and getting facet counts for the distance facet.
		BrowseRequest br3 = new BrowseRequest();
		br3.setCount(10);
		br3.setOffset(0);	

		BrowseSelection sel3 = new BrowseSelection("color");
		sel3.addValue("red");
		HashMap<String, Float> map3 = new HashMap<String, Float>();
		map3.put("red", 3.0f);
		FacetTermQuery colorQ = new FacetTermQuery(sel3,map3);

		BoboBrowser b2 = newBrowser();
		Explanation expl2 = b.explain(colorQ, 0);
		
		br3.setFacetSpec("distance", geoSpec);
		geoSpec.setMinHitCount(0);
		br3.setQuery(colorQ);             // query is color=red
		br3.addSelection(sel);			  // count facets <30,70,5> and <60,120,1>
		answer.clear();
		answer.put("distance", Arrays.asList(new BrowseFacet[]{new BrowseFacet("30,70:5", 0), new BrowseFacet("60,120:1",1)}));		
		doTest(br3, 1 , answer, null);

	}

	/**
	 * This tests GeoFacetHandler
	 * @throws Exception
	 */
	public void testGeo() throws Exception{
		// testing facet counts for two distance facets - <30,70,5>, <60,120,1>
		BrowseRequest br=new BrowseRequest();
		br.setCount(10);
		br.setOffset(0);

        BrowseSelection sel=new BrowseSelection("correctDistance");
        sel.addValue("30,75:100");
        sel.addValue("60,120:1");
        br.addSelection(sel); 
		
		FacetSpec geoSpec=new FacetSpec();
		geoSpec.setMinHitCount(0);
		geoSpec.setOrderBy(FacetSortSpec.OrderValueAsc);
		br.setFacetSpec("correctDistance", geoSpec);
		
		HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
		answer.put("correctDistance", Arrays.asList(new BrowseFacet[]{new BrowseFacet("30,75:100",1),new BrowseFacet("60,120:1",2)}));
		doTest(br,3,answer,null);

		// testing for selection of facet <60,120,1> and verifying that 2 documents match this facet.
		BrowseRequest br2 = new BrowseRequest();
		br2.setCount(10);
		br2.setOffset(0);	

		BrowseSelection sel2 = new BrowseSelection("correctDistance");
		sel2.addValue("60,120:1");
		HashMap<String, Float> map = new HashMap<String, Float>();
		map.put("60,120:1", 3.0f);
		FacetTermQuery geoQ = new FacetTermQuery(sel2,map);
		
		BoboBrowser b = newBrowser();
		Explanation expl = b.explain(geoQ, 0);
		
		br2.setQuery(geoQ);
		doTest(br2,2,null,new String[]{"1","5"});

		expl = b.explain(geoQ, 1);
	    
	    // facet query for color "red" and getting facet counts for the distance facet.
		BrowseRequest br3 = new BrowseRequest();
		br3.setCount(10);
		br3.setOffset(0);	

		BrowseSelection sel3 = new BrowseSelection("color");
		sel3.addValue("red");
		HashMap<String, Float> map3 = new HashMap<String, Float>();
		map3.put("red", 3.0f);
		FacetTermQuery colorQ = new FacetTermQuery(sel3,map3);

		BoboBrowser b2 = newBrowser();
		Explanation expl2 = b.explain(colorQ, 0);

		br3.setFacetSpec("correctDistance", geoSpec);
		geoSpec.setMinHitCount(1);
		br3.setQuery(colorQ);             // query is color=red
		br3.addSelection(sel);			  // count facets <30,70,5> and <60,120,1>
		answer.clear();
		answer.put("correctDistance", Arrays.asList(new BrowseFacet[]{new BrowseFacet("60,120:1",1)}));		
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

			public IntComparator newComparator(
					FieldValueAccessor fieldValueAccessor, final int[] counts) {
				return new IntComparator(){

					public int compare(Integer f1, Integer f2) {
						int val = counts[f2] - counts[f1];
						if (val==0)
				        {
				            val=f2-f1;
				        }
				        return val;
					}
					
					public int compare(int f1, int f2) {
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
	
	/**
	 * Do the test and check result. 
	 * @param req
	 * @param numHits
	 * @param choiceMap
	 * @param ids
	 */
	private BrowseResult  doTest(BrowseRequest req,int numHits,HashMap<String,List<BrowseFacet>> choiceMap,String[] ids){
	  return doTest((BoboBrowser)null,req,numHits,choiceMap,ids);
    }
	
	private BrowseResult doTest(BoboBrowser boboBrowser,BrowseRequest req,int numHits,HashMap<String,List<BrowseFacet>> choiceMap,String[] ids) {
	  	BrowseResult result = null;
	  	try {
	        if (boboBrowser==null) {
	  		boboBrowser=newBrowser();
	  	  }
	        result = boboBrowser.browse(req);
	        doTest(result,req,numHits,choiceMap,ids);
	        return result;// result;
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
	  	      if (result!=null)result.close();
	  			boboBrowser.close();
	  		} catch (IOException e) {
	  			fail(e.getMessage());
	  		}
	  	  }
	  	}
	  	return null;// null;
	  }
	
	public void testLuceneSort() throws IOException
	{
	  
	  IndexReader srcReader=IndexReader.open(_indexDir,true);
      try{
        List<FacetHandler<?>> facetHandlers = new ArrayList<FacetHandler<?>>();
        facetHandlers.add(new SimpleFacetHandler("id"));
        
        BoboIndexReader reader= BoboIndexReader.getInstance(srcReader,facetHandlers, null);       // not facet handlers to help
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
	
	public void testNoCount(){
		BrowseRequest br=new BrowseRequest();
	    br.setCount(0);
	    br.setOffset(0);
	      
	    BrowseSelection sel=new BrowseSelection("color");
	    sel.addValue("red");
	    br.addSelection(sel);
	      
	    FacetSpec ospec=new FacetSpec();
	    ospec.setExpandSelection(false);
	    br.setFacetSpec("color", ospec);
	     

	    HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
	    answer.put("color", Arrays.asList(new BrowseFacet[]{new BrowseFacet("red",3)}));
	     
	    doTest(br,3,null,new String[0]);
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
    
    public void testMultiSort(){
  	  // no sel
  	  BrowseRequest br=new BrowseRequest();
        br.setCount(10);
        br.setOffset(0);


        br.setSort(new SortField[]{new SortField("color",SortField.CUSTOM,false),new SortField("number",SortField.CUSTOM,true)});

        doTest(br,7,null,new String[]{"5","4","6","3","2","1","7"});
        
        // now test with serialization
        
        BrowseResult result = null;
        BoboBrowser boboBrowser=null;
        try {
            boboBrowser=newBrowser();
          
            result = boboBrowser.browse(br);
            
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(result);
            oout.flush();
            byte[] serialized = bout.toByteArray();
            
            ByteArrayInputStream bin = new ByteArrayInputStream(serialized);
            ObjectInputStream oin = new ObjectInputStream(bin);
            
            result = (BrowseResult)oin.readObject();
            
            doTest(result,br,7,null,new String[]{"5","4","6","3","2","1","7"});
            
        } catch (BrowseException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        catch(Exception ioe){
        	ioe.printStackTrace();
          fail(ioe.getMessage());
        }
        finally{
          if (boboBrowser!=null){
            try {
              if(result!=null) result.close();
                boboBrowser.close();
            } catch (IOException e) {
                fail(e.getMessage());
            }
          }
        }
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

			final class CustomSortDocComparator extends DocComparator implements Serializable{

				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

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
      assertEquals(facet.getFacetValueHitCount(), 1);
      res.close();
	}
	
	public void testQueryWithScore() throws Exception{
		BrowseRequest br=new BrowseRequest();
		br.setShowExplanation(false);	// default
		  QueryParser parser=new QueryParser(Version.LUCENE_CURRENT,"color",new StandardAnalyzer(Version.LUCENE_CURRENT));
		  br.setQuery(parser.parse("color:red OR shape:square"));
	      br.setCount(10);
	      br.setOffset(0);
	      
	      br.setSort(new SortField[]{SortField.FIELD_SCORE});
	      BrowseResult res = doTest(br,4,null,new String[]{"1","7","2","5"});
	      
	      BrowseHit[] hits = res.getHits();
	      for (BrowseHit hit : hits){
	    	  assertNull(hit.getExplanation());
	      }
	      
	      br.setShowExplanation(true);
	      res = doTest(br,4,null,new String[]{"1","7","2","5"});
	      hits = res.getHits();
	      for (BrowseHit hit : hits){
	    	  assertNotNull(hit.getExplanation());
	      }
	      
	      Query rawQuery = br.getQuery();
	      
          SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
	      Date d = format.parse("2006/06/01");
	      long fromTime = d.getTime();
	      
	      RecencyBoostScorerBuilder recencyBuilder = new RecencyBoostScorerBuilder("date", 2.0f, TimeUnit.DAYS.convert(fromTime,TimeUnit.MILLISECONDS), 30L, TimeUnit.DAYS);
	      ScoreAdjusterQuery sq = new ScoreAdjusterQuery(rawQuery,recencyBuilder);
	      br.setQuery(sq);
	      
          res = doTest(br,4,null,new String[]{"7","1","2","5"});
	      
	      hits = res.getHits();
	      for (BrowseHit hit : hits){
	    	  assertNotNull(hit.getExplanation());
	    	  System.out.println(hit.getExplanation());
	      }
	      
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
	
  public void testBrowseMultiValWithWeight()
  {
    BrowseRequest br=new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);
    BrowseSelection sel=new BrowseSelection("multiwithweight");
    sel.addValue("cool");
    br.addSelection(sel);
    

    FacetSpec ospec=new FacetSpec();
    br.setFacetSpec("multiwithweight", ospec);
    br.setSort(new SortField[]{new SortField("multiwithweight",SortField.CUSTOM,true)});
    HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();

    answer.put("multiwithweight", Arrays.asList(new BrowseFacet[]{new BrowseFacet("cool",3),new BrowseFacet("good",2)}));
      
    doTest(br,3,answer,new String[]{"1","2","3"});
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
		doTest(br,7,answer,null);
	}
	
	public void testNewRangeFacet(){
	  BrowseRequest br = new BrowseRequest();
	  br.setCount(10);
	  br.setOffset(0);
	  
	  FacetSpec simpleOutput = new FacetSpec();
	  simpleOutput.setExpandSelection(true);
	  br.setFacetSpec("date", simpleOutput);
	  
//	  d1.add(buildMetaField("date","2000/01/01"));
//	  d3.add(buildMetaField("date","2001/12/25"));
//	  d5.add(buildMetaField("date","2002/03/08"));
//	  d2.add(buildMetaField("date","2003/02/14"));
//	  d4.add(buildMetaField("date","2004/11/24"));
//	  d7.add(buildMetaField("date","2006/06/01"));
//	  d6.add(buildMetaField("date","2007/08/01"));
	  
      BrowseSelection sel1 = new BrowseSelection("date");
      sel1.setValues(new String[]{"(2000/01/01 TO 2003/02/14]"});
      BrowseSelection sel2 = new BrowseSelection("date");
      sel2.setValues(new String[]{"(2000/01/01 TO 2003/02/14)"});
      
      
      br.addSelection(sel1);
      
      HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
      answer.put("date",  Arrays.asList(new BrowseFacet[]{new BrowseFacet("[2000/01/01 TO 2003/02/14]", 4), new BrowseFacet("[2003/05/06 TO 2005/04/04]",1)}));
      doTest(br,3,null,null);
      
      br.clearSelections();
      br.addSelection(sel2);
      doTest(br, 2, null, null);
      
      
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
		doTest(br,7,answer,null);
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
      mergedResult.close();
      multiBoboBrowser.close();
	}
	

	public void testFacetQueryBoost() throws Exception{
		BrowseSelection sel = new BrowseSelection("color");
		sel.addValue("red");
		sel.addValue("blue");
		HashMap<String, Float> map = new HashMap<String, Float>();
		map.put("red", 5.0f);
		map.put("blue", 4.0f);
		FacetTermQuery colorQ = new FacetTermQuery(sel,map);
		
		BrowseSelection sel2 = new BrowseSelection("shape");
		sel2.addValue("circle");
		sel2.addValue("square");
		HashMap<String, Float> map2 = new HashMap<String, Float>();
		map2.put("circle", 3.0f);
		map2.put("square", 2.0f);
		FacetTermQuery shapeQ = new FacetTermQuery(sel2,map2);
		shapeQ.setBoost(3.0f);
		
		BooleanQuery bq = new BooleanQuery();
		bq.add(shapeQ,Occur.SHOULD);
		bq.add(colorQ,Occur.SHOULD);
		
		BrowseRequest br = new BrowseRequest();
		br.setSort(new SortField[]{SortField.FIELD_SCORE});
		br.setQuery(bq);
		br.setOffset(0);
		br.setCount(10);
		
		
		BrowseResult res = doTest(br,6,null,new String[]{"4","1","7","5","3","2"});
		BrowseHit[] hits = res.getHits();
		float[] scores = new float[]{13,11,11,10,4.5f,2.5f};  // default coord = 1/2
		for (int i=0;i<hits.length;++i){
			assertEquals(scores[i],hits[i].getScore());
		}
	}
	
	public void testFacetQuery() throws Exception{
		BrowseSelection sel = new BrowseSelection("color");
		sel.addValue("red");
		sel.addValue("blue");
		HashMap<String, Float> map = new HashMap<String, Float>();
		map.put("red", 3.0f);
		map.put("blue", 2.0f);
		FacetTermQuery colorQ = new FacetTermQuery(sel,map);
		
		BrowseSelection sel2 = new BrowseSelection("tag");
		sel2.addValue("rabbit");
		sel2.addValue("dog");
		HashMap<String, Float> map2 = new HashMap<String, Float>();
		map2.put("rabbit", 100.0f);
		map2.put("dog", 50.0f);
		FacetTermQuery tagQ = new FacetTermQuery(sel2,map2);
		
		
		BrowseRequest br = new BrowseRequest();
		br.setQuery(colorQ);
		br.setOffset(0);
		br.setCount(10);
		
		doTest(br,5,null,new String[]{"1","2","7","4","5"});
		
		//BoboBrowser b = newBrowser();
	//	Explanation expl = b.explain(colorQ, 0);
		
		br.setQuery(tagQ);
		doTest(br,4,null,new String[]{"7","1","3","2"});
	//	expl = b.explain(tagQ, 6);
		
	}
	
	 public void testFacetQueryBoolean() throws Exception{
	    BrowseSelection sel = new BrowseSelection("color");
	    sel.addValue("red");
	    sel.addValue("blue");
	    HashMap<String, Float> map = new HashMap<String, Float>();
	    map.put("red", 3.0f);
	    map.put("blue", 2.0f);
	    FacetTermQuery colorQ = new FacetTermQuery(sel,map);
	    
	    BrowseSelection sel2 = new BrowseSelection("tag");
	    sel2.addValue("rabbit");
	    sel2.addValue("dog");
	    HashMap<String, Float> map2 = new HashMap<String, Float>();
	    map2.put("rabbit", 100.0f);
	    map2.put("dog", 50.0f);
	    FacetTermQuery tagQ = new FacetTermQuery(sel2,map2);
	    
	    
	    BrowseRequest br = new BrowseRequest();

	    br.setOffset(0);
	    br.setCount(10);
	    
	    
	    BooleanQuery bq = new BooleanQuery(true);
	    bq.add(colorQ, Occur.SHOULD);
	    bq.add(tagQ, Occur.SHOULD);
	    
	    br.setQuery(bq);
	    doTest(br, 6, null, new String[]{"7","1","3","2","4","5"});
	    
	    
	  }
	
	public void testFacetRangeQuery() throws Exception{
		BrowseSelection sel = new BrowseSelection("numendorsers");
		sel.addValue("[* TO 000010]");
		
		HashMap<String, Float> map = new HashMap<String, Float>();
		map.put("000002", 100.0f);
		map.put("000010", 50.0f);
		FacetTermQuery numberQ = new FacetTermQuery(sel,map);
		
		BrowseRequest br = new BrowseRequest();
		br.setQuery(numberQ);
		br.setOffset(0);
		br.setCount(10);
		
		doTest(br,4,null,new String[]{"5","2","1","6"});
	}
	
	public void testFacetBoost() throws Exception{
	  Map<String,Map<String,Float>> boostMaps = new HashMap<String,Map<String,Float>>();
      HashMap<String,Float> map;

	  map = new HashMap<String, Float>();
	  map.put("red", 3.0f);
	  map.put("blue", 2.0f);
      boostMaps.put("color", map);
	  
	  map = new HashMap<String,Float>();
	  map.put("rabbit", 5.0f);
	  map.put("dog", 7.0f);
      boostMaps.put("tag", map);
      
      Query q = new ScoreAdjusterQuery(new MatchAllDocsQuery(), new FacetBasedBoostScorerBuilder(boostMaps));
      
      BrowseRequest br = new BrowseRequest();
      br.setQuery(q);
	  br.setOffset(0);
	  br.setCount(10);
      br.setSort(new SortField[]{SortField.FIELD_SCORE});
      BoboBrowser b = newBrowser();

      BrowseResult r = b.browse(br);
      
      doTest(r, br,7,null,new String[]{"7","2","1","3","4","5","6"});

//      int firstDoc = r.getHits()[0].getDocid();
//      Explanation expl = b.explain(q, firstDoc);
//      System.out.println(">>> " + expl.toString());
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
			
			public IntComparator newComparator(final FieldValueAccessor fieldValueAccessor,
					final int[] counts) {
				
				return new IntComparator(){

					public int compare(Integer v1, Integer v2) {
						Integer size1 = (Integer)fieldValueAccessor.getRawValue(v1);
						Integer size2 = (Integer)fieldValueAccessor.getRawValue(v2);
						
						int val = size1-size2;
						if (val == 0){
							val = counts[v1]-counts[v2];
						}
						return val;
					}
					
          public int compare(int v1, int v2) {
            int size1 = (Integer)fieldValueAccessor.getRawValue(v1);
            int size2 = (Integer)fieldValueAccessor.getRawValue(v2);
            
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
							val = o2.getFacetValueHitCount()-o1.getFacetValueHitCount();
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
	
	public void testIndexReaderReopen() throws Exception{
		Directory idxDir = new RAMDirectory();
		Document[] docs = buildData();
		
		IndexWriter writer = new IndexWriter(idxDir,new StandardAnalyzer(Version.LUCENE_CURRENT),MaxFieldLength.UNLIMITED);
		writer.addDocument(docs[0]);
		writer.optimize();
		writer.commit();
		
		IndexReader idxReader = IndexReader.open(idxDir,true);
		BoboIndexReader boboReader = BoboIndexReader.getInstance(idxReader,_fconf);

		
		for (int i=1;i<docs.length;++i){
			Document doc = docs[i];
			int numDocs = boboReader.numDocs();
			BoboIndexReader reader = (BoboIndexReader)boboReader.reopen(true);
			assertSame(boboReader,reader);
			
			Directory tmpDir = new RAMDirectory();
			IndexWriter subWriter = new IndexWriter(tmpDir,new StandardAnalyzer(Version.LUCENE_CURRENT),MaxFieldLength.UNLIMITED);
			subWriter.addDocument(doc);
			subWriter.optimize();
			subWriter.close();
			writer.addIndexesNoOptimize(new Directory[]{tmpDir});
			writer.commit();
			reader = (BoboIndexReader)boboReader.reopen();
			assertNotSame(boboReader, reader);
			assertEquals(numDocs+1,reader.numDocs());
			boboReader = reader;
		}
		writer.deleteDocuments(new Term("id","1"));
		writer.commit();
		int numDocs = boboReader.numDocs();
		BoboIndexReader newReader = (BoboIndexReader)boboReader.reopen();
		assertNotSame(newReader,boboReader);
		int numDocs2 = newReader.numDocs();
		if (boboReader!=newReader){
			boboReader.close();
			boboReader = newReader;
		}
		assertEquals(numDocs-1,numDocs2);
		boboReader.close();
	}
	public void testTime() throws Exception
	{
    List<FacetHandler<?>> facetHandlers = new ArrayList<FacetHandler<?>>();
    /* Underlying time facet for DynamicTimeRangeFacetHandler */
    facetHandlers.add(new RangeFacetHandler("timeinmillis", new PredefinedTermListFactory(Long.class, DynamicTimeRangeFacetHandler.NUMBER_FORMAT),null));
    Directory idxDir = new RAMDirectory();
    IndexWriter writer = new IndexWriter(idxDir,new StandardAnalyzer(Version.LUCENE_CURRENT),MaxFieldLength.UNLIMITED);
	  
	  long now = System.currentTimeMillis();
	  DecimalFormat df = new DecimalFormat(DynamicTimeRangeFacetHandler.NUMBER_FORMAT);
	  for(long l=0; l<53; l++)
	  {
	    Document d = new Document();
	    d.add(buildMetaField("timeinmillis", df.format(now - l*3500000)));
	    writer.addDocument(d);
	    writer.optimize();
	    writer.commit();
	  }
    IndexReader idxReader = IndexReader.open(idxDir,true);
    BoboIndexReader boboReader = BoboIndexReader.getInstance(idxReader,facetHandlers);
    BoboBrowser browser = new BoboBrowser(boboReader);
    List<String> ranges = new ArrayList<String>();
    ranges.add("000000001");
    ranges.add("000010000");// one hour
    ranges.add("000020000");// two hours
    ranges.add("000030000");
    ranges.add("000040000");
    ranges.add("001000000");// one day
    ranges.add("002000000");// two days
    ranges.add("003000000");
    ranges.add("004000000");
    FacetHandler<?> facetHandler = new DynamicTimeRangeFacetHandler("timerange", "timeinmillis", now, ranges );
    browser.setFacetHandler(facetHandler );
//  
    BrowseRequest req = new BrowseRequest();
    BrowseFacet facet = null;
    FacetSpec facetSpec = new FacetSpec();
    req.setFacetSpec("timerange", facetSpec);
    BrowseResult result = browser.browse(req);
    FacetAccessible facetholder = result.getFacetAccessor("timerange");
    List<BrowseFacet> facets = facetholder.getFacets();
    facet = facets.get(0);
    assertEquals("order by value", "000000001", facet.getValue());
    assertEquals("order by value", 1 , facet.getFacetValueHitCount());
    facet = facets.get(1);
    assertEquals("order by value", "000010000", facet.getValue());
    assertEquals("order by value", 1 , facet.getFacetValueHitCount());
    facet = facets.get(5);
    assertEquals("order by value", "001000000", facet.getValue());
    assertEquals("order by value", 20 , facet.getFacetValueHitCount());
    facet = facets.get(7);
    assertEquals("order by value", "003000000", facet.getValue());
    assertEquals("order by value", 3 , facet.getFacetValueHitCount());
//  
    req = new BrowseRequest();
    facetSpec = new FacetSpec();
    facetSpec.setMinHitCount(0);
    facetSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
    req.setFacetSpec("timerange", facetSpec);
    result = browser.browse(req);
    facetholder = result.getFacetAccessor("timerange");
    facets = facetholder.getFacets();
    facet = facets.get(0);
    assertEquals("", "002000000", facet.getValue());
    assertEquals("", 25 , facet.getFacetValueHitCount());
    facet = facets.get(1);
    assertEquals("", "001000000", facet.getValue());
    assertEquals("", 20 , facet.getFacetValueHitCount());
    facet = facets.get(2);
    assertEquals("", "003000000", facet.getValue());
    assertEquals("", 3 , facet.getFacetValueHitCount());
    facet = facets.get(8);
    assertEquals("minCount=0", "004000000", facet.getValue());
    assertEquals("minCount=0", 0 , facet.getFacetValueHitCount());
//  
    req = new BrowseRequest();
    facetSpec = new FacetSpec();
    BrowseSelection sel = new BrowseSelection("timerange");
    sel.addValue("001000000");
    req.addSelection(sel);
    facetSpec.setExpandSelection(true);
    req.setFacetSpec("timerange", facetSpec);
    result = browser.browse(req);
    facetholder = result.getFacetAccessor("timerange");
    facets = facetholder.getFacets();
    facet = facets.get(0);
    assertEquals("", "000000001", facet.getValue());
    assertEquals("", 1 , facet.getFacetValueHitCount());
    facet = facets.get(6);
    assertEquals("", "002000000", facet.getValue());
    assertEquals("", 25 , facet.getFacetValueHitCount());
    facet = facets.get(7);
    assertEquals("", "003000000", facet.getValue());
    assertEquals("", 3 , facet.getFacetValueHitCount());
//  
    req = new BrowseRequest();
    facetSpec = new FacetSpec();
    sel = new BrowseSelection("timerange");
    sel.addValue("001000000");
    sel.addValue("003000000");
    sel.addValue("004000000");
    req.addSelection(sel );
    facetSpec.setExpandSelection(false);
    req.setFacetSpec("timerange", facetSpec);
    result = browser.browse(req);
    facetholder = result.getFacetAccessor("timerange");
    facet = facetholder.getFacet("001000000");
    assertEquals("001000000", 20, facet.getFacetValueHitCount());
    facet = facetholder.getFacet("003000000");
    assertEquals("003000000", 3, facet.getFacetValueHitCount());
    facet = facetholder.getFacet("004000000");
    assertEquals("004000000", 0, facet.getFacetValueHitCount());
    assertEquals("",23,result.getNumHits());
	}
	
	public void testHistogramFacetHandler() throws Exception{
		BrowseRequest br=new BrowseRequest();
	    br.setCount(0);
	    br.setOffset(0);
	    
	    FacetSpec output=new FacetSpec();
	    output.setMaxCount(100);
	    output.setMinHitCount(1);
	    br.setFacetSpec("numberhisto", output);
	    
	    
	    BrowseFacet[] answerBucketFacets = new BrowseFacet[5];     
	    answerBucketFacets[0] =  new BrowseFacet("0000000000", 3);
	    answerBucketFacets[1] =  new BrowseFacet("0000000002", 1);
	    answerBucketFacets[2] =  new BrowseFacet("0000000009", 1);
	    answerBucketFacets[3] =  new BrowseFacet("0000000010",1);
	    answerBucketFacets[4] =  new BrowseFacet("0000000021",1);
      
        HashMap<String,List<BrowseFacet>> answer = new HashMap<String,List<BrowseFacet>>();
        answer.put("numberhisto", Arrays.asList(answerBucketFacets)); 
      
	    
        doTest(br,7,answer,null);
        
        
        // now with selection
        
        BrowseSelection sel = new BrowseSelection("color");
        sel.addValue("green");
        br.addSelection(sel);
        
        answerBucketFacets = new BrowseFacet[2]; 
        answerBucketFacets[0] =  new BrowseFacet("0000000002",1);
        answerBucketFacets[1] =  new BrowseFacet("0000000021",1);
        
        answer = new HashMap<String,List<BrowseFacet>>();
        answer.put("numberhisto", Arrays.asList(answerBucketFacets)); 
      
	    
        doTest(br,2,answer,null);
	}
	

	 public void testBucketFacetHandlerForNumbers() throws Exception{
		 /*
		  * 
		  * 
		String[][] predefinedBuckets2 = new String[3][];
		predefinedBuckets2[0] =  new String[]{"2","3"};
		predefinedBuckets2[1] =  new String[]{"1","4"};
		predefinedBuckets2[2] =  new String[]{"7","8"};
        
        Map<String,String[]> predefinedNumberSets = new HashMap<String,String[]>();
        predefinedNumberSets.put("s1", predefinedBuckets2[0]);
        predefinedNumberSets.put("s2", predefinedBuckets2[1]);
        predefinedNumberSets.put("s3", predefinedBuckets2[2]);
		  */
		 BrowseRequest br=new BrowseRequest();
		    br.setCount(10);
		    br.setOffset(0);
		    
		    FacetSpec output=new FacetSpec();
		    output.setOrderBy(FacetSortSpec.OrderHitsDesc);
		    br.setFacetSpec("sets", output);
	      
		    BrowseFacet[] answerBucketFacets = new BrowseFacet[3];     
		    answerBucketFacets[0] =  new BrowseFacet("s1", 5);
		    answerBucketFacets[1] =  new BrowseFacet("s2", 4);
		    answerBucketFacets[2] =  new BrowseFacet("s3", 3);
	      
	        HashMap<String,List<BrowseFacet>> answer = new HashMap<String,List<BrowseFacet>>();
	        answer.put("sets", Arrays.asList(answerBucketFacets)); 
		    doTest(br,7,answer,null);
		    
		    br=new BrowseRequest();
		    br.setCount(10);
		    br.setOffset(0);
		    
		    BrowseSelection sel=new BrowseSelection("sets");
	        sel.addValue("s1");
	        br.addSelection(sel);
		    
		    output=new FacetSpec();
		    output.setOrderBy(FacetSortSpec.OrderHitsDesc);
		    br.setFacetSpec("sets", output);
	      
		    answerBucketFacets = new BrowseFacet[3];     
		    answerBucketFacets[0] =  new BrowseFacet("s1", 5);
		    answerBucketFacets[1] =  new BrowseFacet("s2", 3);
		    answerBucketFacets[2] =  new BrowseFacet("s3", 1);
	      
	        answer = new HashMap<String,List<BrowseFacet>>();
	        answer.put("sets", Arrays.asList(answerBucketFacets)); 
		    doTest(br,4,answer,null);
	 }
	 
	 public void testBucketFacetHandlerForStrings() throws Exception{
	    BrowseRequest br=new BrowseRequest();
	    br.setCount(10);
	    br.setOffset(0);
	    
	    BrowseSelection sel=new BrowseSelection("groups");
        sel.addValue("g2");
        br.addSelection(sel);
    
	    FacetSpec output=new FacetSpec();
	    output.setOrderBy(FacetSortSpec.OrderHitsDesc);
	    br.setFacetSpec("groups", output);
      
	    BrowseFacet[] answerBucketFacets = new BrowseFacet[3];     
	    answerBucketFacets[0] =  new BrowseFacet("g2", 3);
	    answerBucketFacets[1] =  new BrowseFacet("g1", 1);
	    answerBucketFacets[2] =  new BrowseFacet("g3", 1);
      
        HashMap<String,List<BrowseFacet>> answer = new HashMap<String,List<BrowseFacet>>();
        answer.put("groups", Arrays.asList(answerBucketFacets)); 
	    doTest(br,3,answer,null);
	    
	    br=new BrowseRequest();
	    br.setCount(10);
	    br.setOffset(0);
	    
	    sel=new BrowseSelection("groups");
        sel.addValue("g2");
        sel.addValue("g1");
        sel.setSelectionOperation(ValueOperation.ValueOperationAnd);
        br.addSelection(sel);
    
	    output=new FacetSpec();
	    output.setOrderBy(FacetSortSpec.OrderHitsDesc);
	    br.setFacetSpec("groups", output);
      
	    answerBucketFacets = new BrowseFacet[2];     
	    answerBucketFacets[0] =  new BrowseFacet("g1", 1);
	    answerBucketFacets[1] =  new BrowseFacet("g2", 1);
      
      answer = new HashMap<String,List<BrowseFacet>>();
      answer.put("groups", Arrays.asList(answerBucketFacets)); 
	    doTest(br,1,answer,null);
	    
	    br=new BrowseRequest();
	    br.setCount(10);
	    br.setOffset(0);
	    
	    sel=new BrowseSelection("groups");
        sel.addValue("g2");
        sel.addValue("g1");
        sel.setSelectionOperation(ValueOperation.ValueOperationOr);
        br.addSelection(sel);
    
	    output=new FacetSpec();
	    output.setOrderBy(FacetSortSpec.OrderHitsDesc);
	    br.setFacetSpec("groups", output);
      
	    answerBucketFacets = new BrowseFacet[3];     
	    answerBucketFacets[0] =  new BrowseFacet("g1", 3);
	    answerBucketFacets[1] =  new BrowseFacet("g2", 3);
	    answerBucketFacets[2] =  new BrowseFacet("g3", 1);
      
      answer = new HashMap<String,List<BrowseFacet>>();
      answer.put("groups", Arrays.asList(answerBucketFacets)); 
	    doTest(br,5,answer,null);
    
	  }
	 
	public static void main(String[] args)throws Exception {
		//BoboTestCase test=new BoboTestCase("testSimpleGroupbyFacetHandler");
	  BoboTestCase test=new BoboTestCase("testFacetRangeQuery");
		test.setUp();
		test.testFacetRangeQuery();
		test.tearDown();
	}

  public void testVirtual() throws Exception{
    BrowseRequest br=new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);

    BrowseSelection sel=new BrowseSelection("virtual");
    sel.addValue("10");
    sel.addValue("11");
    br.addSelection(sel); 
    
    FacetSpec spec = new FacetSpec();
    spec.setOrderBy(FacetSortSpec.OrderValueAsc);
    br.setFacetSpec("virtual", spec);
    
    HashMap<String,List<BrowseFacet>> answer=new HashMap<String,List<BrowseFacet>>();
    answer.put("virtual", Arrays.asList(new BrowseFacet[]{new BrowseFacet("0010", 1), new BrowseFacet("0011", 1)}));
    doTest(br, 2, answer, new String[] {"1", "2"});
  }
}
