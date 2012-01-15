package com.browseengine.bobo.docidset;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;


public final class EmptyDocIdSet extends RandomAccessDocIdSet 
{
  private static EmptyDocIdSet SINGLETON=new EmptyDocIdSet();

  private static class EmptyDocIdSetIterator extends DocIdSetIterator
  {
    @Override
    public int docID() {	return -1; }

    @Override
    public int nextDoc() throws IOException { return DocIdSetIterator.NO_MORE_DOCS;  }

    @Override
    public int advance(int target) throws IOException {return DocIdSetIterator.NO_MORE_DOCS; }
  }

  private static EmptyDocIdSetIterator SINGLETON_ITERATOR = new EmptyDocIdSetIterator();

  private EmptyDocIdSet() { }

  public static EmptyDocIdSet getInstance()
  {
    return SINGLETON;
  }

  @Override
  public DocIdSetIterator iterator() 
  {
    return SINGLETON_ITERATOR;
  }

  @Override
  public boolean get(int docId)
  {
    return false;
  }

}
