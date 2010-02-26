package com.browseengine.bobo.api;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.sort.SortCollector;

public interface Browsable extends Searchable
{
	
	void browse(BrowseRequest req, 
	            Collector hitCollector,
	            Map<String,FacetAccessible> facets) throws BrowseException;

	void browse(BrowseRequest req, 
	            Collector hitCollector,
	            Map<String,FacetAccessible> facets,
	            int start) throws BrowseException;

	BrowseResult browse(BrowseRequest req) throws BrowseException;

	Set<String> getFacetNames();
	
	void setFacetHandler(FacetHandler<?> facetHandler) throws IOException;

	FacetHandler<?> getFacetHandler(String name);
	
	Similarity getSimilarity();
	
	void setSimilarity(Similarity similarity);
	
	String[] getFieldVal(int docid,String fieldname) throws IOException;
	
	Object[] getRawFieldVal(int docid,String fieldname) throws IOException;
	
	int numDocs();
	
	SortCollector getSortCollector(SortField[] sort,Query q,int offset,int count,boolean fetchStoredFields,boolean forceScoring);
	
	Explanation explain(Query q, int docid) throws IOException;
}
