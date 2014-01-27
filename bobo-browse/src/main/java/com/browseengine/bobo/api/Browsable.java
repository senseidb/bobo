package com.browseengine.bobo.api;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.similarities.Similarity;

import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.sort.SortCollector;

public interface Browsable {

  void browse(BrowseRequest req, Collector hitCollector, Map<String, FacetAccessible> facets,
      int start) throws BrowseException;

  IndexReader getIndexReader();

  BrowseResult browse(BrowseRequest req) throws BrowseException;

  Set<String> getFacetNames();

  void setFacetHandler(FacetHandler<?> facetHandler) throws IOException;

  FacetHandler<?> getFacetHandler(String name);

  Map<String, FacetHandler<?>> getFacetHandlerMap();

  void setSimilarity(Similarity similarity);

  String[] getFieldVal(int docid, String fieldname) throws IOException;

  Object[] getRawFieldVal(int docid, String fieldname) throws IOException;

  int numDocs();

  SortCollector getSortCollector(SortField[] sort, Query q, int offset, int count,
      boolean fetchStoredFields, Set<String> termVectorsToFetch, String[] groupBy, int maxPerGroup,
      boolean collectDocIdCache);

  void doClose() throws IOException;

  Explanation explain(Query q, int deBasedDoc) throws IOException;

  void close() throws IOException;
}
