package com.browseengine.bobo.docidset;

import org.apache.lucene.search.DocIdSet;

public abstract class RandomAccessDocIdSet extends DocIdSet
{
  public abstract boolean get(int docId);
}
