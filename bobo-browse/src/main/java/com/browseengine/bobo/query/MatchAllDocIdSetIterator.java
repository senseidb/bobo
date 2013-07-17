package com.browseengine.bobo.query;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.Bits;

public class MatchAllDocIdSetIterator extends DocIdSetIterator {
  private final Bits _acceptDocs;
  private final int _maxDoc;
  private int _docID;

  public MatchAllDocIdSetIterator(AtomicReader reader, Bits acceptDocs) throws IOException {
    _acceptDocs = acceptDocs;
    _maxDoc = reader.maxDoc();
    _docID = -1;
  }

  @Override
  public int advance(int target) throws IOException {
    _docID = target;
    while (_docID < _maxDoc) {
      if (_acceptDocs == null || _acceptDocs.get(_docID)) {
        return _docID;
      }
      _docID++;
    }
    return NO_MORE_DOCS;
  }

  @Override
  public int docID() {
    return _docID;
  }

  @Override
  public int nextDoc() throws IOException {
    return advance(_docID + 1);
  }

  @Override
  public long cost() {
    // TODO Auto-generated method stub
    return 0;
  }
}
