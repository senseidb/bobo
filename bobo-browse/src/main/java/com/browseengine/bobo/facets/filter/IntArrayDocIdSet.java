package com.browseengine.bobo.facets.filter;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

public class IntArrayDocIdSet extends DocIdSet {
  private IntList array = null;

  private int pos = -1;

  public IntArrayDocIdSet(int length) {
    array = new IntArrayList(length);
  }

  public IntArrayDocIdSet() {
    array = new IntArrayList();
  }

  
  public void addDoc(int docid) {
    ++pos;
    array.add(docid);
  }
  
  @Override
  public final boolean isCacheable() {
    return true;
  }

  protected int binarySearchForNearest(int val, int begin, int end) {

    int mid = (begin + end) / 2;
    int midval = array.get(mid);
    
    if(mid == end)
      return midval>=val? mid : -1;
        
    if (midval < val) {
      // Find number equal or greater than the target.
      if (array.get(mid + 1) >= val) return mid + 1;
    
      return binarySearchForNearest(val, mid + 1, end);
    }
    else {
      // Find number equal or greater than the target.
      if (midval == val) return mid;

      return binarySearchForNearest(val, begin, mid);
    }
  }

  class IntArrayDocIdSetIterator extends DocIdSetIterator {
    int lastReturn = -1;

    int cursor = -1;
    
    public IntArrayDocIdSetIterator()
    {
      if(pos == -1) lastReturn = DocIdSetIterator.NO_MORE_DOCS;
    }
    
    @Override
    public int docID() {
      return lastReturn;
    }
    
    @Override
    public int nextDoc() throws IOException {
      if (cursor < pos) {
        return (lastReturn = array.get(++cursor));
      }
      return (lastReturn = DocIdSetIterator.NO_MORE_DOCS);
    }

    @Override
    public int advance(int target) throws IOException {
      if (lastReturn == DocIdSetIterator.NO_MORE_DOCS) return DocIdSetIterator.NO_MORE_DOCS;
      
      if (target <= lastReturn) target = lastReturn + 1;
      
      int end = Math.min(cursor + (target - lastReturn), pos);
      int index = binarySearchForNearest(target, cursor + 1, end);
      
      if (index == -1) {
        cursor = pos;
        return (lastReturn = DocIdSetIterator.NO_MORE_DOCS);
      } else {
        cursor = index;
        return (lastReturn = array.get(cursor));
      }
    }
  }

  @Override
  public IntArrayDocIdSetIterator iterator() {
     return new IntArrayDocIdSetIterator();
  }

  public int size() {
    return pos + 1;
  }
}
