package com.browseengine.bobo.search;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;


public final class FacetHitCollector{
	
	public FacetCountCollectorSource _facetCountCollectorSource;	
	public FacetCountCollectorSource _collectAllSource = null;
	public FacetHandler<?> facetHandler;
	public RandomAccessFilter _filter;
	public final CurrentPointers _currentPointers = new CurrentPointers();
	public LinkedList<FacetCountCollector> _countCollectorList = new LinkedList<FacetCountCollector>();
	public LinkedList<FacetCountCollector> _collectAllCollectorList = new LinkedList<FacetCountCollector>();
	
	public void setNextReader(BoboIndexReader reader,int docBase) throws IOException{
		if (_collectAllSource!=null){
			FacetCountCollector collector = _collectAllSource.getFacetCountCollector(reader, docBase);
			_collectAllCollectorList.add(collector);
			collector.collectAll();
		}
		else{
		  if (_filter!=null){
			_currentPointers.docidSet = _filter.getRandomAccessDocIdSet(reader);
			_currentPointers.postDocIDSetIterator = _currentPointers.docidSet.iterator();
			_currentPointers.doc = _currentPointers.postDocIDSetIterator.nextDoc();
		  }
		  if (_facetCountCollectorSource!=null){
		    _currentPointers.facetCountCollector = _facetCountCollectorSource.getFacetCountCollector(reader, docBase);
		    _countCollectorList.add(_currentPointers.facetCountCollector);
		  }
		}
	}
	
	public static class CurrentPointers{
		public RandomAccessDocIdSet docidSet=null;
		public DocIdSetIterator postDocIDSetIterator = null;
		public int doc;
		public FacetCountCollector facetCountCollector;
	}
}
