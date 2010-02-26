package com.browseengine.bobo.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.MultiSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.sort.MultiSortCollector;
import com.browseengine.bobo.sort.SortCollector;


/**
 * Provides implementation of Browser for multiple Browser instances
 */
public class MultiBoboBrowser extends MultiSearcher implements Browsable
{
  private static Logger logger = Logger.getLogger(MultiBoboBrowser.class);
  
  protected final Browsable[] _subBrowsers;
  /**
   * 
   * @param browsers
   *          Browsers to search on
   * @throws IOException
   */
  public MultiBoboBrowser(Browsable[] browsers) throws IOException
  {
    super(browsers);
    _subBrowsers = browsers;
  }

  /**
   * Implementation of the browse method using a Lucene HitCollector
   * 
   * @param req
   *          BrowseRequest
   * @param hc
   *          Collector for the hits generated during a search
   *          
   */
  public void browse(BrowseRequest req,final Collector hc, Map<String, FacetAccessible> facetMap) throws BrowseException
  {
    browse(req, hc, facetMap, 0);
  }

  public void browse(BrowseRequest req,final Collector hc, Map<String, FacetAccessible> facetMap, int start) throws BrowseException
  {
    Browsable[] browsers = getSubBrowsers();
    int[] starts = getStarts();

    Map<String, List<FacetAccessible>> mergedMap = new HashMap<String,List<FacetAccessible>>();
    try
    {
	    Map<String,FacetAccessible> facetColMap = new HashMap<String,FacetAccessible>();
	    for (int i = 0; i < browsers.length; i++)
	    {
	      try
	      {
		      browsers[i].browse(req, hc, facetColMap, (start + starts[i]));
	      }
	      finally
	      {
	    	  Set<Entry<String,FacetAccessible>> entries = facetColMap.entrySet();
	    	  for (Entry<String,FacetAccessible> entry : entries)
	    	  {
	    		  String name = entry.getKey();
	    		  FacetAccessible facetAccessor = entry.getValue();
	    		  List<FacetAccessible> list = mergedMap.get(name);
	    		  if (list == null)
	    		  {
	    			 list = new ArrayList<FacetAccessible>(browsers.length);
	    			 mergedMap.put(name, list);
	    		  }
	    		  list.add(facetAccessor);
	    	  }
	    	  facetColMap.clear();
	      }
	    }
    }
    finally
    {
      Set<Entry<String,List<FacetAccessible>>> entries = mergedMap.entrySet();
  	  for (Entry<String,List<FacetAccessible>> entry : entries)
  	  {
  		  String name = entry.getKey();
  		  FacetHandler handler = getFacetHandler(name);
  		  try
  		  {
  			  List<FacetAccessible> subList = entry.getValue();
  			  if (subList!=null)
  			  {
  			    FacetAccessible merged = handler.merge(req.getFacetSpec(name), subList);
  	  		  	facetMap.put(name, merged);
  			  }
  		  }
  		  catch(Exception e)
  		  {
  			  logger.error(e.getMessage(),e);
  		  }
  	  }
    }
  }

  /**
   * Generate a merged BrowseResult from the given BrowseRequest
   * @param req
   *          BrowseRequest for generating the facets
   * @return BrowseResult of the results of the BrowseRequest
   */
  public BrowseResult browse(BrowseRequest req) throws BrowseException
  {

    final BrowseResult result = new BrowseResult();

    long start = System.currentTimeMillis();
    int offset = req.getOffset();
    int count = req.getCount();

    if (offset<0 || count<0){
	  throw new IllegalArgumentException("both offset and count must be > 0: "+offset+"/"+count);
    }
    SortCollector collector = getSortCollector(req.getSort(),req.getQuery(), offset, count, req.isFetchStoredFields(),false);
    
    Map<String, FacetAccessible> facetCollectors = new HashMap<String, FacetAccessible>();
    browse(req, collector, facetCollectors);
    BrowseHit[] hits = null;
    try{
      hits = collector.topDocs();
    }
    catch (IOException e){
      logger.error(e.getMessage(), e);
      hits = new BrowseHit[0];
    }
    result.setHits(hits);
    result.setNumHits(collector.getTotalHits());
    result.setTotalDocs(numDocs());
    result.addAll(facetCollectors);
    long end = System.currentTimeMillis();
    result.setTime(end - start);
    return result;
  }
  
  /**
   * Return the values of a field for the given doc
   * 
   */
  public String[] getFieldVal(int docid, final String fieldname) throws IOException
  {
    int i = subSearcher(docid);
    Browsable browser = getSubBrowsers()[i];
    return browser.getFieldVal(subDoc(docid),fieldname);
  }


  public Object[] getRawFieldVal(int docid,String fieldname) throws IOException{
	  int i = subSearcher(docid);
	  Browsable browser = getSubBrowsers()[i];
	  return browser.getRawFieldVal(subDoc(docid),fieldname);
  }
  /**
   * Gets the array of sub-browsers
   * 
   * @return sub-browsers
   * @see MultiSearcher#getSearchables()
   */
  public Browsable[] getSubBrowsers()
  {
    return _subBrowsers;
  }
  
  

  @Override
  public int[] getStarts() {
	// TODO Auto-generated method stub
	return super.getStarts();
  }



  /**
   * Compare BrowseFacets by their value
   */
  public static class BrowseFacetValueComparator implements Comparator<BrowseFacet>
  {
    public int compare(BrowseFacet o1, BrowseFacet o2)
    {
      return o1.getValue().compareTo(o2.getValue());
    }
  }

  /**
   * Gets the sub-browser for a given docid
   * 
   * @param docid
   * @return sub-browser instance
   * @see MultiSearcher#subSearcher(int)
   */
  public Browsable subBrowser(int docid)
  {
    return ((Browsable) (getSubBrowsers()[subSearcher(docid)]));
  }

  @Override
  public void setSimilarity(Similarity similarity)
  {
    super.setSimilarity(similarity);
    for(Browsable subBrowser : getSubBrowsers())
    {
      subBrowser.setSimilarity(similarity);
    }
  }
  
  public int numDocs()
  {
    int count = 0;
    Browsable[] subBrowsers = getSubBrowsers();
    for (Browsable subBrowser : subBrowsers)
    {
      count += subBrowser.numDocs();
    }
    return count;
  }

  public Set<String> getFacetNames()
  {
    Set<String> names = new HashSet<String>();
    Browsable[] subBrowsers = getSubBrowsers();
    for (Browsable subBrowser : subBrowsers)
    {
      names.addAll(subBrowser.getFacetNames());
    }
    return names;
  }
  
  public FacetHandler<?> getFacetHandler(String name)
  {
    Browsable[] subBrowsers = getSubBrowsers();
    for (Browsable subBrowser : subBrowsers)
    {
      FacetHandler<?> subHandler = subBrowser.getFacetHandler(name);
      if (subHandler!=null) return subHandler;
    }
    return null;
  }
	
  
  public void setFacetHandler(FacetHandler<?> facetHandler) throws IOException
  {
	Browsable[] subBrowsers = getSubBrowsers();
	for (Browsable subBrowser : subBrowsers)
	{
	  subBrowser.setFacetHandler(facetHandler);
	}
  }

  public SortCollector getSortCollector(SortField[] sort, Query q,int offset, int count, boolean fetchStoredFields,
		boolean forceScoring) {
	if (_subBrowsers.length==1){
		return _subBrowsers[0].getSortCollector(sort, q, offset, count, fetchStoredFields, forceScoring);
	}
	return new MultiSortCollector(this, q, sort, offset, count, forceScoring,fetchStoredFields);
  }
  
  public void close() throws IOException
  {
    Browsable[] subBrowsers = getSubBrowsers();
    for (Browsable subBrowser : subBrowsers)
    {
      subBrowser.close();
    }
  }
}
