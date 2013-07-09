package com.browseengine.bobo.sort;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;

public class LuceneCustomDocComparatorSource extends DocComparatorSource {
  private final FieldComparator<Comparable<?>> _luceneComparator;

  public LuceneCustomDocComparatorSource(String fieldname,
      FieldComparator<Comparable<?>> luceneComparator) {
    _luceneComparator = luceneComparator;
  }

  @Override
  public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {
    _luceneComparator.setNextReader(reader.getContext());
    return new DocComparator() {

      @Override
      public Comparable<?> value(ScoreDoc doc) {
        return _luceneComparator.value(doc.doc);
      }

      @Override
      public int compare(ScoreDoc doc1, ScoreDoc doc2) {
        return _luceneComparator.compare(doc1.doc, doc2.doc);
      }

      @Override
      public DocComparator setScorer(Scorer scorer) {
        _luceneComparator.setScorer(scorer);
        return this;
      }
    };
  }
}
