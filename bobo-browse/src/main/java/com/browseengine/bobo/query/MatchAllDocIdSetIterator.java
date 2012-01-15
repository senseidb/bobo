package com.browseengine.bobo.query;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.DocIdSetIterator;

public class MatchAllDocIdSetIterator extends DocIdSetIterator {
    private final TermDocs _termDocs;
    private int _docid;
	public MatchAllDocIdSetIterator(IndexReader reader) throws IOException {
		_termDocs = reader.termDocs(null);
		_docid = -1;
	}
	@Override
	public int advance(int target) throws IOException {
	    return _docid = _termDocs.skipTo(target) ? _termDocs.doc() : NO_MORE_DOCS;
	}
	
	@Override
	public int docID() {
		return _docid;
	}
	
	@Override
	public int nextDoc() throws IOException {
		return _docid = _termDocs.next() ? _termDocs.doc() : NO_MORE_DOCS;
	}
}
