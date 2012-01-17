package com.browseengine.bobo.query;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Scorer;

public interface ScorerBuilder {
  Scorer createScorer(Scorer innerScorer, IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException;
  Explanation explain(IndexReader reader,int doc,Explanation innerExplaination) throws IOException;
}
