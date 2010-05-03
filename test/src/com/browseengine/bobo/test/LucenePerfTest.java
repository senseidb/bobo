package com.browseengine.bobo.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
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
import com.browseengine.bobo.facets.impl.MultiValueFacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;
import com.browseengine.bobo.search.BoboSearcher2;

public class LucenePerfTest
{
  public static String[] words = { "manager", "university", "in", "business",
      "management", "a", "development", "consultant", "director", "10",
      "services", "on", "senior", "marketing", "project", "sales",
      "technology", "systems", "as", "software", "new", "professional",
      "owner", "experience", "inc", "team", "company" };

  /**
   * @param args
   * @throws IOException
   * @throws CorruptIndexException
   * @throws InterruptedException
   * @throws BrowseException 
   */
  public static void main(String[] args) throws CorruptIndexException,
      IOException, InterruptedException, BrowseException
  {
    int numThread = 40;
    Thread[] threads = new Thread[numThread];
    File file = new File("/Users/xgu/lucene29test/caches/people-search-index");
//    FSDirectory directory = new SimpleFSDirectory(file);
    FSDirectory directory = FSDirectory.getDirectory(file);
    IndexReader reader = IndexReader.open(directory, true);
    final Random rand = new Random(987129);
    final Collection<FacetHandler<?>> facetHandlers = new ArrayList<FacetHandler<?>>();
    facetHandlers.add(new MultiValueFacetHandler("ccid", new PredefinedTermListFactory<Integer>(Integer.class,"0000000000")));
    facetHandlers.add(new SimpleFacetHandler("industry", new PredefinedTermListFactory<Integer>(Integer.class,"0000000000")));
    final BoboIndexReader boboReader = BoboIndexReader.getInstance(reader, facetHandlers , null);
    for(int x =0; x<threads.length; x++)
    {
      threads[x] = new Thread()
      {public void run(){
        try
        {
          oneRun(boboReader, facetHandlers);
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
    for(int x =0; x<threads.length; x++)
    {
      threads[x].join();
    }
    System.out.println("2.9");
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
  private static void oneRun(BoboIndexReader boboReader,
      Collection<FacetHandler<?>> facetHandlers) throws IOException,
      BrowseException
  {
    int numItr = 15;
    long tt = 0;
    for(int x=0; x< numItr; x++)
    {
      long t0 = System.currentTimeMillis();
      BoboBrowser browser = new BoboBrowser(boboReader);
      BrowseRequest req = new BrowseRequest();
      FacetSpec spec = new FacetSpec();
      spec.setMaxCount(50);
      spec.setOrderBy(FacetSortSpec.OrderHitsDesc);
//      req.setFacetSpec("ccid", spec);
//      req.setFacetSpec("industry", spec);
      req.setQuery(new TermQuery(new Term("b","company")));
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
//        System.out.println(tf1 - tf0 + "\tfacet get time\tsize: " + facets.size());
//        System.out.println(Arrays.toString(facets.toArray()));
      }
      long t2 = System.currentTimeMillis();
//      System.out.println(t2 - t0 +"\ttotal time\t\t\t hits: "+ bres.getNumHits());
      tt+= (t2 - t0);
//      System.out.println(t2 - t0 -(tf1-tf0)+"\tsearch time\t");
    }
    System.out.println(tt/numItr);
  }


}
