/**
 * 
 */
package com.browseengine.bobo.search.section;

import org.apache.lucene.index.Term;

/**
 *
 */
public interface MetaDataCacheProvider
{
  public MetaDataCache get(Term term);
}
