package com.browseengine.bobo.sort;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;

public abstract class DocComparator{
  public abstract int compare(ScoreDoc doc1, ScoreDoc doc2);
  
  public abstract Comparable value(ScoreDoc doc);
  
  public DocComparator setScorer(Scorer scorer){
	  return this;
  }
}
