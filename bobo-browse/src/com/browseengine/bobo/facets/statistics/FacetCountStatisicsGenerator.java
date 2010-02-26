package com.browseengine.bobo.facets.statistics;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.FacetCountCollector;

public abstract class FacetCountStatisicsGenerator
{
  private int _minCount = 1;
  
  public int getMinCount()
  {
    return _minCount;
  }

  public void setMinCount(int minCount)
  {
    _minCount = minCount;
  }
  
  public abstract double calculateDistributionScore(int[] distribution,int collectedSampleCount,int numSamplesCollected,int totalSamplesCount);

  public FacetCountStatistics generateStatistic(int[] distribution,int n)
  {
    int[] tmp=distribution;
    int totalSampleCount=distribution.length;
    boolean sorted=false;
    if (n>0)
    {
      totalSampleCount = Math.min(n, tmp.length);
      // this is crappy, to be made better with a pq
      int[] tmp2 = new int[distribution.length];
      System.arraycopy(distribution, 0, tmp2, 0, distribution.length);
      
      Arrays.sort(tmp2);
      
      tmp = new int[totalSampleCount];
      System.arraycopy(tmp2, 0, tmp, 0, tmp.length);
      sorted = true;
    }
    
    int collectedSampleCount = 0;
    int numSamplesCollected = 0;
    
    for (int count : tmp)
    {
      if (count >= _minCount)
      {
        collectedSampleCount+=count; 
        numSamplesCollected++;
      }
      else
      {
        if (sorted) break;
      }
    }
    
    double distScore = calculateDistributionScore(tmp, collectedSampleCount, numSamplesCollected,totalSampleCount);
    
    FacetCountStatistics stats = new FacetCountStatistics();
    
    stats.setDistribution(distScore);
    stats.setNumSamplesCollected(numSamplesCollected);
    stats.setCollectedSampleCount(collectedSampleCount);
    stats.setTotalSampleCount(totalSampleCount);
    return stats;
  }
  
  public FacetCountStatistics generateStatistic(FacetCountCollector countHitCollector,int n)
  {
    return generateStatistic(countHitCollector.getCountDistribution(),n);
  } 
  
  public static void main(String[] args) throws Exception
  {
    Directory idxDir = FSDirectory.open(new File("/Users/jwang/dataset/facet_idx_2/beef"));
    QueryParser qp = new QueryParser(Version.LUCENE_CURRENT,"b",new StandardAnalyzer(Version.LUCENE_CURRENT));
    String q = "pc:yahoo";
    Query query = qp.parse(q);
    
    
    BrowseRequest req = new BrowseRequest();
    req.setQuery(query);
    
    FacetSpec fspec = new FacetSpec();
    fspec.setExpandSelection(true);
    fspec.setMaxCount(5);
    fspec.setOrderBy(FacetSortSpec.OrderHitsDesc);
    
    req.setFacetSpec("ccid", fspec);
    req.setFacetSpec("pcid", fspec);
    req.setFacetSpec("education_id", fspec);
    req.setFacetSpec("geo_region", fspec);
    req.setFacetSpec("geo_country", fspec);
    req.setFacetSpec("industry", fspec);
    req.setFacetSpec("proposal_accepts", fspec);
    req.setFacetSpec("num_endorsers", fspec);
    req.setFacetSpec("group_id", fspec);
    
    BoboIndexReader reader = BoboIndexReader.getInstance(IndexReader.open(idxDir));
    BoboBrowser browser = new BoboBrowser(reader);
    
    BrowseResult res = browser.browse(req);
    
    Map<String,FacetAccessible> facetMap = res.getFacetMap();
    Collection<FacetAccessible> facetCountCollectors = facetMap.values();
    Iterator<FacetAccessible> iter = facetCountCollectors.iterator();
    while (iter.hasNext())
    {
      FacetAccessible f = iter.next();
      if (f instanceof FacetCountCollector)
      {
        System.out.println("====================================");
        FacetCountCollector fc = (FacetCountCollector)f;
        int[] dist = fc.getCountDistribution();
        if (dist!=null)
        {
          ChiSquaredFacetCountStatisticsGenerator gen = new ChiSquaredFacetCountStatisticsGenerator();
          gen.setMinCount(0);
          FacetCountStatistics stats = gen.generateStatistic(dist, 0);
          System.out.println("stat for field "+fc.getName()+": "+stats);
          System.out.println("Centered distribution score: " + (stats.getDistribution()-(double)(stats.getNumSamplesCollected()-1))/Math.sqrt((2.0*(double)(stats.getNumSamplesCollected()-1))));
          System.out.println("........................");
          List<BrowseFacet> facetList = fc.getFacets();
          System.out.println(facetList);
          System.out.println("........................");
        }
        System.out.println("====================================");
      }
    }
    reader.close();
  }
}
