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
	private boolean  _takeComplement = false;
	
	public interface FacetDataCacheBuilder{
		FacetDataCache build(BoboIndexReader reader);
		String getName();
	}
	
	// If takeComplement is true, we still return the filter for NotValues . Therefore, the calling function of this class needs to apply NotFilter on top
	// of this filter if takeComplement is true.
	public AdaptiveFacetFilter(FacetDataCacheBuilder facetDataCacheBuilder,RandomAccessFilter facetFilter,String[] val, boolean takeComplement){
	  _facetFilter = facetFilter;
	  _facetDataCacheBuilder = facetDataCacheBuilder;
	  _valSet = new HashSet<String>(Arrays.asList(val));
	  _takeComplement = takeComplement;
	}
	
	 public double getFacetSelectivity(BoboIndexReader reader)
	 {
	   double selectivity = _facetFilter.getFacetSelectivity(reader);
	   if(_takeComplement)
	     return 1.0 - selectivity;
	   return selectivity;
	 }
	
	@Override
	public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboIndexReader reader)
			throws IOException {
	
      RandomAccessDocIdSet innerDocSet = _facetFilter.getRandomAccessDocIdSet(reader);
      if (innerDocSet==EmptyDocIdSet.getInstance()){
    	  return innerDocSet;
      }
		  
	  FacetDataCache dataCache = _facetDataCacheBuilder.build(reader);
	  int totalCount = reader.maxDoc();
	  TermValueList valArray = dataCache.valArray;
	  int freqCount = 0;
	  
	  
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
	  
	  // takeComplement is only used to choose between TermListRandomAccessDocIdSet and innerDocSet
	  int validFreqCount = _takeComplement ? (totalCount - freqCount) : freqCount;
	  
	  if (_facetDataCacheBuilder.getName() != null && ((validFreqCount<<1) < totalCount)) {
	    return new TermListRandomAccessDocIdSet(_facetDataCacheBuilder.getName(), innerDocSet, validVals, reader);
	  }
	  else{
		  return innerDocSet;
	  }
	}

	public static class TermListRandomAccessDocIdSet extends RandomAccessDocIdSet{

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
		
		public static class TermDocIdSet extends DocIdSet{
			final Term term;
      private final IndexReader reader;
			public TermDocIdSet(IndexReader reader, String name, String val){
				this.reader = reader;
        term = new Term(name,val);
			}
			
			@Override
			public DocIdSetIterator iterator() throws IOException {
				final TermDocs td = reader.termDocs(term);
				if (td==null){
					return EmptyDocIdSet.getInstance().iterator();
				}
				return new DocIdSetIterator(){

					private int _doc = -1;
					@Override
					public int advance(int target) throws IOException {
						if (td.skipTo(target)){
							_doc = td.doc();
						}
						else{
							td.close();
							_doc= DocIdSetIterator.NO_MORE_DOCS;
						}
						return _doc;
					}

					@Override
					public int docID() {
						return _doc;
					}

					@Override
					public int nextDoc() throws IOException {
						if (td.next()){
							_doc = td.doc();
						}
						else{
							td.close();
							_doc = DocIdSetIterator.NO_MORE_DOCS;
						}
						return _doc;
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
				return new TermDocIdSet(_reader, _name,_vals.get(0)).iterator();
			}
			else{
				if (_vals.size()<OR_THRESHOLD){
					ArrayList<DocIdSet> docSetList = new ArrayList<DocIdSet>(_vals.size());
					for (String val : _vals){
						docSetList.add(new TermDocIdSet(_reader, _name,val));
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
