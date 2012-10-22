/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  John Wang
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * send mail to owner@browseengine.com.
 */

package com.browseengine.bobo.api;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.ReaderUtil;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.RuntimeFacetHandler;
import com.browseengine.bobo.facets.RuntimeFacetHandlerFactory;

/**
 * bobo browse index reader
 * 
 */
public class BoboIndexReader extends FilterIndexReader
{
  private static final String                     SPRING_CONFIG = "bobo.spring";
  private static Logger                           logger        = Logger.getLogger(BoboIndexReader.class);

  protected Map<String, FacetHandler<?>>               _facetHandlerMap;

  protected Collection<FacetHandler<?>>                _facetHandlers;
  protected Collection<RuntimeFacetHandlerFactory<?,?>> _runtimeFacetHandlerFactories;
  protected Map<String,RuntimeFacetHandlerFactory<?,?>> _runtimeFacetHandlerFactoryMap;
  protected WorkArea                                _workArea;

  protected IndexReader _srcReader;
  protected BoboIndexReader[] _subReaders = null;
  protected int[] _starts = null;
  private Directory _dir = null;
  
  private final Map<String,Object> _facetDataMap = new HashMap<String,Object>();
  private final ThreadLocal<Map<String,Object>> _runtimeFacetDataMap = new ThreadLocal<Map<String,Object>>()
  {
    protected Map<String,Object> initialValue() { return new HashMap<String,Object>(); }
  };
  
  private final ThreadLocal<Map<String,RuntimeFacetHandler<?>>> _runtimeFacetHandlerMap = new ThreadLocal<Map<String,RuntimeFacetHandler<?>>>()
  {
    protected Map<String,RuntimeFacetHandler<?>> initialValue() { return new HashMap<String,RuntimeFacetHandler<?>>(); }
  };
  
  /**
   * Constructor
   * 
   * @param reader
   *          Index reader
   * @throws IOException
   */
  public static BoboIndexReader getInstance(IndexReader reader) throws IOException
  {
    return BoboIndexReader.getInstance(reader, null, null, new WorkArea());
  }

  public static BoboIndexReader getInstance(IndexReader reader, WorkArea workArea) throws IOException
  {
    return BoboIndexReader.getInstance(reader, null, null, workArea);
  }

  /**
   * Constructor.
   * 
   * @param reader
   *          index reader
   * @param facetHandlers
   *          List of facet handlers
   * @throws IOException
   */
  public static BoboIndexReader getInstance(IndexReader reader,
                                            Collection<FacetHandler<?>> facetHandlers,
                                            Collection<RuntimeFacetHandlerFactory<?,?>> facetHandlerFactories) throws IOException
  {
    return BoboIndexReader.getInstance(reader, facetHandlers, facetHandlerFactories, new WorkArea());
  }
  
  public static BoboIndexReader getInstance(IndexReader reader,
          Collection<FacetHandler<?>> facetHandlers) throws IOException
  {
    return BoboIndexReader.getInstance(reader, facetHandlers, Collections.EMPTY_LIST, new WorkArea());
  }

  public static BoboIndexReader getInstance(IndexReader reader,
                                            Collection<FacetHandler<?>> facetHandlers,
                                            Collection<RuntimeFacetHandlerFactory<?,?>> facetHandlerFactories,
                                            WorkArea workArea) throws IOException
  {
    BoboIndexReader boboReader = new BoboIndexReader(reader, facetHandlers, facetHandlerFactories, workArea);
    boboReader.facetInit();
    return boboReader;
  }

  public static BoboIndexReader getInstanceAsSubReader(IndexReader reader) throws IOException
  {
    return getInstanceAsSubReader(reader, null, null, new WorkArea());
  }

  public static BoboIndexReader getInstanceAsSubReader(IndexReader reader,
                                                       Collection<FacetHandler<?>> facetHandlers,
                                                       Collection<RuntimeFacetHandlerFactory<?,?>> facetHandlerFactories) throws IOException
  {
    return getInstanceAsSubReader(reader, facetHandlers, facetHandlerFactories, new WorkArea());
  }

  public static BoboIndexReader getInstanceAsSubReader(IndexReader reader,
                                                       Collection<FacetHandler<?>> facetHandlers,
                                                       Collection<RuntimeFacetHandlerFactory<?,?>> facetHandlerFactories,
                                                       WorkArea workArea) throws IOException
  {
    BoboIndexReader boboReader = new BoboIndexReader(reader, facetHandlers, facetHandlerFactories, workArea, false);
    boboReader.facetInit();
    return boboReader;
  }
  
  

  @Override
  public long getVersion() {
	try {
		SegmentInfos sinfos = new SegmentInfos();
		sinfos.read(_dir);
		return sinfos.getVersion();
	} catch (Exception e) {
		return 0L;
	} 
  }

  public IndexReader getInnerReader()
  {
    return in;
  }
  
  @Override
  public synchronized IndexReader reopen() throws CorruptIndexException,
		IOException {
		  throw new UnsupportedOperationException();
	/*
   IndexReader newInner = null;

	SegmentInfos sinfos = new SegmentInfos();
	sinfos.read(_dir);
	int size = sinfos.size();
	  
	if (in instanceof MultiReader){
		// setup current reader list
	  List<IndexReader> boboReaderList = new LinkedList<IndexReader>();
	  ReaderUtil.gatherSubReaders((List<IndexReader>)boboReaderList, in);
	  Map<String,BoboIndexReader> readerMap = new HashMap<String,BoboIndexReader>();
	  for (IndexReader reader : boboReaderList){
		  BoboIndexReader boboReader = (BoboIndexReader)reader;
		  SegmentReader sreader = (SegmentReader)(boboReader.in);
		  readerMap.put(sreader.getSegmentName(),boboReader);
	  }
	  
	  ArrayList<BoboIndexReader> currentReaders = new ArrayList<BoboIndexReader>(size);
	  boolean isNewReader = false;
	  for (int i=0;i<size;++i){
		  SegmentInfo sinfo = (SegmentInfo)sinfos.info(i);
		  BoboIndexReader breader = readerMap.remove(sinfo.name);
		  if (breader!=null){
			  // should use SegmentReader.reopen
			  // TODO: see LUCENE-2559
			  BoboIndexReader newReader = (BoboIndexReader)breader.reopen(true);
			  if (newReader!=breader){
				  isNewReader = true;
			  }
			  if (newReader!=null){
			    currentReaders.add(newReader);
			  }
		  }
		  else{
			  isNewReader = true;
			  SegmentReader newSreader = SegmentReader.get(true, sinfo, 1);
			  breader = BoboIndexReader.getInstanceAsSubReader(newSreader,this._facetHandlers,this._runtimeFacetHandlerFactories);
			  breader._dir = _dir;
			  currentReaders.add(breader);
		  }
	  }
	  isNewReader = isNewReader || (readerMap.size() != 0);
	  if (!isNewReader){
		  return this;
	  }
	  else{
		  MultiReader newMreader = new MultiReader(currentReaders.toArray(new BoboIndexReader[currentReaders.size()]),false);
		  BoboIndexReader newReader = BoboIndexReader.getInstanceAsSubReader(newMreader,this._facetHandlers,this._runtimeFacetHandlerFactories);
		  newReader._dir = _dir;
		  return newReader;
	  }
	}
	else if (in instanceof SegmentReader){
	   // should use SegmentReader.reopen
		// TODO: see LUCENE-2559
		
	  SegmentReader sreader = (SegmentReader)in;
	  int numDels = sreader.numDeletedDocs();
	  
	  SegmentInfo sinfo = null;
	  boolean sameSeg = false;
	  //get SegmentInfo instance
	  for (int i=0;i<size;++i){
		SegmentInfo sinfoTmp = (SegmentInfo)sinfos.info(i);
		if (sinfoTmp.name.equals(sreader.getSegmentName())){
		  int numDels2 = sinfoTmp.getDelCount();
		  sameSeg = numDels==numDels2;
		  sinfo = sinfoTmp;
		  break;
		}
	  }
	 
	  if (sinfo == null){
		  // segment no longer exists
		  return null;
	  }
	  if (sameSeg){
	    return this;	
	  }
	  else{
		SegmentReader newSreader = SegmentReader.get(true, sinfo, 1);
		return BoboIndexReader.getInstanceAsSubReader(newSreader,this._facetHandlers,this._runtimeFacetHandlerFactories);
	  }
	}
	else{
	  // should not reach here, a catch-all default case
	  IndexReader reader = in.reopen(true);
	  if (in!=reader){
	    return BoboIndexReader.getInstance(newInner, _facetHandlers, _runtimeFacetHandlerFactories, _workArea);
	  }
	  else{
		return this;
	  }
	}*/
  }

  @Override
  public synchronized IndexReader reopen(boolean openReadOnly)
		throws CorruptIndexException, IOException {

	// bobo readers are always readonly 
	return reopen();
  }

  public Object getFacetData(String name){
	  return _facetDataMap.get(name);
  }
  
  public Object putFacetData(String name,Object data){
	  return _facetDataMap.put(name, data);
  }
  
  public Object getRuntimeFacetData(String name)
  {
    Map<String,Object> map = _runtimeFacetDataMap.get();
    if(map == null) return null;

    return map.get(name);
  }

  public Object putRuntimeFacetData(String name,Object data)
  {
    Map<String,Object> map = _runtimeFacetDataMap.get();
    if(map == null)
    {
      map = new HashMap<String,Object>();
      _runtimeFacetDataMap.set(map);
    }
    return map.put(name, data);
  }

  public void clearRuntimeFacetData()
  {
    _runtimeFacetDataMap.set(null);
  }

  public RuntimeFacetHandler<?> getRuntimeFacetHandler(String name)
  {
    Map<String,RuntimeFacetHandler<?>> map = _runtimeFacetHandlerMap.get();
    if(map == null) return null;

    return map.get(name);
  }

  public void putRuntimeFacetHandler(String name,RuntimeFacetHandler<?> data)
  {
    Map<String,RuntimeFacetHandler<?>> map = _runtimeFacetHandlerMap.get();
    if(map == null)
    {
      map = new HashMap<String,RuntimeFacetHandler<?>>();
      _runtimeFacetHandlerMap.set(map);
    }
    map.put(name, data);
  }

  public void clearRuntimeFacetHandler()
  {
    _runtimeFacetHandlerMap.set(null);
  }
  
  
  @Override
  protected void doClose() throws IOException
  {
   
    
    //We can not clean up the facetDataMap, as it might be used by other BoboIndexReaders created by the copy method 
    /*_facetDataMap.clear();
    if(_srcReader != null) _srcReader.close();*/
    //super.doClose();
  }
  
  @Override
  protected void doCommit(Map commitUserData) throws IOException
  {
    if(_srcReader != null) _srcReader.flush(commitUserData);
  }

  @Override
  protected void doDelete(int n) throws  CorruptIndexException, IOException
  {
    if(_srcReader != null) _srcReader.deleteDocument(n);
  }
  
  private void loadFacetHandler(String name,
                                Set<String> loaded,
                                Set<String> visited,
                                WorkArea workArea) throws IOException
  {
    FacetHandler<?> facetHandler = _facetHandlerMap.get(name);
    if (facetHandler != null && !loaded.contains(name))
    {
      visited.add(name);
      Set<String> dependsOn = facetHandler.getDependsOn();
      if (dependsOn.size() > 0)
      {
        Iterator<String> iter = dependsOn.iterator();
        while (iter.hasNext())
        {
          String f = iter.next();
          if (name.equals(f))
            continue;
          if (!loaded.contains(f))
          {
            if (visited.contains(f))
            {
              throw new IOException("Facet handler dependency cycle detected, facet handler: "
                  + name + " not loaded");
            }
            loadFacetHandler(f, loaded, visited, workArea);
          }
          if (!loaded.contains(f))
          {
            throw new IOException("unable to load facet handler: " + f);
          }
          facetHandler.putDependedFacetHandler(_facetHandlerMap.get(f));
        }
      }

      long start = System.currentTimeMillis();
      facetHandler.loadFacetData(this, workArea);
      long end = System.currentTimeMillis();
      if (logger.isDebugEnabled()){
    	StringBuffer buf = new StringBuffer();
    	buf.append("facetHandler loaded: ").append(name).append(", took: ").append(end-start).append(" ms");
        logger.debug(buf.toString());
      }
      loaded.add(name);
    }
  }
  
  private void loadFacetHandlers(WorkArea workArea, Set<String> toBeRemoved) throws IOException
  {
    Set<String> loaded = new HashSet<String>();
    Set<String> visited = new HashSet<String>();

    for(String name : _facetHandlerMap.keySet())
    {
      loadFacetHandler(name, loaded, visited, workArea);
    }

    for(String name : toBeRemoved)
    {
      _facetHandlerMap.remove(name);
    }
  }

  /**
   * Find all the leaf sub-readers and wrap each in BoboIndexReader.
   * @param reader
   * @param workArea
   * @return
   * @throws IOException
   */
  private static IndexReader[] createSubReaders(IndexReader reader, WorkArea workArea) throws IOException
  {
    List<IndexReader> readerList = new ArrayList<IndexReader>();
    ReaderUtil.gatherSubReaders(readerList, reader);
    IndexReader[] subReaders = (IndexReader[])readerList.toArray(new IndexReader[readerList.size()]);
    BoboIndexReader[] boboReaders;
    
    if(subReaders != null && subReaders.length > 0)
    {
      boboReaders = new BoboIndexReader[subReaders.length];
      for(int i = 0; i < subReaders.length; i++)
      {
        boboReaders[i] = new BoboIndexReader(subReaders[i], null, null, workArea, false);
      }
    }
    else
    {
      boboReaders = new BoboIndexReader[]{ new BoboIndexReader(reader, null, null, workArea, false) };
    }
    return boboReaders;
  }
  
  @Override
  public Directory directory()
  {
    return (_subReaders != null ? _subReaders[0].directory() : super.directory());
  }
  
  private static Collection<FacetHandler<?>> loadFromIndex(File file,WorkArea workArea) throws IOException
  {
   // File springFile = new File(file, SPRING_CONFIG);
   // FileSystemXmlApplicationContext appCtx =
     //   new FileSystemXmlApplicationContext("file:" + springFile.getAbsolutePath());
    //return (Collection<FacetHandler<?>>) appCtx.getBean("handlers");
	  
	  Set<Entry<Class<?>,Object>> entries = workArea.map.entrySet();
      FileSystemXmlApplicationContext appCtx = new FileSystemXmlApplicationContext();
      for (Entry<Class<?>,Object> entry : entries){
		  Object obj = entry.getValue();
		  if (obj instanceof ClassLoader){
			  appCtx.setClassLoader((ClassLoader)obj);
	    	  break;
		  }
	  }
	  
      String absolutePath = file.getAbsolutePath();
      String partOne = absolutePath.substring(0, absolutePath.lastIndexOf(File.separator ));
      String partTwo = URLEncoder.encode(absolutePath.substring(absolutePath.lastIndexOf(File.separator ) + 1), "UTF-8");
      absolutePath = partOne + File.separator + partTwo;
      
      File springFile = new File(new File(absolutePath), SPRING_CONFIG);
      appCtx.setConfigLocation("file:" + springFile.getAbsolutePath());
      appCtx.refresh();
      
      return (Collection<FacetHandler<?>>) appCtx.getBean("handlers");

  }
  
  protected void initialize(Collection<FacetHandler<?>> facetHandlers) throws IOException
  {
    if (facetHandlers == null) // try to load from index
    {
      Directory idxDir = directory();
      if (idxDir != null && idxDir instanceof FSDirectory)
      {
        FSDirectory fsDir = (FSDirectory) idxDir;
        File file = fsDir.getFile();

        if (new File(file, SPRING_CONFIG).exists())
        {
          facetHandlers = loadFromIndex(file,_workArea);
        }
        else
        {
          facetHandlers = new ArrayList<FacetHandler<?>>();
        }
      }
      else
      {
        facetHandlers = new ArrayList<FacetHandler<?>>();
      }
    }
    
    _facetHandlers = facetHandlers;
    _facetHandlerMap = new HashMap<String, FacetHandler<?>>();
    for (FacetHandler<?> facetHandler : facetHandlers)
    {
      _facetHandlerMap.put(facetHandler.getName(), facetHandler);
    }
  }

  /**
   * 
   * @param reader
   * @param facetHandlers
   * @param facetHandlerFactories
   * @param workArea
   * @throws IOException
   */
  protected BoboIndexReader(IndexReader reader,
                            Collection<FacetHandler<?>> facetHandlers,
                            Collection<RuntimeFacetHandlerFactory<?,?>> facetHandlerFactories,
                            WorkArea workArea) throws IOException
  {
    this(reader, facetHandlers, facetHandlerFactories, workArea, true);
    _srcReader = reader;
  }
  
  /**
   * @param reader
   * @param facetHandlers
   * @param facetHandlerFactories
   * @param workArea
   * @param useSubReaders true => we create a MultiReader of all the leaf sub-readers as
   * the inner reader. false => we use the given reader as the inner reader.
   * @throws IOException
   */
  protected BoboIndexReader(IndexReader reader,
                            Collection<FacetHandler<?>> facetHandlers,
                            Collection<RuntimeFacetHandlerFactory<?,?>> facetHandlerFactories,
                            WorkArea workArea,
                            boolean useSubReaders) throws IOException
  {
    super(useSubReaders ? new MultiReader(createSubReaders(reader, workArea), false) : reader);
    if(useSubReaders)
    {
      _dir = reader.directory();
      BoboIndexReader[] subReaders = (BoboIndexReader[])in.getSequentialSubReaders();
      if(subReaders != null && subReaders.length > 0)
      {
        _subReaders = subReaders;
        
        int maxDoc = 0;
        _starts = new int[_subReaders.length + 1];
        for (int i = 0; i < _subReaders.length; i++)
        {
          _subReaders[i]._dir = _dir;
          if(facetHandlers != null) _subReaders[i].setFacetHandlers(facetHandlers);
          _starts[i] = maxDoc;
          maxDoc += _subReaders[i].maxDoc();
        }
        _starts[_subReaders.length] = maxDoc;
      }
    }
    _runtimeFacetHandlerFactories = facetHandlerFactories;
    _runtimeFacetHandlerFactoryMap = new HashMap<String,RuntimeFacetHandlerFactory<?,?>>();
    if (_runtimeFacetHandlerFactories!=null)
    {
      for(RuntimeFacetHandlerFactory<?,?> factory : _runtimeFacetHandlerFactories)
      {
        _runtimeFacetHandlerFactoryMap.put(factory.getName(), factory);
      }
    }
    _facetHandlers = facetHandlers;
    _workArea = workArea;
  }

  protected void facetInit() throws IOException
  {
    facetInit(new HashSet<String>());
  }
  
  protected void facetInit(Set<String> toBeRemoved) throws IOException
  {
    initialize(_facetHandlers);
    if(_subReaders == null)
    {
      loadFacetHandlers(_workArea, toBeRemoved);  
    }
    else
    {
      for(BoboIndexReader r : _subReaders)
      {
        r.facetInit(toBeRemoved);
      }
      
      for(String name : toBeRemoved)
      {
        _facetHandlerMap.remove(name);
      }
    }
  }

  protected void setFacetHandlers(Collection<FacetHandler<?>> facetHandlers)
  {
    _facetHandlers = facetHandlers;
  }
  /**
   * @deprecated use {@link org.apache.lucene.search.MatchAllDocsQuery} instead.
   * @return query that matches all docs in the index
   */
  public Query getFastMatchAllDocsQuery()
  {
    return new MatchAllDocsQuery();
  }

  /**
   * Utility method to dump out all fields (name and terms) for a given index.
   * 
   * @param outFile
   *          File to dump to.
   * @throws IOException
   */
  public void dumpFields(File outFile) throws IOException
  {
    FileWriter writer = null;
    try
    {
      writer = new FileWriter(outFile);
      PrintWriter out = new PrintWriter(writer);
      Set<String> fieldNames = getFacetNames();
      for (String fieldName : fieldNames)
      {
        TermEnum te = terms(new Term(fieldName, ""));
        out.write(fieldName + ":\n");
        while (te.next())
        {
          Term term = te.term();
          if (!fieldName.equals(term.field()))
          {
            break;
          }
          out.write(term.text() + "\n");
        }
        out.write("\n\n");
      }
    }
    finally
    {
      if (writer != null)
      {
        writer.close();
      }
    }
  }

  /**
   * Gets all the facet field names
   * 
   * @return Set of facet field names
   */
  public Set<String> getFacetNames()
  {
    return _facetHandlerMap.keySet();
  }

  /**
   * Gets a facet handler
   * 
   * @param fieldname
   *          name
   * @return facet handler
   */
  public FacetHandler<?> getFacetHandler(String fieldname)
  {
    FacetHandler<?> f = _facetHandlerMap.get(fieldname);
    if(f == null)
      f = getRuntimeFacetHandler(fieldname);
    return f;
  }
  
  

  @Override
  public IndexReader[] getSequentialSubReaders() {
	return _subReaders;
  }

/**
   * Gets the facet handler map
   * 
   * @return facet handler map
   */
  public Map<String, FacetHandler<?>> getFacetHandlerMap()
  {
    return _facetHandlerMap;
  }

  
  /**
   * @return the map of RuntimeFacetHandlerFactories
   */
  public Map<String,RuntimeFacetHandlerFactory<?, ?>> getRuntimeFacetHandlerFactoryMap()
  {
    return _runtimeFacetHandlerFactoryMap;
  }

  /**
   * @return the map of RuntimeFacetHandlers
   */
  public Map<String, RuntimeFacetHandler<?>> getRuntimeFacetHandlerMap()
  {
    return _runtimeFacetHandlerMap.get();
  }

  /**
   * @return the map of RuntimeFacetData
   */
  public Map<String, Object> getRuntimeFacetDataMap()
  {
    return _runtimeFacetDataMap.get();
  }

  public void setRuntimeFacetHandlerMap(Map<String, RuntimeFacetHandler<?>> map)
  {
    _runtimeFacetHandlerMap.set(map);
  }

  public void setRuntimeFacetDataMap(Map<String, Object> map)
  {
    _runtimeFacetDataMap.set(map);
  }

  @Override
  public Document document(int docid) throws IOException
  {
    if(_subReaders != null)
    {
      int readerIndex = readerIndex(docid, _starts, _subReaders.length);
      BoboIndexReader subReader = _subReaders[readerIndex];
      return subReader.document(docid - _starts[readerIndex]);
    }
    else
    {
      Document doc = super.document(docid);
      Collection<FacetHandler<?>> facetHandlers = _facetHandlerMap.values();
      for (FacetHandler<?> facetHandler : facetHandlers)
      {
        String[] vals = facetHandler.getFieldValues(this,docid);
        if (vals != null)
        {
          String[] values = doc.getValues(facetHandler.getName());
          Set<String> storedVals = new HashSet<String>(Arrays.asList(values));
        	
          for (String val : vals)
          {
        	storedVals.add(val);
          }
          doc.removeField(facetHandler.getName());
          
          for (String val : storedVals){
        	  doc.add(new Field(facetHandler.getName(),
                      val,
                      Field.Store.NO,
                      Field.Index.NOT_ANALYZED));
          }
        }
      }
      return doc;
    }
  }
  
  private static int readerIndex(int n, int[] starts, int numSubReaders)
  {
    int lo = 0;
    int hi = numSubReaders - 1;

    while (hi >= lo)
    {
      int mid = (lo + hi) >>> 1;
      int midValue = starts[mid];
      if (n < midValue)
        hi = mid - 1;
      else if (n > midValue)
        lo = mid + 1;
      else
      {
        while (mid+1 < numSubReaders && starts[mid+1] == midValue)
        {
          mid++;
        }
        return mid;
      }
    }
    return hi;
  }

  /**
   * Work area for loading
   */
  public static class WorkArea
  {
    HashMap<Class<?>, Object> map = new HashMap<Class<?>, Object>();

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> cls)
    {
      T obj = (T) map.get(cls);
      return obj;
    }

    public void put(Object obj)
    {
      map.put(obj.getClass(), obj);
    }
    

    public void clear()
    {
      map.clear();
    }
    
    @Override
    public String toString(){
    	return map.toString();
    }
  }

  private BoboIndexReader(IndexReader in)
  {
    super(in);
  }
  
  public BoboIndexReader copy(IndexReader in)
  {
    if(_subReaders != null)
    {
      throw new IllegalStateException("this BoboIndexReader has subreaders");
    }
    BoboIndexReader copy = new BoboIndexReader(in);
    copy._facetHandlerMap = this._facetHandlerMap;
    copy._facetHandlers = this._facetHandlers;
    copy._runtimeFacetHandlerFactories = this._runtimeFacetHandlerFactories;
    copy._runtimeFacetHandlerFactoryMap = this._runtimeFacetHandlerFactoryMap;
    copy._workArea = this._workArea;
    copy._facetDataMap.putAll(this._facetDataMap);
    copy._srcReader = in;
    copy._starts = this._starts;    
    return copy;
  }
  

  
}
