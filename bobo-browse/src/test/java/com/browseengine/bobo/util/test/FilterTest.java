package com.browseengine.bobo.util.test;

import java.util.BitSet;

import junit.framework.TestCase;

import org.apache.lucene.search.DocIdSetIterator;

import com.browseengine.bobo.docidset.FilteredDocSetIterator;
import com.kamikaze.docidset.impl.IntArrayDocIdSet;

public class FilterTest extends TestCase
{
  public void testFilterdDocSetIterator()
  {
    IntArrayDocIdSet set1 = new IntArrayDocIdSet();
    for (int i=0;i<100;++i)
    {
      set1.addDoc(2*i);         // 100 even numbers
    }
    
    DocIdSetIterator filteredIter = new FilteredDocSetIterator(set1.iterator())
    {

      @Override
      protected boolean match(int doc)
      {
        return doc%5 == 0;
      }
    };
    
    BitSet bs = new BitSet();
    for (int i=0;i<100;++i)
    {
      int n = 10*i;
      if (n < 200)
      {
        bs.set(n);
      }
    }
    
    try
    {
      int doc;
      while((doc=filteredIter.nextDoc())!=DocIdSetIterator.NO_MORE_DOCS)
      {
        if (!bs.get(doc)){
          fail("failed: "+doc+" not in expected set");
          return;
        }
        else
        {
          bs.clear(doc);
        }
      }
      if (bs.cardinality()>0)
      {
        fail("failed: leftover cardinatity: "+bs.cardinality());
      }
    }
    catch(Exception e)
    {
      fail(e.getMessage());
    }
  }
}
