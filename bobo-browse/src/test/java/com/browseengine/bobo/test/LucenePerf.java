package com.browseengine.bobo.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.lucene.LucenePackage;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.PredefinedTermListFactory;
import com.browseengine.bobo.facets.impl.DefaultFacetCountCollector;
import com.browseengine.bobo.facets.impl.MultiValueFacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;

public class LucenePerf
{
  public static String[] words = { "manager", "university", "in", "business",
      "management", "a", "development", "consultant", "director", "10",
      "services", "on", "senior", "marketing", "project", "sales",
      "technology", "systems", "as", "software", "new", "professional",
      "owner", "experience", "inc", "team", "company" };
  public static final ArrayList<String> wordlist = new ArrayList<String>();
  static final Random rand = new Random(987129);

  public static int inNumItr = 10;
  /**
   * @param args
   * @throws IOException
   * @throws CorruptIndexException
   * @throws InterruptedException
   * @throws BrowseException 
   */
  public static void main(String[] args) throws CorruptIndexException,IOException, InterruptedException, BrowseException
  {
    System.out.println(LucenePackage.get());
    System.out.println(Arrays.toString(args));
    String filename = "/Users/xgu/lucene29test/caches/people-search-index";
    if (args.length>0) filename ="/Users/xgu/lucene29test/caches/people-search-index";
    System.out.println(filename);
    File file = new File(filename);
    FSDirectory directory = new SimpleFSDirectory(file);
//    FSDirectory directory = FSDirectory.getDirectory(file);
    System.out.println(directory.getClass().getName());
    IndexReader reader = IndexReader.open(directory, true);
    loadFile();
//    TermEnum termEnum = reader.terms(new Term("b", ""));
//    while(termEnum.next())
//    {
//      Term t = termEnum.term();
//      wordlist.add(t.text());
//    }
//    words = wordlist.toArray(new String[1]);
    System.out.println("load the words " + words.length);

    final Collection<FacetHandler<?>> facetHandlers = new ArrayList<FacetHandler<?>>();
    facetHandlers.add(new MultiValueFacetHandler("ccid", new PredefinedTermListFactory<Integer>(Integer.class,"0000000000")));
//    facetHandlers.add(new MultiValueFacetHandler("pcid", new PredefinedTermListFactory<Integer>(Integer.class,"0000000000")));
    facetHandlers.add(new SimpleFacetHandler("industry", new PredefinedTermListFactory<Integer>(Integer.class,"0000000000")));
    facetHandlers.add(new SimpleFacetHandler("geo_region"));
    facetHandlers.add(new MultiValueFacetHandler("education_id", new PredefinedTermListFactory<Integer>(Integer.class,"0000000000")));
    long tgetinstance = System.currentTimeMillis();
    final BoboIndexReader boboReader = BoboIndexReader.getInstance(reader, facetHandlers , null);
    System.out.println("getInstanceTime: " + (System.currentTimeMillis() - tgetinstance));
//warming
    for(int x=0; x<30; x++)
    {
      doSearch(5, boboReader, facetHandlers);
    }
    long start = System.currentTimeMillis();
    int numThread = 2;
    System.out.println(numThread+" threads");
    int numItr = 1000;
    long ttime = 0;
    for(int x=1; x<numItr+1; x++)
    {
      long time = doSearch(numThread, boboReader, facetHandlers);
      ttime += time;
      if (x % 20 == 0)
      {
        System.out.println("total time: " + ttime);
        System.out.println("average time: " + ((float)ttime/(float)x/(float)numThread/(float)inNumItr));
      }
    }
    System.out.println("total time: " + ttime);
    System.out.println("number of iterations: "+ numItr + "\t\tnumThread: "+ numThread + "\t\tinner itr: " +inNumItr);
    System.out.println("average time: " + ((float)ttime/(float)numItr/(float)numThread/(float)inNumItr));
    System.out.println(LucenePackage.get());
  }
  private static long doSearch(int numThread,final BoboIndexReader boboReader, final Collection<FacetHandler<?>> facetHandlers) throws IOException,
      CorruptIndexException, InterruptedException
  {
    Thread[] threads = new Thread[numThread];
    final long[] times = new long[numThread];
    for(int x =0; x<threads.length; x++)
    {
      final int y = x;
      threads[x] = new Thread()
      {public void run(){
        try
        {
          times[y] = oneRun(boboReader, facetHandlers);
        } catch (IOException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (BrowseException e)
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }};
    }
    for(int x =0; x<threads.length; x++)
    {
      threads[x].setDaemon(true);
      threads[x].start();
    }      
    long sum = 0;
    for(int x =0; x<threads.length; x++)
    {
      threads[x].join();
      sum+= times[x];
    }
    return sum;
  }
/*
 * [00000001371(3156), 00000001025(2951), 00000001035(2688), 00000001009(2429), 00000157234(2318), 00000001028(1871),
 *  00000001063(1711), 00000002114(1371), 00000001033(1340), 00000001384(1187), 00000001292(1016), 00000001694(993),
 *   00000001483(980), 00000001062(884), 00000001115(883), 00000001441(854), 00000001052(695), 00000001093(681),
 *    00000001714(665), 00000001128(641), 00000224605(619), 00000001053(616), 00000002271(613), 00000001288(609),
 *     00000001038(607), 00000001060(585), 00000001043(573), 00000157240(555), 00000001044(549), 00000001663(546),
 *      00000001231(544), 00000001123(526), 00000001505(497), 00000001120(487), 00000001070(484), 00000001217(480), 
 *      00000001073(478), 00000001006(452), 00000001068(437), 00000001207(432), 00000001066(415), 00000001116(415),
 *       00000001271(415), 00000001015(407), 00000011448(401), 00000001040(399), 00000001235(393), 00000001058(391),
 *        00000001482(382), -00000000001(0)]

 */
  private static long oneRun(BoboIndexReader boboReader,
      Collection<FacetHandler<?>> facetHandlers) throws IOException,
      BrowseException
  {
    long tt = 0;
    long hitscount = 0;
    for(int x=0; x< inNumItr; x++)
    {
      long t0 = System.currentTimeMillis();
      BoboBrowser browser = new BoboBrowser(boboReader);
      BrowseRequest req = new BrowseRequest();
      req.setCount(500);
      FacetSpec spec = new FacetSpec();
      spec.setMaxCount(50);
      spec.setOrderBy(FacetSortSpec.OrderHitsDesc);
//      req.setFacetSpec("ccid", spec);
//      req.setFacetSpec("pcid", spec);
//      req.setFacetSpec("education_id", spec);
      req.setFacetSpec("geo_region", spec);
//      req.setFacetSpec("industry", spec);
      String qstr = words[nextInt()];
//      qstr = "project manager";
      String[] terms = qstr.split(" ");
      BooleanQuery q = new BooleanQuery();
    for(String s : terms)
    {
      q.add( new TermQuery(new Term("b",s)), Occur.MUST);
    }
//      q.add(new MatchAllDocsQuery(), Occur.MUST);
      req.setQuery(q);//new TermQuery(new Term("b",qstr)));
      BrowseSelection sel = new BrowseSelection("ccid");
      sel.addValue("0000001384");
//      req.addSelection(sel );
      BrowseSelection seli = new BrowseSelection("industry");
      seli.addValue("0000000052");
//      req.addSelection(seli );
      long tf0=0;
      long tf1=0;
      BrowseResult bres = browser.browse(req);
      for(Entry<String, FacetAccessible> entry: bres.getFacetMap().entrySet())
      {
//        System.out.println(entry.getKey());
        FacetAccessible fa = entry.getValue();
        tf0 = System.currentTimeMillis();
        List<BrowseFacet> facets = fa.getFacets();
        tf1=System.currentTimeMillis();
        System.out.println(tf1 - tf0 + "\tfacet "+entry.getKey()+" get time\tsize: " + facets.size());
//        System.out.println(Arrays.toString(facets.toArray()));
        fa.close();
      }
      browser.close();
//      System.out.println(t2 - t0 +"\ttotal time\t\t\t hits: "+ bres.getNumHits());
      hitscount += bres.getNumHits();
      long t2 = System.currentTimeMillis();
      tt+= (t2 - t0);
      browser.close();
//      System.out.println(t2 - t0 -(tf1-tf0)+"\tsearch time\t");
    }
    if (hitscount> 80000)System.out.println("avg hits count: " + hitscount/inNumItr);
    try
    {
      Thread.sleep(50);
    } catch (InterruptedException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return tt;
  }
  
  public static void loadFile()
  {
    File file = new File("/Users/xgu/lucene29test/keywords");
    try
    {
      FileInputStream fis = new FileInputStream("/Users/xgu/lucene29test/bbb");
      InputStreamReader isr = new InputStreamReader(fis);
      LineNumberReader reader = new LineNumberReader(isr);
      String line;
      while( (line=reader.readLine())!=null)
      {
        wordlist.add(line);
      }
      words = wordlist.toArray(new String[1]);
    } catch (Exception e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  public static int ii =28000 ;
  public static synchronized int nextInt()
  {
    int ret = ii;
    ii = (ret+1) % words.length;
    return ret;
  }
}
