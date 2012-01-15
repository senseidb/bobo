package com.browseengine.bobo.query.scoring;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.query.MatchAllDocIdSetIterator;

public class FacetTermQuery extends Query {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = Logger.getLogger(FacetTermQuery.class);
	
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

	public String getName(){
	  return _name;
	}
	
	public Map<String,Float> getBoostMap(){
	  return _boostMap;
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
	
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    if( !(obj instanceof FacetTermQuery))
      return false;
    
    FacetTermQuery other = (FacetTermQuery) obj;
    if(! this.toString().equals(other.toString() ) )
        return false;
    if( !_name.equals(other.getName()))
      return false;

    Map<String,Float> _boostMap_1 = this._boostMap;
    Map<String,Float> _boostMap_2 = other.getBoostMap();
    
    if(_boostMap_1.size() != _boostMap_2.size())
      return false;
    Iterator<String> it_map = _boostMap_1.keySet().iterator();
    while(it_map.hasNext()){
      String key_1 = it_map.next();
      if(!_boostMap_2.containsKey(key_1))
        return false;
      else{
        float boost_1 = _boostMap_1.get(key_1);
        float boost_2 = _boostMap_2.get(key_1);
  
        if (Float.floatToIntBits(boost_1) != Float.floatToIntBits(boost_2))
          return false;        
      }
    }
    
    return true;
  }

	private class FacetTermWeight extends Weight{
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Similarity _similarity;
		private float value;
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
					 Explanation exp1 =  scorer.explain(docid);
					 Explanation exp2 = new Explanation(getBoost(), "boost");
					 Explanation expl = new Explanation();
					 expl.setDescription("product of:");
					 expl.setValue(exp1.getValue()*exp2.getValue());
					 expl.addDetail(exp1);
					 expl.addDetail(exp2);
					 return expl;
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
			return value;
		}

		public void normalize(float norm) {
			value = getBoost();
		}
		
		private final DocIdSetIterator buildIterator(final RandomAccessDocIdSet docset,final TermDocs td){
			return new DocIdSetIterator(){
				private int doc = DocIdSetIterator.NO_MORE_DOCS;
				
				@Override
				public int advance(int target) throws IOException {
					if (td.skipTo(target)){
						doc = td.doc();
						while(!docset.get(doc)){
							if (td.next()){
								doc = td.doc();
							}
							else{
								doc = DocIdSetIterator.NO_MORE_DOCS;
								break;
							}
						}
						return doc;
					}
					else{
						doc = DocIdSetIterator.NO_MORE_DOCS;
						return doc;
					}
				}

				@Override
				public int docID() {
					return doc;
				}

				@Override
				public int nextDoc() throws IOException {
					if (td.next()){
						doc = td.doc();
						while(!docset.get(doc)){
							if (td.next()){
								doc = td.doc();
							}
							else{
								doc = DocIdSetIterator.NO_MORE_DOCS;
								break;
							}
						}
						return doc;
					}
					else{
						doc = DocIdSetIterator.NO_MORE_DOCS;
						return doc;
					}
				}
				
			};
		}

		@Override
		public Scorer scorer(IndexReader reader,boolean scoreDocsInOrder,boolean topScorer) throws IOException {
			if (reader instanceof BoboIndexReader){
			  BoboIndexReader boboReader = (BoboIndexReader)reader;
			  TermDocs termDocs = boboReader.termDocs(null);
			  FacetHandler<?> fhandler = boboReader.getFacetHandler(FacetTermQuery.this._name);
			  if (fhandler!=null){
				 DocIdSetIterator dociter = null;
				 RandomAccessFilter filter = fhandler.buildFilter(FacetTermQuery.this._sel);
				 if (filter!=null){
					 RandomAccessDocIdSet docset =filter.getRandomAccessDocIdSet(boboReader);
					 if (docset!=null){
						 dociter = buildIterator(docset, termDocs);
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
			  else{
				  logger.error("FacetHandler is not defined for the field: "+FacetTermQuery.this._name);
			  }
			  return null;
			}
			else{
			  throw new IOException("index reader not instance of "+BoboIndexReader.class);
			}
		}

		public float sumOfSquaredWeights() throws IOException {
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
			return _scorer==null ? 1.0f : _scorer.score(_docSetIter.docID())*getBoost();
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
