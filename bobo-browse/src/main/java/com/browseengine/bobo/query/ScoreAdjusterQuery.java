package com.browseengine.bobo.query;

import java.io.IOException;
import java.util.Set;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;

public class ScoreAdjusterQuery extends Query {
  private class ScoreAdjusterWeight extends Weight {
    Weight _innerWeight;

    public ScoreAdjusterWeight(Weight innerWeight) throws IOException {
      _innerWeight = innerWeight;
    }

    @Override
    public String toString() {
      return "weight(" + ScoreAdjusterQuery.this + ")";
    }

    @Override
    public Query getQuery() {
      return _innerWeight.getQuery();
    }

    @Override
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer,
        Bits acceptDocs) throws IOException {
      Scorer innerScorer = _innerWeight.scorer(context, scoreDocsInOrder, topScorer, acceptDocs);
      return _scorerBuilder
          .createScorer(innerScorer, context.reader(), scoreDocsInOrder, topScorer);
    }

    @Override
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
      Explanation innerExplain = _innerWeight.explain(context, doc);
      return _scorerBuilder.explain(context.reader(), doc, innerExplain);
    }

    @Override
    public float getValueForNormalization() throws IOException {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public void normalize(float norm, float topLevelBoost) {
      // TODO Auto-generated method stub
    }
  }

  protected final Query _query;
  protected final ScorerBuilder _scorerBuilder;

  public ScoreAdjusterQuery(Query query, ScorerBuilder scorerBuilder) {
    _query = query;
    _scorerBuilder = scorerBuilder;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public void extractTerms(Set terms) {
    _query.extractTerms(terms);
  }

  @Override
  public Weight createWeight(IndexSearcher searcher) throws IOException {
    return new ScoreAdjusterWeight(_query.createWeight(searcher));
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    _query.rewrite(reader);
    return this;
  }

  @Override
  public String toString(String field) {
    return _query.toString(field);
  }
}
