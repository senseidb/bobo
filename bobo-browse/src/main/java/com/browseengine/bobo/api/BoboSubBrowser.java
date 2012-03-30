package com.browseengine.bobo.api;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.Weight;

import com.browseengine.bobo.facets.CombinedFacetAccessible;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandlerInitializerParam;
import com.browseengine.bobo.facets.RuntimeFacetHandler;
import com.browseengine.bobo.facets.RuntimeFacetHandlerFactory;
import com.browseengine.bobo.facets.filter.AndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.search.BoboSearcher2;
import com.browseengine.bobo.search.FacetHitCollector;
import com.browseengine.bobo.sort.SortCollector;

/**
 * This class implements the browsing functionality.
 */
public class BoboSubBrowser extends BoboSearcher2 implements Browsable,Closeable
{
  private static Logger                   logger = Logger.getLogger(BoboSubBrowser.class);
  
  private final BoboIndexReader           _reader;
  private final Map<String, RuntimeFacetHandlerFactory<?,?>> _runtimeFacetHandlerFactoryMap;
  private final HashMap<String, FacetHandler<?>> _runtimeFacetHandlerMap;
  private HashMap<String, FacetHandler<?>> _allFacetHandlerMap;
  private ArrayList<RuntimeFacetHandler<?>> _runtimeFacetHandlers = null;
  public BoboIndexReader getIndexReader()
  {
    return _reader;
  }

  /**
   * Constructor.
   * 
   * @param reader
   *          A bobo reader instance
   */
  public BoboSubBrowser(BoboIndexReader reader)
  {
    super(reader);
    _reader = reader;
    _runtimeFacetHandlerMap = new HashMap<String, FacetHandler<?>>();
    _runtimeFacetHandlerFactoryMap = reader.getRuntimeFacetHandlerFactoryMap();
    _allFacetHandlerMap = null;
  }
  
  private boolean isNoQueryNoFilter(BrowseRequest req)
  {
    Query q = req.getQuery();
    Filter filter = req.getFilter();
    return ((q == null || q instanceof MatchAllDocsQuery) && filter == null && !_reader.hasDeletions()); 
  }

  public Object[] getRawFieldVal(int docid,String fieldname) throws IOException{
	  FacetHandler<?> facetHandler = getFacetHandler(fieldname);
	  if (facetHandler==null){
		  return getFieldVal(docid,fieldname);
	  }
	  else{
		  return facetHandler.getRawFieldValues(_reader,docid);
	  }
  }
  
  /**
   * Sets runtime facet handler. If has the same name as a preload handler, for the
   * duration of this browser, this one will be used.
   * 
   * @param facetHandler
   *          Runtime facet handler
   */
  public void setFacetHandler(FacetHandler<?> facetHandler) throws IOException
  {
	Set<String> dependsOn = facetHandler.getDependsOn();
	BoboIndexReader reader = (BoboIndexReader) getIndexReader();
	if (dependsOn.size() > 0)
	{
		Iterator<String> iter = dependsOn.iterator();
		while(iter.hasNext())
		{
			String fn = iter.next();
			FacetHandler<?> f = _runtimeFacetHandlerMap.get(fn);
			if (f == null)
			{
				f = reader.getFacetHandler(fn);
			}
			if (f==null)
			{
				throw new IOException("depended on facet handler: "+fn+", but is not found");
			}
			facetHandler.putDependedFacetHandler(f);
		}
	}
    facetHandler.loadFacetData(reader);
    _runtimeFacetHandlerMap.put(facetHandler.getName(), facetHandler);
  }

  /**
   * Gets a defined facet handler
   * 
   * @param name
   *          facet name
   * @return a facet handler
   */
  public FacetHandler<?> getFacetHandler(String name)
  {
    return getFacetHandlerMap().get(name);
  }
  
  public Map<String,FacetHandler<?>> getFacetHandlerMap(){
	  if (_allFacetHandlerMap == null){
		_allFacetHandlerMap = new HashMap<String,FacetHandler<?>>(_reader.getFacetHandlerMap());
    }
		_allFacetHandlerMap.putAll(_runtimeFacetHandlerMap);
	  return _allFacetHandlerMap;
  }

  /**
   * Gets a set of facet names
   * 
   * @return set of facet names
   */
  public Set<String> getFacetNames()
  {
	Map<String,FacetHandler<?>> map = getFacetHandlerMap();
	return map.keySet();
  }

  /**
   * browses the index.
   * 
   * @param req
   *          browse request
   * @param collector
   *          collector for the hits
   * @param facetMap map to gather facet data
   */
  public void browse(BrowseRequest req,
                     Collector collector,
                     Map<String, FacetAccessible> facetMap) throws BrowseException
  {
    browse(req, collector, facetMap, 0);
  }
  
  public void browse(BrowseRequest req,
                     Collector collector,
                     Map<String, FacetAccessible> facetMap,
                     int start) throws BrowseException
  {
    Weight w = null;
    try
    {
      Query q = req.getQuery();
      if (q == null)
      {
        q = new MatchAllDocsQuery();
      }
      w = createWeight(q);
    }
    catch (IOException ioe)
    {
      throw new BrowseException(ioe.getMessage(), ioe);
    }
    browse(req, w, collector, facetMap, start);
  }
  
  public void browse(BrowseRequest req,
                     Weight weight,
                     Collector collector,
                     Map<String, FacetAccessible> facetMap,
                     int start) throws BrowseException
  {

    if (_reader == null)
      return;

    
    //      initialize all RuntimeFacetHandlers with data supplied by user at run-time.
    _runtimeFacetHandlers = new ArrayList<RuntimeFacetHandler<?>>(_runtimeFacetHandlerFactoryMap.size());

    Set<String> runtimeFacetNames = _runtimeFacetHandlerFactoryMap.keySet();
    for(String facetName : runtimeFacetNames)
    {
      FacetHandler<?> sfacetHandler = this.getFacetHandler(facetName);
      if (sfacetHandler!=null)
      {
        logger.warn("attempting to reset facetHandler: " + sfacetHandler);
        continue;
      }
      RuntimeFacetHandlerFactory<FacetHandlerInitializerParam,?> factory = (RuntimeFacetHandlerFactory<FacetHandlerInitializerParam, ?>) _runtimeFacetHandlerFactoryMap.get(facetName);
      
        try
        {

          FacetHandlerInitializerParam data = req.getFacethandlerData(facetName);
          if (data == null)
            data = FacetHandlerInitializerParam.EMPTY_PARAM;
          if (data != FacetHandlerInitializerParam.EMPTY_PARAM || !factory.isLoadLazily())
          {
            RuntimeFacetHandler<?> facetHandler =  factory.get(data);
            if (facetHandler != null)
            {
              _runtimeFacetHandlers.add(facetHandler); // add to a list so we close them after search
              this.setFacetHandler(facetHandler);
            }
          }
        }
        catch (IOException e)
        {
          logger.error("error trying to set FacetHandler : " + facetName+":"+e.getMessage(),e);
        }
    }
    // done initialize all RuntimeFacetHandlers with data supplied by user at run-time.

    Set<String> fields = getFacetNames();

    LinkedList<Filter> preFilterList = new LinkedList<Filter>();
    List<FacetHitCollector> facetHitCollectorList = new LinkedList<FacetHitCollector>();
    
    Filter baseFilter = req.getFilter();
    if (baseFilter != null)
    {
      preFilterList.add(baseFilter);
    }

    int selCount = req.getSelectionCount();
    boolean isNoQueryNoFilter = isNoQueryNoFilter(req);

    boolean isDefaultSearch = isNoQueryNoFilter && selCount == 0;
    try
    {
      
      for (String name : fields)
      {
        BrowseSelection sel = req.getSelection(name);
        FacetSpec ospec = req.getFacetSpec(name);

        FacetHandler<?> handler = getFacetHandler(name);
        
        if (handler == null){
        	logger.error("facet handler: "+name+" is not defined, ignored.");
        	continue;
        }
        
        FacetHitCollector facetHitCollector = null;

        RandomAccessFilter filter = null;
        if (sel != null)
        {
          filter = handler.buildFilter(sel);
        }

        if (ospec == null)
        {
          if (filter != null)
          {
            preFilterList.add(filter);
          }
        }
        else
        {
          /*FacetSpec fspec = new FacetSpec(); // OrderValueAsc,
          fspec.setMaxCount(0);
          fspec.setMinHitCount(1);
          
          fspec.setExpandSelection(ospec.isExpandSelection());*/
          FacetSpec fspec = ospec;

          facetHitCollector = new FacetHitCollector();
          facetHitCollector.facetHandler = handler;
          
          if (isDefaultSearch)
          {
        	facetHitCollector._collectAllSource=handler.getFacetCountCollectorSource(sel, fspec);
          }
          else
          {
            facetHitCollector._facetCountCollectorSource = handler.getFacetCountCollectorSource(sel, fspec);            
            if (ospec.isExpandSelection())
            {
              if (isNoQueryNoFilter && sel!=null && selCount == 1)
              {
            	facetHitCollector._collectAllSource=handler.getFacetCountCollectorSource(sel, fspec);
                if (filter != null)
                {
                  preFilterList.add(filter);
                }
              }
              else
              {
                if (filter != null)
                {
                	facetHitCollector._filter = filter;
                }
              }
            }
            else
            {
              if (filter != null)
              {
                preFilterList.add(filter);
              }
            }
          }
        }
        if (facetHitCollector != null)
        {
          facetHitCollectorList.add(facetHitCollector);
        }
      }

      Filter finalFilter = null;
      if (preFilterList.size() > 0)
      {
        if (preFilterList.size() == 1)
        {
          finalFilter = preFilterList.getFirst();
        }
        else
        {
          finalFilter = new AndFilter(preFilterList);
        }
      }

      setFacetHitCollectorList(facetHitCollectorList);

      try
      {
        if (weight == null)
        {
          Query q = req.getQuery();
          if (q==null)
          {
            q = new MatchAllDocsQuery();
          }
          weight = createWeight(q);
        }
        search(weight, finalFilter, collector, start, req.getMapReduceWrapper());
      }
      finally
      {
        for (FacetHitCollector facetCollector : facetHitCollectorList)
        {
          String name = facetCollector.facetHandler.getName();
          LinkedList<FacetCountCollector> resultcollector=null;
          resultcollector = facetCollector._countCollectorList;
          if (resultcollector == null || resultcollector.size() == 0){
        	  resultcollector = facetCollector._collectAllCollectorList;
          }
          if (resultcollector!=null){
        	FacetSpec fspec = req.getFacetSpec(name);
        	assert fspec != null;
            if(resultcollector.size() == 1)
            {
              facetMap.put(name, resultcollector.get(0));             
            }
            else
            {
              ArrayList<FacetAccessible> finalList = new ArrayList<FacetAccessible>(resultcollector.size());
              for (FacetCountCollector fc : resultcollector){
                finalList.add((FacetAccessible)fc);
              }
        	  CombinedFacetAccessible combinedCollector = new CombinedFacetAccessible(fspec, finalList);
              facetMap.put(name, combinedCollector);
        	}
          }
        }
      }
    }
    catch (IOException ioe)
    {
      throw new BrowseException(ioe.getMessage(), ioe);
    }
  }
  
  public SortCollector getSortCollector(SortField[] sort,Query q,int offset,int count,boolean fetchStoredFields,Set<String> termVectorsToFetch,boolean forceScoring,String[] groupBy, int maxPerGroup, boolean collectDocIdCache){
	  return SortCollector.buildSortCollector(this,q,sort, offset, count, forceScoring,fetchStoredFields, termVectorsToFetch,groupBy, maxPerGroup, collectDocIdCache);
  }

  /**
   * browses the index.
   * 
   * @param req
   *          browse request
   * @return browse result
   */
  public BrowseResult browse(BrowseRequest req) throws BrowseException
  {
    if (_reader == null)
      return new BrowseResult();

    final BrowseResult result = new BrowseResult();

    long start = System.currentTimeMillis();

    SortCollector collector = getSortCollector(req.getSort(),req.getQuery(), req.getOffset(), req.getCount(), req.isFetchStoredFields(), req.getTermVectorsToFetch(),false,req.getGroupBy(), req.getMaxPerGroup(), req.getCollectDocIdCache());
    
    Map<String, FacetAccessible> facetCollectors = new HashMap<String, FacetAccessible>();
    browse(req, collector, facetCollectors);
    BrowseHit[] hits = null;

    try
    {
      hits = collector.topDocs();
    }
    catch (IOException e)
    {
      logger.error(e.getMessage(), e);
      hits = new BrowseHit[0];
    }
    
    Query q = req.getQuery();
    if (q == null){
    	q = new MatchAllDocsQuery();
    }
    if (req.isShowExplanation()){
    	for (BrowseHit hit : hits){
    		try {
				Explanation expl = explain(q, hit.getDocid());
				hit.setExplanation(expl);
			} catch (IOException e) {
				logger.error(e.getMessage(),e);
			}
    	}
    }
    result.setHits(hits);
    result.setNumHits(collector.getTotalHits());
    result.setNumGroups(collector.getTotalGroups());
    result.setGroupAccessibles(collector.getGroupAccessibles());
    result.setSortCollector(collector);
    result.setTotalDocs(_reader.numDocs());
    result.addAll(facetCollectors);
    long end = System.currentTimeMillis();
    result.setTime(end - start);
    return result;
  }
  
  public Map<String, FacetHandler<?>> getRuntimeFacetHandlerMap()
  {
    return _runtimeFacetHandlerMap;
  }

  public int numDocs()
  {
    return _reader.numDocs();
  }

  @Override
  public Document doc(int docid) throws CorruptIndexException,
      IOException
  {
    Document doc = super.doc(docid);
    for (FacetHandler<?> handler : _runtimeFacetHandlerMap.values())
    {
      String[] vals = handler.getFieldValues(_reader,docid);
      for (String val : vals)
      {
        doc.add(new Field(handler.getName(),
                          val,
                          Field.Store.NO,
                          Field.Index.NOT_ANALYZED));
      }
    }
    return doc;
  }

  /**
   * Returns the field data for a given doc.
   * 
   * @param docid
   *          doc
   * @param fieldname
   *          name of the field
   * @return field data
   */
  public String[] getFieldVal(int docid, final String fieldname) throws IOException
  {
    FacetHandler<?> facetHandler = getFacetHandler(fieldname);
    if (facetHandler != null)
    {
      return facetHandler.getFieldValues(_reader,docid);
    }
    else
    {
      logger.warn("facet handler: " + fieldname
          + " not defined, looking at stored field.");
      // this is not predefined, so it will be slow
      Document doc = _reader.document(docid, new FieldSelector()
      {
        private static final long serialVersionUID = 1L;

        public FieldSelectorResult accept(String field)
        {
          if (fieldname.equals(field))
          {
            return FieldSelectorResult.LOAD_AND_BREAK;
          }
          else
          {
            return FieldSelectorResult.NO_LOAD;
          }
        }
      });
      return doc.getValues(fieldname);
    }
  }
  
  public void close() throws IOException
  {
    if (_runtimeFacetHandlers!=null)
    {
      for(RuntimeFacetHandler<?> handler : _runtimeFacetHandlers)
      {
        handler.close();
      }
    }
    if(_reader != null)
    {
      _reader.clearRuntimeFacetData();
      _reader.clearRuntimeFacetHandler();
    }
    super.close();
  }
}
