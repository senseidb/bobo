package com.browseengine.bobo.facets.filter;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.docidset.RandomAccessDocIdSet;

public abstract class RandomAccessFilter extends Filter {
  @Override
  public DocIdSet getDocIdSet(AtomicReaderContext context, Bits acceptDocs) throws IOException {
    AtomicReader reader = context.reader();
    if (reader instanceof BoboSegmentReader) {
      return getRandomAccessDocIdSet((BoboSegmentReader) reader);
    } else {
      throw new IllegalStateException("reader not instance of " + BoboSegmentReader.class);
    }
  }

  public abstract RandomAccessDocIdSet getRandomAccessDocIdSet(BoboSegmentReader reader)
      throws IOException;

  public double getFacetSelectivity(BoboSegmentReader reader) {
    return 0.50;
  }

}
