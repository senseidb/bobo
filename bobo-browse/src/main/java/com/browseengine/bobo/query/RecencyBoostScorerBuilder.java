package com.browseengine.bobo.query;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermLongList;
import com.browseengine.bobo.util.BigSegmentedArray;

public class RecencyBoostScorerBuilder implements ScorerBuilder {

  private final float _maxFactor;
  private final TimeUnit _timeunit;
  private final float _min;
  private final float _max;
  private final long _cutoffInMillis;
  private final float _A;
  private final String _timeFacetName;
  private final long _now;

  public RecencyBoostScorerBuilder(String timeFacetName, float maxFactor, long cutoff,
      TimeUnit timeunit) {
    this(timeFacetName, maxFactor, timeunit.convert(System.currentTimeMillis(),
      TimeUnit.MILLISECONDS), cutoff, timeunit);
  }

  public RecencyBoostScorerBuilder(String timeFacetName, float maxFactor, long from, long cutoff,
      TimeUnit timeunit) {
    _timeFacetName = timeFacetName;
    _maxFactor = maxFactor;
    _min = 1.0f;
    _max = _maxFactor + _min;
    _timeunit = timeunit;
    _cutoffInMillis = _timeunit.toMillis(cutoff);
    _A = (_min - _max) / (((float) _cutoffInMillis) * ((float) _cutoffInMillis));
    _now = timeunit.toMillis(from);
  }

  @Override
  public Explanation explain(AtomicReader reader, int doc, Explanation innerExplaination)
      throws IOException {
    if (reader instanceof BoboSegmentReader) {
      BoboSegmentReader boboReader = (BoboSegmentReader) reader;
      Object dataObj = boboReader.getFacetData(_timeFacetName);
      if (dataObj instanceof FacetDataCache<?>) {
        @SuppressWarnings("unchecked")
        FacetDataCache<Long> facetDataCache = (FacetDataCache<Long>) (boboReader
            .getFacetData(_timeFacetName));
        final BigSegmentedArray orderArray = facetDataCache.orderArray;
        final TermLongList termList = (TermLongList) facetDataCache.valArray;
        Explanation finalExpl = new Explanation();
        finalExpl.addDetail(innerExplaination);
        float rawScore = innerExplaination.getValue();
        long timeVal = termList.getPrimitiveValue(orderArray.get(doc));
        float timeScore = computeTimeFactor(timeVal);
        float finalScore = combineScores(timeScore, rawScore);
        finalExpl.setValue(finalScore);
        finalExpl.setDescription("final score = (time score: " + timeScore + ") * (raw score: "
            + rawScore + "), timeVal: " + timeVal);
        return finalExpl;
      } else {
        throw new IllegalStateException(
            "underlying facet data must be of type FacetDataCache<Long>");
      }
    } else {
      throw new IllegalStateException("reader not instance of " + BoboSegmentReader.class);
    }
  }

  @Override
  public Scorer createScorer(final Scorer innerScorer, AtomicReader reader,
      boolean scoreDocsInOrder, boolean topScorer) throws IOException {
    if (reader instanceof BoboSegmentReader) {
      BoboSegmentReader boboReader = (BoboSegmentReader) reader;
      Object dataObj = boboReader.getFacetData(_timeFacetName);
      if (dataObj instanceof FacetDataCache<?>) {
        @SuppressWarnings("unchecked")
        FacetDataCache<Long> facetDataCache = (FacetDataCache<Long>) (boboReader
            .getFacetData(_timeFacetName));
        final BigSegmentedArray orderArray = facetDataCache.orderArray;
        final TermLongList termList = (TermLongList) facetDataCache.valArray;
        return new Scorer(innerScorer.getWeight()) {

          @Override
          public float score() throws IOException {
            float rawScore = innerScorer.score();
            long timeVal = termList.getRawValue(orderArray.get(innerScorer.docID()));
            float timeScore = computeTimeFactor(timeVal);
            return combineScores(timeScore, rawScore);
          }

          @Override
          public int advance(int target) throws IOException {
            return innerScorer.advance(target);
          }

          @Override
          public int docID() {
            return innerScorer.docID();
          }

          @Override
          public int nextDoc() throws IOException {
            return innerScorer.nextDoc();
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

        };
      } else {
        throw new IllegalStateException(
            "underlying facet data must be of type FacetDataCache<Long>");
      }
    } else {
      throw new IllegalStateException("reader not instance of " + BoboSegmentReader.class);
    }
  }

  protected float computeTimeFactor(long timeVal) {
    long xVal = _now - timeVal;
    if (xVal > _cutoffInMillis) {
      return _min;
    } else {
      float xValFloat = xVal;
      return _A * xValFloat * xValFloat + _max;
    }
  }

  private static float combineScores(float timeScore, float rawScore) {
    return timeScore * rawScore;
  }
}
