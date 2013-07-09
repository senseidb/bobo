package com.browseengine.bobo.query;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;

public interface ScorerBuilder {
  Scorer createScorer(Scorer innerScorer, AtomicReader reader, boolean scoreDocsInOrder,
      boolean topScorer) throws IOException;

  Explanation explain(AtomicReader reader, int doc, Explanation innerExplaination)
      throws IOException;
}
