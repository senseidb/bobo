package com.browseengine.bobo.query.scoring;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.query.MatchAllDocIdSetIterator;

public class FacetTermQuery extends Query {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final String _name;
	private final BrowseSelection _sel;
	private final FacetTermScoringFunctionFactory _scoringFactory;
	private final Map<String,Float> _boostMap;
	
	public FacetTermQuery(BrowseSelection sel,Map<String,Float> boostMap){
		this(sel,boostMap,new DefaultFacetTermScoringFunctionFactory());
	}
	
	public FacetTermQuery(BrowseSelection sel,Map<String,Float> boostMap,FacetTermScoringFunctionFactory scoringFactory){
		_name = sel.getFieldName();
		_sel = sel;
		_scoringFactory = scoringFactory;
		_boostMap = boostMap;
	}

	@Override
	public String toString(String fieldname) {
		return String.valueOf(_sel);
	}
	
	@Override
	public Weight createWeight(Searcher searcher) throws IOException {
		return new FacetTermWeight(searcher.getSimilarity());
	}
	
	@Override
	public void extractTerms(Set terms) {
		String[] vals =_sel.getValues();
		for (String val : vals){
			terms.add(new Term(_name,val));
		}
	}

	private class FacetTermWeight extends Weight{
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Similarity _similarity;
        public FacetTermWeight(Similarity sim) {
        	_similarity = sim;
		}
        
		public Explanation explain(IndexReader reader, int docid)
				throws IOException {
			BoboIndexReader boboReader = (BoboIndexReader)reader;
			FacetHandler<?> fhandler = boboReader.getFacetHandler(FacetTermQuery.this._name);
			if (fhandler!=null){
				 BoboDocScorer scorer = null;
				 if (fhandler instanceof FacetScoreable){
					 scorer = ((FacetScoreable)fhandler).getDocScorer(boboReader,_scoringFactory, _boostMap);
					 return scorer.explain(docid);
				 }
				 else{
					 return null;
				 }
			}
			return null;
		}

		public Query getQuery() {
			return FacetTermQuery.this;
		}

		public float getValue() {
			return 0;
		}

		public void normalize(float score) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Scorer scorer(IndexReader reader,boolean scoreDocsInOrder,boolean topScorer) throws IOException {
			if (reader instanceof BoboIndexReader){
			  BoboIndexReader boboReader = (BoboIndexReader)reader;
			  FacetHandler<?> fhandler = boboReader.getFacetHandler(FacetTermQuery.this._name);
			  if (fhandler!=null){
				 DocIdSetIterator dociter = null;
				 RandomAccessFilter filter = fhandler.buildFilter(FacetTermQuery.this._sel);
				 if (filter!=null){
					 DocIdSet docset =filter.getDocIdSet(boboReader);
					 if (docset!=null){
						 dociter = docset.iterator();
					 }
				 }
				 if (dociter==null){
					 dociter = new MatchAllDocIdSetIterator(reader);
				 }
				 BoboDocScorer scorer = null;
				 if (fhandler instanceof FacetScoreable){
					 scorer = ((FacetScoreable)fhandler).getDocScorer(boboReader,_scoringFactory, _boostMap);
				 }
				 return new FacetTermScorer(_similarity,dociter,scorer);
			  }
			  return null;
			}
			else{
			  throw new IOException("index reader not instance of "+BoboIndexReader.class);
			}
		}

		public float sumOfSquaredWeights() throws IOException {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
	private class FacetTermScorer extends Scorer{
		private final DocIdSetIterator _docSetIter;
		private final BoboDocScorer _scorer;
		
		protected FacetTermScorer(Similarity similarity,DocIdSetIterator docidsetIter,BoboDocScorer scorer) {
			super(similarity);
			_docSetIter = docidsetIter;
			_scorer = scorer;
		}

		@Override
		public float score() throws IOException {
			return _scorer==null ? 1.0f : _scorer.score(_docSetIter.docID());
		}

		@Override
		public int docID() {
			return _docSetIter.docID();
		}

		@Override
		public int nextDoc() throws IOException {
			return _docSetIter.nextDoc();
		}

		@Override
		public int advance(int target) throws IOException {
			return _docSetIter.advance(target);
		}
		
	}

}
