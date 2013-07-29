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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DocumentStoredFieldVisitor;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FilterAtomicReader;
import org.apache.lucene.index.StoredFieldVisitor;

import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.RuntimeFacetHandler;
import com.browseengine.bobo.facets.RuntimeFacetHandlerFactory;

public class BoboSegmentReader extends FilterAtomicReader {
  private static Logger logger = Logger.getLogger(BoboSegmentReader.class);

  protected Map<String, FacetHandler<?>> _facetHandlerMap;

  protected Collection<FacetHandler<?>> _facetHandlers;
  protected Collection<RuntimeFacetHandlerFactory<?, ?>> _runtimeFacetHandlerFactories;
  protected Map<String, RuntimeFacetHandlerFactory<?, ?>> _runtimeFacetHandlerFactoryMap;
  protected WorkArea _workArea;

  private final Map<String, Object> _facetDataMap = new HashMap<String, Object>();
  private final ThreadLocal<Map<String, Object>> _runtimeFacetDataMap = new ThreadLocal<Map<String, Object>>() {
    @Override
    protected Map<String, Object> initialValue() {
      return new HashMap<String, Object>();
    }
  };

  private final ThreadLocal<Map<String, RuntimeFacetHandler<?>>> _runtimeFacetHandlerMap = new ThreadLocal<Map<String, RuntimeFacetHandler<?>>>() {
    @Override
    protected Map<String, RuntimeFacetHandler<?>> initialValue() {
      return new HashMap<String, RuntimeFacetHandler<?>>();
    }
  };

  public static BoboSegmentReader getInstance(AtomicReader reader,
      Collection<FacetHandler<?>> facetHandlers,
      Collection<RuntimeFacetHandlerFactory<?, ?>> facetHandlerFactories) throws IOException {
    return getInstance(reader, facetHandlers, facetHandlerFactories, new WorkArea());
  }

  private static BoboSegmentReader getInstance(AtomicReader reader,
      Collection<FacetHandler<?>> facetHandlers,
      Collection<RuntimeFacetHandlerFactory<?, ?>> facetHandlerFactories, WorkArea workArea)
      throws IOException {
    BoboSegmentReader boboReader = new BoboSegmentReader(reader, facetHandlers,
        facetHandlerFactories, workArea);
    boboReader.facetInit();
    return boboReader;
  }

  public Object getFacetData(String name) {
    return _facetDataMap.get(name);
  }

  public Object putFacetData(String name, Object data) {
    return _facetDataMap.put(name, data);
  }

  public Object getRuntimeFacetData(String name) {
    Map<String, Object> map = _runtimeFacetDataMap.get();
    if (map == null) return null;

    return map.get(name);
  }

  public Object putRuntimeFacetData(String name, Object data) {
    Map<String, Object> map = _runtimeFacetDataMap.get();
    if (map == null) {
      map = new HashMap<String, Object>();
      _runtimeFacetDataMap.set(map);
    }
    return map.put(name, data);
  }

  public void clearRuntimeFacetData() {
    _runtimeFacetDataMap.set(null);
  }

  public RuntimeFacetHandler<?> getRuntimeFacetHandler(String name) {
    Map<String, RuntimeFacetHandler<?>> map = _runtimeFacetHandlerMap.get();
    if (map == null) return null;

    return map.get(name);
  }

  public void putRuntimeFacetHandler(String name, RuntimeFacetHandler<?> data) {
    Map<String, RuntimeFacetHandler<?>> map = _runtimeFacetHandlerMap.get();
    if (map == null) {
      map = new HashMap<String, RuntimeFacetHandler<?>>();
      _runtimeFacetHandlerMap.set(map);
    }
    map.put(name, data);
  }

  public void clearRuntimeFacetHandler() {
    _runtimeFacetHandlerMap.set(null);
  }

  @Override
  protected void doClose() throws IOException {
    // do nothing
  }

  private void loadFacetHandler(String name, Set<String> loaded, Set<String> visited,
      WorkArea workArea) throws IOException {
    FacetHandler<?> facetHandler = _facetHandlerMap.get(name);
    if (facetHandler != null && !loaded.contains(name)) {
      visited.add(name);
      Set<String> dependsOn = facetHandler.getDependsOn();
      if (dependsOn.size() > 0) {
        Iterator<String> iter = dependsOn.iterator();
        while (iter.hasNext()) {
          String f = iter.next();
          if (name.equals(f)) continue;
          if (!loaded.contains(f)) {
            if (visited.contains(f)) {
              throw new IOException("Facet handler dependency cycle detected, facet handler: "
                  + name + " not loaded");
            }
            loadFacetHandler(f, loaded, visited, workArea);
          }
          if (!loaded.contains(f)) {
            throw new IOException("unable to load facet handler: " + f);
          }
          facetHandler.putDependedFacetHandler(_facetHandlerMap.get(f));
        }
      }

      long start = System.currentTimeMillis();
      facetHandler.loadFacetData(this, workArea);
      long end = System.currentTimeMillis();
      if (logger.isDebugEnabled()) {
        StringBuffer buf = new StringBuffer();
        buf.append("facetHandler loaded: ").append(name).append(", took: ").append(end - start)
            .append(" ms");
        logger.debug(buf.toString());
      }
      loaded.add(name);
    }
  }

  private void loadFacetHandlers(WorkArea workArea) throws IOException {
    Set<String> loaded = new HashSet<String>();
    Set<String> visited = new HashSet<String>();

    for (String name : _facetHandlerMap.keySet()) {
      loadFacetHandler(name, loaded, visited, workArea);
    }
  }

  protected void initialize(Collection<FacetHandler<?>> facetHandlers) throws IOException {
    _facetHandlers = facetHandlers;
    _facetHandlerMap = new HashMap<String, FacetHandler<?>>();
    for (FacetHandler<?> facetHandler : facetHandlers) {
      _facetHandlerMap.put(facetHandler.getName(), facetHandler);
    }
  }

  /**
   * @param reader
   * @param facetHandlers
   * @param facetHandlerFactories
   * @param workArea
   * the inner reader. false => we use the given reader as the inner reader.
   * @throws IOException
   */
  protected BoboSegmentReader(AtomicReader reader, Collection<FacetHandler<?>> facetHandlers,
      Collection<RuntimeFacetHandlerFactory<?, ?>> facetHandlerFactories, WorkArea workArea)
      throws IOException {
    super(reader);
    _runtimeFacetHandlerFactories = facetHandlerFactories;
    _runtimeFacetHandlerFactoryMap = new HashMap<String, RuntimeFacetHandlerFactory<?, ?>>();
    if (_runtimeFacetHandlerFactories != null) {
      for (RuntimeFacetHandlerFactory<?, ?> factory : _runtimeFacetHandlerFactories) {
        _runtimeFacetHandlerFactoryMap.put(factory.getName(), factory);
      }
    }
    _facetHandlers = facetHandlers;
    _workArea = workArea;
  }

  protected void facetInit() throws IOException {
    initialize(_facetHandlers);
    loadFacetHandlers(_workArea);
  }

  /**
   * Gets all the facet field names
   *
   * @return Set of facet field names
   */
  public Set<String> getFacetNames() {
    return _facetHandlerMap.keySet();
  }

  /**
   * Gets a facet handler
   *
   * @param fieldname
   *          name
   * @return facet handler
   */
  public FacetHandler<?> getFacetHandler(String fieldname) {
    FacetHandler<?> f = _facetHandlerMap.get(fieldname);
    if (f == null) f = getRuntimeFacetHandler(fieldname);
    return f;
  }

  /**
     * Gets the facet handler map
     *
     * @return facet handler map
     */
  public Map<String, FacetHandler<?>> getFacetHandlerMap() {
    return _facetHandlerMap;
  }

  /**
   * @return the map of RuntimeFacetHandlerFactories
   */
  public Map<String, RuntimeFacetHandlerFactory<?, ?>> getRuntimeFacetHandlerFactoryMap() {
    return _runtimeFacetHandlerFactoryMap;
  }

  /**
   * @return the map of RuntimeFacetHandlers
   */
  public Map<String, RuntimeFacetHandler<?>> getRuntimeFacetHandlerMap() {
    return _runtimeFacetHandlerMap.get();
  }

  /**
   * @return the map of RuntimeFacetData
   */
  public Map<String, Object> getRuntimeFacetDataMap() {
    return _runtimeFacetDataMap.get();
  }

  public void setRuntimeFacetHandlerMap(Map<String, RuntimeFacetHandler<?>> map) {
    _runtimeFacetHandlerMap.set(map);
  }

  public void setRuntimeFacetDataMap(Map<String, Object> map) {
    _runtimeFacetDataMap.set(map);
  }

  @Override
  public void document(int docID, StoredFieldVisitor visitor) throws IOException {
    super.document(docID, visitor);
    if (!(visitor instanceof DocumentStoredFieldVisitor)) {
      return;
    }

    Document doc = ((DocumentStoredFieldVisitor) visitor).getDocument();

    Collection<FacetHandler<?>> facetHandlers = _facetHandlerMap.values();
    for (FacetHandler<?> facetHandler : facetHandlers) {
      String[] vals = facetHandler.getFieldValues(this, docID);
      if (vals != null) {
        String[] values = doc.getValues(facetHandler.getName());
        Set<String> storedVals = new HashSet<String>(Arrays.asList(values));

        for (String val : vals) {
          storedVals.add(val);
        }
        doc.removeField(facetHandler.getName());

        for (String val : storedVals) {
          doc.add(new StringField(facetHandler.getName(), val, Field.Store.NO));
        }
      }
    }
  }

  public String[] getStoredFieldValue(int docid, final String fieldname) throws IOException {
    DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor(fieldname);
    super.document(docid, visitor);
    Document doc = visitor.getDocument();
    return doc.getValues(fieldname);
  }

  /**
   * Work area for loading
   */
  public static class WorkArea {
    HashMap<Class<?>, Object> map = new HashMap<Class<?>, Object>();

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> cls) {
      T obj = (T) map.get(cls);
      return obj;
    }

    public void put(Object obj) {
      map.put(obj.getClass(), obj);
    }

    public void clear() {
      map.clear();
    }

    @Override
    public String toString() {
      return map.toString();
    }
  }

  private BoboSegmentReader(AtomicReader in) {
    super(in);
  }

  public BoboSegmentReader copy(AtomicReader in) {
    BoboSegmentReader copy = new BoboSegmentReader(in);
    copy._facetHandlerMap = this._facetHandlerMap;
    copy._facetHandlers = this._facetHandlers;
    copy._runtimeFacetHandlerFactories = this._runtimeFacetHandlerFactories;
    copy._runtimeFacetHandlerFactoryMap = this._runtimeFacetHandlerFactoryMap;
    copy._workArea = this._workArea;
    copy._facetDataMap.putAll(this._facetDataMap);
    return copy;
  }

  public AtomicReader getInnerReader() {
    return in;
  }

}
