package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;

public class RandomAccessNotFilter extends RandomAccessFilter {
  protected final RandomAccessFilter _innerFilter;

  public RandomAccessNotFilter(RandomAccessFilter innerFilter) {
    _innerFilter = innerFilter;
  }

  @Override
  public double getFacetSelectivity(BoboSegmentReader reader) {
    double selectivity = _innerFilter.getFacetSelectivity(reader);
    selectivity = selectivity > 0.999 ? 0.0 : (1 - selectivity);
    return selectivity;
  }

  @Override
  public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboSegmentReader reader) throws IOException {
    final RandomAccessDocIdSet innerDocIdSet = _innerFilter.getRandomAccessDocIdSet(reader);
    final DocIdSet notInnerDocIdSet = new NotDocIdSet(innerDocIdSet, reader.maxDoc());
    return new RandomAccessDocIdSet() {
      @Override
      public boolean get(int docId) {
        return !innerDocIdSet.get(docId);
      }

      @Override
      public DocIdSetIterator iterator() throws IOException {
        return notInnerDocIdSet.iterator();
      }
    };
  }

}
