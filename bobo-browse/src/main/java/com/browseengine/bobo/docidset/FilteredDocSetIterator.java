package com.browseengine.bobo.docidset;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;


public abstract class FilteredDocSetIterator extends DocIdSetIterator {
	protected DocIdSetIterator _innerIter;
	private int _currentDoc;
	
	public FilteredDocSetIterator(DocIdSetIterator innerIter)
	{
		if (innerIter == null)
		{
			throw new IllegalArgumentException("null iterator");
		}
		_innerIter=innerIter;
		_currentDoc=-1;
	}
	
	abstract protected boolean match(int doc);
	
	public final int docID() {
		return _currentDoc;
	}

	public final int nextDoc() throws IOException{
		int docid = _innerIter.nextDoc();
		while(docid!=DocIdSetIterator.NO_MORE_DOCS)
		{
			if (match(docid))
			{
				_currentDoc=docid;
				return docid;
			}
			else{
				docid = _innerIter.nextDoc();
			}
		}
		return DocIdSetIterator.NO_MORE_DOCS;
	}

	public final int advance(int n) throws IOException{
		int docid =_innerIter.advance(n);
		while (docid!=DocIdSetIterator.NO_MORE_DOCS)
		{
			if (match(docid))
			{
			  _currentDoc=docid;
			  return docid;
			}
			else
			{
			  docid=_innerIter.nextDoc();
			}
		}
		return DocIdSetIterator.NO_MORE_DOCS;
	}

}
