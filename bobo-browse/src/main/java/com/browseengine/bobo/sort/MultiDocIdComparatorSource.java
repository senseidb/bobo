/**
 * 
 */
package com.browseengine.bobo.sort;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

/**
 * @author ymatsuda
 *
 */
public class MultiDocIdComparatorSource extends DocComparatorSource
{
  private DocComparatorSource[] _compSources;
  
  public MultiDocIdComparatorSource(DocComparatorSource[] compSources)
  {
    _compSources = compSources;
  }
  
  @Override
  public DocComparator getComparator(IndexReader reader, int docBase) throws IOException
  {
    DocComparator[] comparators = new DocComparator[_compSources.length];
    for (int i=0; i<_compSources.length; ++i)
    {
      comparators[i] = _compSources[i].getComparator(reader,docBase);
    }
    return new MultiDocIdComparator(comparators);
  }
}
