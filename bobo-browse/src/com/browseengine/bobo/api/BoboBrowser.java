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
  
  public static void gatherSubReaders(List<BoboIndexReader> readerList,BoboIndexReader reader){
	  BoboIndexReader[] subReaders = reader._subReaders;
	  if (subReaders == null){
		  readerList.add(reader);
	  }
	  else{
		  for (int i = 0; i < subReaders.length; i++) {
			 gatherSubReaders(readerList, subReaders[i]);
		  }  
	  }
  }
  
  public static BoboSubBrowser[] createSegmentedBrowsables(List<BoboIndexReader> readerList){
	  BoboSubBrowser[] browsables = new BoboSubBrowser[readerList.size()];
	  int i = 0;
	  for (BoboIndexReader reader : readerList){
		  browsables[i] = new BoboSubBrowser(reader);
		  i++;
	  }
	  return browsables;
  }

  public static Browsable[] createBrowsables(BoboIndexReader reader)
  {
    List<BoboIndexReader> readerList = new ArrayList<BoboIndexReader>();
    gatherSubReaders(readerList, reader);
    return createSegmentedBrowsables(readerList);
  }
  
  public static List<BoboIndexReader> gatherSubReaders(List<BoboIndexReader> readerList){
	  List<BoboIndexReader> subreaderList = new ArrayList<BoboIndexReader>();
	  for (BoboIndexReader reader : readerList){
		  gatherSubReaders(subreaderList, reader);
	  }
	  return subreaderList;
  }
  
  public static Browsable[] createBrowsables(List<BoboIndexReader> readerList){
	  List<BoboIndexReader> subreaders = gatherSubReaders(readerList);
	  return createSegmentedBrowsables(subreaders);
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
