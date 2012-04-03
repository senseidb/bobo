package com.browseengine.bobo.test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.RAMDirectory;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.impl.PathFacetHandler;

public class TestPathMultiVal extends TestCase {
	
	private RAMDirectory directory;
	private Analyzer analyzer;
	private List<FacetHandler<?>> facetHandlers;
	
	static final String PathHandlerName = "path";

	public TestPathMultiVal(String name) {
		super(name);
		facetHandlers = new LinkedList<FacetHandler<?>>();
	}

    private void addMetaDataField(Document doc, String name,String[] vals)
    {
      for (String val : vals){
        Field field = new Field(name, val,Store.NO,Index.NOT_ANALYZED_NO_NORMS);
        field.setOmitTermFreqAndPositions(true);
        doc.add(field);
      }
    }
	  
	@Override
	protected void setUp() throws Exception {
	    directory = new RAMDirectory();
	    analyzer = new WhitespaceAnalyzer();
	    IndexWriter writer = new IndexWriter(directory, analyzer, true, MaxFieldLength.UNLIMITED);
	    Document doc = new Document();
	    addMetaDataField(doc,PathHandlerName,new String[]{"/a/b/c","/a/b/d"});
	    writer.addDocument(doc);
	    writer.commit();
	    
	    PathFacetHandler pathHandler = new PathFacetHandler("path",true);
	    facetHandlers.add(pathHandler);
	}
	
	public void testMultiValPath() throws Exception{
		IndexReader reader = IndexReader.open(directory,true);
		BoboIndexReader boboReader = BoboIndexReader.getInstance(reader, facetHandlers);
		
		BoboBrowser browser = new BoboBrowser(boboReader);
		BrowseRequest req = new BrowseRequest();
		
		BrowseSelection sel = new BrowseSelection(PathHandlerName);
		sel.addValue("/a");
		HashMap<String,String> propMap = new HashMap<String,String>();
		propMap.put(PathFacetHandler.SEL_PROP_NAME_DEPTH,"0");
		propMap.put(PathFacetHandler.SEL_PROP_NAME_STRICT,"false");
		sel.setSelectionProperties(propMap);
		
		req.addSelection(sel);
		
		FacetSpec fs = new FacetSpec();
		fs.setMinHitCount(1);
		req.setFacetSpec(PathHandlerName, fs);
		
		BrowseResult res = browser.browse(req);
		assertEquals(res.getNumHits(),1);
		FacetAccessible fa = res.getFacetAccessor(PathHandlerName);
		List<BrowseFacet> facets = fa.getFacets();
		System.out.println(facets);
		assertEquals(1,facets.size());
		BrowseFacet facet = facets.get(0);
		assertEquals(2,facet.getFacetValueHitCount());
	}
}
