/**
 *
 */
package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;

public class SpatialFacetFilter extends RandomAccessFilter {

  private final Filter filter;

  public SpatialFacetFilter(Filter filter) {
    this.filter = filter;
  }

  /*
   * (non-Javadoc)
   * @see
   * com.browseengine.bobo.facets.filter.RandomAccessFilter#getRandomAccessDocIdSet(com.browseengine
   * .bobo.api.BoboIndexReader)
   */
  @Override
  public RandomAccessDocIdSet getRandomAccessDocIdSet(BoboSegmentReader reader) throws IOException {
    DocIdSet docIdSet = filter.getDocIdSet(reader.getContext(), reader.getLiveDocs());
    if (docIdSet == null) {
      return null;
    }
    return new SpatialDocIdSet(docIdSet);
  }

  private static final class SpatialDocIdSet extends RandomAccessDocIdSet {
    DocIdSet innerDocIdSet;

    SpatialDocIdSet(DocIdSet docIdSet) {
      innerDocIdSet = docIdSet;
    }

    @Override
    public boolean get(int docid) {
      try {
        return innerDocIdSet.bits().get(docid);
      } catch (IOException e) {
        return false;
      }
    }

    @Override
    public DocIdSetIterator iterator() throws IOException {
      return innerDocIdSet.iterator();
    }
  }
}
