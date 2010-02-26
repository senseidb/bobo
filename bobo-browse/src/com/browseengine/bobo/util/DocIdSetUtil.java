package com.browseengine.bobo.util;

import java.io.IOException;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

public class DocIdSetUtil
{
  private DocIdSetUtil(){}
  
  public static String toString(DocIdSet docIdSet) throws IOException
  {
    DocIdSetIterator iter = docIdSet.iterator();
    StringBuffer buf = new StringBuffer();
    boolean firstTime = true;
    buf.append("[");
    int docid;
    while((docid=iter.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS)
    {
      if (firstTime)
      {
        firstTime = false;
      }
      else
      {
        buf.append(",");
      }
      buf.append(docid);
    }
    buf.append("]");
    return buf.toString();
  }
}
