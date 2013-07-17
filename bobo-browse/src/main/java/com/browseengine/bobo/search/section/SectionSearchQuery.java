/**
 *
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;

/**
 *
 */
public class SectionSearchQuery extends Query {
  private final Query _query;

  private class SectionSearchWeight extends Weight {
    Weight _weight;

    public SectionSearchWeight(IndexSearcher searcher, Query query) throws IOException {
      _weight = searcher.createNormalizedWeight(query);
    }

    @Override
    public String toString() {
      return "weight(" + SectionSearchQuery.this + ")";
    }

    @Override
    public Query getQuery() {
      return SectionSearchQuery.this;
    }

    public float getValue() {
      return getBoost();
    }

    @Override
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
      Explanation result = new Explanation();
      result.setValue(getBoost());
      result.setDescription(SectionSearchQuery.this.toString());
      return result;
    }

    @Override
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, boolean topScorer,
        Bits acceptDocs) throws IOException {
      SectionSearchScorer scorer = new SectionSearchScorer(_weight, getValue(), context.reader());
      return scorer;
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

  public class SectionSearchScorer extends Scorer {
    private int _curDoc = -1;
    private final float _curScr;
    private final SectionSearchQueryPlan _plan;

    public SectionSearchScorer(Weight weight, float score, AtomicReader reader) throws IOException {
      super(weight);
      _curScr = score;

      SectionSearchQueryPlanBuilder builer = new SectionSearchQueryPlanBuilder(reader);
      _plan = builer.getPlan(_query);
      if (_plan != null) {
        _curDoc = -1;
      } else {
        _curDoc = DocIdSetIterator.NO_MORE_DOCS;
        ;
      }
    }

    @Override
    public int docID() {
      return _curDoc;
    }

    @Override
    public int nextDoc() throws IOException {
      return advance(0);
    }

    @Override
    public float score() throws IOException {
      return _curScr;
    }

    @Override
    public int advance(int target) throws IOException {
      if (_curDoc < DocIdSetIterator.NO_MORE_DOCS) {
        if (target <= _curDoc) target = _curDoc + 1;
        return _plan.fetch(target);
      }
      return _curDoc;
    }

    @Override
    public int freq() throws IOException {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public long cost() {
      // TODO Auto-generated method stub
      return 0;
    }
  }

  /**
   * constructs SectionSearchQuery
   *
   * @param query
   */
  public SectionSearchQuery(Query query) {
    _query = query;
  }

  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("SECTION(" + _query.toString() + ")");
    return buffer.toString();
  }

  @Override
  public Weight createWeight(IndexSearcher searcher) throws IOException {
    return new SectionSearchWeight(searcher, _query);
  }

  @Override
  public Query rewrite(IndexReader reader) throws IOException {
    _query.rewrite(reader);
    return this;
  }
}
