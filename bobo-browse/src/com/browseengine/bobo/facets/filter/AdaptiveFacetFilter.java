package com.browseengine.bobo.facets.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.docidset.EmptyDocIdSet;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermValueList;
import com.kamikaze.docidset.impl.OrDocIdSet;

public class AdaptiveFacetFilter extends RandomAccessFilter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = Logger.getLogger(AdaptiveFacetFilter.class);
	
	private final RandomAccessFilter _facetFilter;
	private final FacetDataCacheBuilder _facetDataCacheBuilder;
	private final Set<String> _valSet;
	
	public interface FacetDataCacheBuilder{
		FacetDataCache build(BoboIndexReader reader);
		String getName();
	}
	
	public AdaptiveFacetFilter(FacetDataCacheBuilder facetDataCacheBuilder,RandomAccessFilter facetFilter,String[] val){
	  _facetFilter = facetFilter;
	  _facetDataCacheBuilder = facetDataCacheBuilder;
	  _valSet = new HashSet<String>(Arrays.asList(val));
	}
	
	@Override
	public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboIndexReader reader)
			throws IOException {
	  FacetDataCache dataCache = _facetDataCacheBuilder.build(reader);
	  int totalCount = reader.maxDoc();
	  TermValueList valArray = dataCache.valArray;
	  int freqCount = 0;
	  
	  RandomAccessDocIdSet innerDocSet = _facetFilter.getRandomAccessDocIdSet(reader);
	  
	  ArrayList<String> validVals = new ArrayList<String>(_valSet.size());
	  for (String val : _valSet){
		  int idx = valArray.indexOf(val);
		  if (idx>=0){
			  validVals.add(valArray.get(idx));		// get and format the value
			  freqCount+=dataCache.freqs[idx];
		  }
	  }
	  
	  if (validVals.size()==0){
		  return EmptyDocIdSet.getInstance();
	  }
	  
	  if (freqCount<<1 < totalCount){
		  return new TermListRandomAccessDocIdSet(_facetDataCacheBuilder.getName(), innerDocSet, validVals, reader);
	  }
	  else{
		  return innerDocSet;
	  }
	}

	private static class TermListRandomAccessDocIdSet extends RandomAccessDocIdSet{

		private final RandomAccessDocIdSet _innerSet;
		private final ArrayList<String> _vals;
		private final IndexReader _reader;
		private final String _name;
		private final static int OR_THRESHOLD = 5;
		
		TermListRandomAccessDocIdSet(String name,RandomAccessDocIdSet innerSet,ArrayList<String> vals,IndexReader reader){
			_name = name;
			_innerSet = innerSet;
			_vals = vals;
			_reader = reader;
		}
		
		private class TermDocIdSet extends DocIdSet{
			final Term term;
			TermDocIdSet(String name,String val){
				term = new Term(name,val);
			}
			
			@Override
			public DocIdSetIterator iterator() throws IOException {
				final TermDocs td = _reader.termDocs(term);
				if (td==null){
					return EmptyDocIdSet.getInstance().iterator();
				}
				return new DocIdSetIterator(){

					@Override
					public int advance(int target) throws IOException {
						if (td.skipTo(target)){
							return td.doc();
						}
						else{
							td.close();
							return DocIdSetIterator.NO_MORE_DOCS;
						}
					}

					@Override
					public int docID() {
						return td.doc();
					}

					@Override
					public int nextDoc() throws IOException {
						if (td.next()){
							return td.doc();
						}
						else{
							td.close();
							return DocIdSetIterator.NO_MORE_DOCS;
						}
					}
					
				};
			}
		}

		@Override
		public boolean get(int docId) {
			return _innerSet.get(docId);
		}

		@Override
		public DocIdSetIterator iterator() throws IOException {
			if (_vals.size()==0){
				return EmptyDocIdSet.getInstance().iterator();
			}
			if (_vals.size()==1){
				return new TermDocIdSet(_name,_vals.get(0)).iterator();
			}
			else{
				if (_vals.size()<OR_THRESHOLD){
					ArrayList<DocIdSet> docSetList = new ArrayList<DocIdSet>(_vals.size());
					for (String val : _vals){
						docSetList.add(new TermDocIdSet(_name,val));
					}
					return new OrDocIdSet(docSetList).iterator();
				}
				else{
					return _innerSet.iterator();
				}
			}
		}
	}
}
