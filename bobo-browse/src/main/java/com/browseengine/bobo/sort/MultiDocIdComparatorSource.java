/**
 *
 */
package com.browseengine.bobo.sort;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;

public class MultiDocIdComparatorSource extends DocComparatorSource {
  private final DocComparatorSource[] _compSources;

  public MultiDocIdComparatorSource(DocComparatorSource[] compSources) {
    _compSources = compSources;
  }

  @Override
  public DocComparator getComparator(AtomicReader reader, int docBase) throws IOException {
    DocComparator[] comparators = new DocComparator[_compSources.length];
    for (int i = 0; i < _compSources.length; ++i) {
      comparators[i] = _compSources[i].getComparator(reader, docBase);
    }
    return new MultiDocIdComparator(comparators);
  }
}
