package com.browseengine.bobo.bench;

import java.io.File;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;

public class BoboLocalClient
{

  private static class SearchRunner extends Thread
  {
    private final BoboIndexReader _reader;
    SearchRunner(BoboIndexReader reader)
    {
      _reader = reader;
    }
    
    public void run()
    {
      int numIter=1000000;
      try
      {
        for (int i=0;i<numIter;++i)
        {

          BoboBrowser svc = new BoboBrowser(_reader);
          BrowseRequest req = new BrowseRequest();
          
          //req.setQuery(q);
          
          //BrowseSelection sel = new BrowseSelection("ccid");
          //sel.addValue("1009");
          //req.addSelection(sel);
          
          BrowseSelection sel2 = new BrowseSelection("industry");
          sel2.addValue("4");
          req.addSelection(sel2);
          
          FacetSpec fspec = new FacetSpec();
          fspec.setExpandSelection(true);
          fspec.setMaxCount(10);
          fspec.setOrderBy(FacetSortSpec.OrderHitsDesc);
          
           req.setFacetSpec("ccid", fspec);
           req.setFacetSpec("pcid", fspec);
           req.setFacetSpec("education_id", fspec);
           req.setFacetSpec("geo_region", fspec);
           req.setFacetSpec("geo_country", fspec);
           req.setFacetSpec("industry", fspec);
           req.setFacetSpec("proposal_accepts", fspec);
           req.setFacetSpec("num_endorsers", fspec);
           //req.setFacetSpec("group_id", fspec);
          long start = System.currentTimeMillis();
          
          BrowseResult result = svc.browse(req);
          long end = System.currentTimeMillis();
          System.out.println("took: "+(end-start));
          svc.close();
          
          Thread.sleep(200);
  
        }
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
  }
  
  /**
   * @param args
   * @throws BrowseException 
   */
  public static void main(String[] args) throws Exception
  {
    File file = new File("/Users/jwang/dataset/facet_idx_2/beef");

    FSDirectory idxDir = FSDirectory.open(file);
    
    IndexReader reader = IndexReader.open(idxDir,true);
    
    long start =System.currentTimeMillis();
    BoboIndexReader boboReader = BoboIndexReader.getInstance(reader);

    long end =System.currentTimeMillis();
    
    System.out.println("load took: "+(end-start));
    int numThreads = 10;
    
    Thread[] threads = new Thread[numThreads];
    for (int i=0;i<numThreads;++i)
    {
      threads[i]=new SearchRunner(boboReader);
    }
    
    for (Thread t : threads)
    {
      t.start();
    }
  }

}
