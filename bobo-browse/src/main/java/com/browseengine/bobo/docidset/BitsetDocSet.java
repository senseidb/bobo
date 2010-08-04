package com.browseengine.bobo.docidset;

import java.io.IOException;
import java.util.BitSet;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

public class BitsetDocSet extends DocIdSet {
	private final BitSet _bs;
	public BitsetDocSet() {
		_bs=new BitSet();
	}
	
	public BitsetDocSet(int nbits) {
		_bs=new BitSet(nbits);
	}

	public void addDoc(int docid) {
		_bs.set(docid);
	}

	public int size() {
		return _bs.cardinality();
	}

	@Override
	public DocIdSetIterator iterator() {
		return new BitsDocIdSetIterator(_bs);
	}

	private static class BitsDocIdSetIterator extends DocIdSetIterator
	{
		private final BitSet _bs;
		private int _current;
		BitsDocIdSetIterator(BitSet bs)
		{
			_bs=bs;
			_current=-1;
		}
		
		@Override
		public int docID() {
			return _current;
		}

		@Override
		public int nextDoc() throws IOException {
			return _bs.nextSetBit(_current+1);
		}

		@Override
		public int advance(int target) throws IOException {
			return _bs.nextSetBit(target);
		}
		
	}
}
