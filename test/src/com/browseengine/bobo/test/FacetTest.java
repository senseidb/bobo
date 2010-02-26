package com.browseengine.bobo.test;

import java.io.File;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;

public class FacetTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		File idx = new File("/Users/jwang/dataset/people-search-index-norm/beef");
		
		Directory idxDir = FSDirectory.open(idx);
		IndexReader reader=IndexReader.open(idxDir,true);
		
		BoboIndexReader boboReader=BoboIndexReader.getInstance(reader);
		BoboBrowser browser=new BoboBrowser(boboReader);
		int iter=1000000;
		for (int i=0;i<iter;++i)
		{
		  doBrowse(browser);
		}
	}
	
	static void doBrowse(BoboBrowser browser) throws Exception
	{
		String q="java";
		QueryParser parser=new QueryParser(Version.LUCENE_CURRENT,"b",new StandardAnalyzer(Version.LUCENE_CURRENT));
		Query query=parser.parse(q);
		BrowseRequest br=new BrowseRequest();
		//br.setQuery(query);
		br.setOffset(0);
		br.setCount(0);

        BrowseSelection geoSel=new BrowseSelection("geo_region");
        geoSel.addValue("5227");
        BrowseSelection industrySel=new BrowseSelection("industry_norm");
        industrySel.addValue("1");
		
        //br.addSelection(geoSel);
        br.addSelection(industrySel);
		
		FacetSpec regionSpec=new FacetSpec();
		regionSpec.setExpandSelection(true);
		regionSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
		regionSpec.setMaxCount(5);
		
        FacetSpec industrySpec=new FacetSpec();
        industrySpec.setExpandSelection(true);
        industrySpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
        industrySpec.setMaxCount(5);
        

        FacetSpec numEndorserSpec=new FacetSpec();
        numEndorserSpec.setExpandSelection(true);
    
		br.setFacetSpec("industry_norm", industrySpec);
        br.setFacetSpec("geo_region", regionSpec);
        br.setFacetSpec("num_endorsers_norm", numEndorserSpec);

		long start=System.currentTimeMillis();
		BrowseResult res=browser.browse(br);
		long end=System.currentTimeMillis();
		System.out.println("result: "+res);
		System.out.println("took: "+(end-start));
	}

}
