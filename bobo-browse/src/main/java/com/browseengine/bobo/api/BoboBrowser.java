/**
 * 
 */
package com.browseengine.bobo.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.util.ReaderUtil;

import com.browseengine.bobo.facets.FacetHandler;

/**
 * @author ymatsuda
 *
 */
public class BoboBrowser extends MultiBoboBrowser
{
  /**
   * @param reader BoboIndexReader
   * @throws IOException
   */
  public BoboBrowser(BoboIndexReader reader) throws IOException
  {
    super(createBrowsables(reader));
  }

  public static Browsable[] createBrowsables(BoboIndexReader reader)
  {
    List<IndexReader> readerList = new ArrayList<IndexReader>();
    ReaderUtil.gatherSubReaders(readerList, reader);
    IndexReader[] subReaders = (IndexReader[])readerList.toArray(new IndexReader[readerList.size()]);
    if(subReaders == null || subReaders.length == 0)
    {
      return new Browsable[]{ new BoboSubBrowser(reader) };
    }
    else
    {
      Browsable[] subBrowsables = new Browsable[subReaders.length];
      for(int i = 0; i < subReaders.length; i++)
      {
        subBrowsables[i] = new BoboSubBrowser((BoboIndexReader)subReaders[i]);
      }
      return subBrowsables;
    }
  }
  
  public static Browsable[] createBrowsables(List<BoboIndexReader> readerList){
	  List<Browsable> browsableList = new ArrayList<Browsable>();
	  for (BoboIndexReader boboReader : readerList){
		  Browsable[] sub = createBrowsables(boboReader);
		  browsableList.addAll(Arrays.asList(sub));
	  }
	  return browsableList.toArray(new Browsable[browsableList.size()]);
  }
  
  /**
   * Gets a set of facet names
   * 
   * @return set of facet names
   */
  public Set<String> getFacetNames()
  {
    return _subBrowsers[0].getFacetNames();
  }
  
  public FacetHandler<?> getFacetHandler(String name)
  {
    return _subBrowsers[0].getFacetHandler(name);
  }
}
