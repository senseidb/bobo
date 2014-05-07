package com.browseengine.bobo.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.similarities.Similarity;

import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.sort.SortCollector;

/**
 * Provides implementation of Browser for multiple Browser instances
 */
public class MultiBoboBrowser extends MultiReader implements Browsable {
  private static Logger logger = Logger.getLogger(MultiBoboBrowser.class);

  private IndexSearcher _indexSearcher = null;
  protected Browsable[] _subBrowsers;

  public MultiBoboBrowser(BoboMultiReader reader) throws IOException {
    this(reader._subReaders);
  }

  public MultiBoboBrowser(List<BoboSegmentReader> segmentReaders) throws IOException {
    super(segmentReaders.toArray(new BoboSegmentReader[0]), false);
    _indexSearcher = new IndexSearcher(this);
    initSubBrowsers();
  }

  /**
   *
   * @param browsers
   *          Browsers to search on
   * @throws IOException
   */
  public MultiBoboBrowser(Browsable[] browsers) throws IOException {
    super(getSubReaders(browsers), false);
    _indexSearcher = new IndexSearcher(this);
    initSubBrowsers();
  }

  public void initSubBrowsers() {
    List<AtomicReaderContext> leaves = getContext().leaves();
    _subBrowsers = new BoboSubBrowser[leaves.size()];
    for (int i = 0; i < leaves.size(); ++i) {
      _subBrowsers[i] = new BoboSubBrowser(leaves.get(i));
    }
  }

  private static IndexReader[] getSubReaders(Browsable[] browsers) {
    IndexReader[] readers = new IndexReader[browsers.length];
    for (int i = 0; i < browsers.length; ++i) {
      readers[i] = browsers[i].getIndexReader();
    }
    return readers;
  }

  public void browse(BrowseRequest req, final Collector hc, Map<String, FacetAccessible> facetMap)
      throws BrowseException {
    Weight w = null;
    try {
      Query q = req.getQuery();
      MatchAllDocsQuery matchAllDocsQuery = new MatchAllDocsQuery();
      if (q == null) {
        q = matchAllDocsQuery;
      } else if (!(q instanceof MatchAllDocsQuery)) {
        // MatchAllQuery is needed to filter out the deleted docids, that reside in
        // ZoieSegmentReader and are not visible on Bobo level
        matchAllDocsQuery.setBoost(0f);
        q = QueriesSupport.combineAnd(matchAllDocsQuery, q);
      }
      req.setQuery(q);
      w = _indexSearcher.createNormalizedWeight(q);
    } catch (Exception ioe) {
      throw new BrowseException(ioe.getMessage(), ioe);
    }
    browse(req, w, hc, facetMap, 0);
  }

  @Override
  public void browse(BrowseRequest req, Weight w, final Collector hc,
      Map<String, FacetAccessible> facetMap, int start) throws BrowseException {
    // index empty
    if (_subBrowsers == null || _subBrowsers.length == 0) {
      return;
    }

    Map<String, List<FacetAccessible>> mergedMap = new HashMap<String, List<FacetAccessible>>();
    try {
      Map<String, FacetAccessible> facetColMap = new HashMap<String, FacetAccessible>();
      for (int i = 0; i < _subBrowsers.length; i++) {
        try {
          _subBrowsers[i].browse(req, w, hc, facetColMap, (start + readerBase(i)));
        } finally {
          Set<Entry<String, FacetAccessible>> entries = facetColMap.entrySet();
          for (Entry<String, FacetAccessible> entry : entries) {
            String name = entry.getKey();
            FacetAccessible facetAccessor = entry.getValue();
            List<FacetAccessible> list = mergedMap.get(name);
            if (list == null) {
              list = new ArrayList<FacetAccessible>(_subBrowsers.length);
              mergedMap.put(name, list);
            }
            list.add(facetAccessor);
          }
          facetColMap.clear();
        }
      }
    } finally {
      if (req.getMapReduceWrapper() != null) {
        req.getMapReduceWrapper().finalizePartition();
      }
      Set<Entry<String, List<FacetAccessible>>> entries = mergedMap.entrySet();
      for (Entry<String, List<FacetAccessible>> entry : entries) {
        String name = entry.getKey();
        FacetHandler<?> handler = getFacetHandler(name);
        try {
          List<FacetAccessible> subList = entry.getValue();
          if (subList != null) {
            FacetAccessible merged = handler.merge(req.getFacetSpec(name), subList);
            facetMap.put(name, merged);
          }
        } catch (Exception e) {
          logger.error(e.getMessage(), e);
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
  @Override
  public BrowseResult browse(BrowseRequest req) throws BrowseException {

    final BrowseResult result = new BrowseResult();

    // index empty
    if (_subBrowsers == null || _subBrowsers.length == 0) {
      return result;
    }
    long start = System.currentTimeMillis();
    int offset = req.getOffset();
    int count = req.getCount();

    if (offset < 0 || count < 0) {
      throw new IllegalArgumentException("both offset and count must be > 0: " + offset + "/"
          + count);
    }
    SortCollector collector = getSortCollector(req.getSort(), req.getQuery(), offset, count,
      req.isFetchStoredFields(), req.getTermVectorsToFetch(), req.getGroupBy(),
      req.getMaxPerGroup(), req.getCollectDocIdCache());

    Map<String, FacetAccessible> facetCollectors = new HashMap<String, FacetAccessible>();
    browse(req, collector, facetCollectors);
    if (req.getMapReduceWrapper() != null) {
      result.setMapReduceResult(req.getMapReduceWrapper().getResult());
    }
    BrowseHit[] hits = null;
    try {
      hits = collector.topDocs();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      result.addError(e.getMessage());
      hits = new BrowseHit[0];
    }

    Query q = req.getQuery();
    if (req.isShowExplanation()) {
      for (BrowseHit hit : hits) {
        try {
          int doc = hit.getDocid();
          Explanation expl = _indexSearcher.explain(q, doc);
          hit.setExplanation(expl);
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
          result.addError(e.getMessage());
        }
      }
    }

    result.setHits(hits);
    result.setNumHits(collector.getTotalHits());
    result.setNumGroups(collector.getTotalGroups());
    result.setGroupAccessibles(collector.getGroupAccessibles());
    result.setSortCollector(collector);
    result.setTotalDocs(numDocs());
    result.addAll(facetCollectors);
    long end = System.currentTimeMillis();
    result.setTime(end - start);
    // set the transaction ID to trace transactions
    result.setTid(req.getTid());
    return result;
  }

  /**
   * Return the values of a field for the given doc
   *
   */
  @Override
  public String[] getFieldVal(int docid, final String fieldname) throws IOException {
    int i = readerIndex(docid);
    Browsable browser = _subBrowsers[i];
    return browser.getFieldVal(docid - readerBase(i), fieldname);
  }

  @Override
  public Object[] getRawFieldVal(int docid, String fieldname) throws IOException {
    int i = readerIndex(docid);
    Browsable browser = _subBrowsers[i];
    return browser.getRawFieldVal(docid - readerBase(i), fieldname);
  }

  /**
   * Compare BrowseFacets by their value
   */
  public static class BrowseFacetValueComparator implements Comparator<BrowseFacet> {
    @Override
    public int compare(BrowseFacet o1, BrowseFacet o2) {
      return o1.getValue().compareTo(o2.getValue());
    }
  }

  /**
   * Gets the sub-browser for a given docid
   *
   * @param docid
   * @return sub-browser instance
   */
  public Browsable subBrowser(int docid) {
    int i = readerIndex(docid);
    return _subBrowsers[i];
  }

  @Override
  public Set<String> getFacetNames() {
    Set<String> names = new HashSet<String>();
    for (Browsable subBrowser : _subBrowsers) {
      names.addAll(subBrowser.getFacetNames());
    }
    return names;
  }

  @Override
  public FacetHandler<?> getFacetHandler(String name) {
    for (Browsable subBrowser : _subBrowsers) {
      FacetHandler<?> subHandler = subBrowser.getFacetHandler(name);
      if (subHandler != null) {
        return subHandler;
      }
    }
    return null;
  }

  @Override
  public Map<String, FacetHandler<?>> getFacetHandlerMap() {
    HashMap<String, FacetHandler<?>> map = new HashMap<String, FacetHandler<?>>();
    for (Browsable subBrowser : _subBrowsers) {
      map.putAll(subBrowser.getFacetHandlerMap());
    }
    return map;
  }

  @Override
  public void setFacetHandler(FacetHandler<?> facetHandler) throws IOException {
    for (Browsable subBrowser : _subBrowsers) {
      subBrowser.setFacetHandler(facetHandler);
    }
  }

  @Override
  public SortCollector getSortCollector(SortField[] sort, Query q, int offset, int count,
      boolean fetchStoredFields, Set<String> termVectorsToFetch, String[] groupBy, int maxPerGroup,
      boolean collectDocIdCache) {
    if (_subBrowsers.length == 1) {
      return _subBrowsers[0].getSortCollector(sort, q, offset, count, fetchStoredFields,
        termVectorsToFetch, groupBy, maxPerGroup, collectDocIdCache);
    }
    return SortCollector.buildSortCollector(this, q, sort, offset, count, fetchStoredFields,
      termVectorsToFetch, groupBy, maxPerGroup, collectDocIdCache);
  }

  @Override
  public void doClose() throws IOException {
    super.doClose();
    for (Browsable subBrowser : _subBrowsers) {
      subBrowser.doClose();
    }
  }

  @Override
  public IndexReader getIndexReader() {
    return this;
  }

  public void setSimilarity(Similarity similarity) {
    _indexSearcher.setSimilarity(similarity);
  }
}
