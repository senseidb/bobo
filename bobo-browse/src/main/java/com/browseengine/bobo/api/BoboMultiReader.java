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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FilterDirectoryReader;

import com.browseengine.bobo.facets.FacetHandler;

public class BoboMultiReader extends FilterDirectoryReader {
  protected List<BoboSegmentReader> _subReaders = new ArrayList<BoboSegmentReader>();

  /**
   * Constructor
   *
   * @param reader
   *          Index reader
   * @throws IOException
   */
  public static BoboMultiReader getInstance(DirectoryReader reader) throws IOException {
    return BoboMultiReader.getInstance(reader, null);
  }

  public static BoboMultiReader getInstance(DirectoryReader reader,
      Collection<FacetHandler<?>> facetHandlers) throws IOException {
    BoboMultiReader boboReader = new BoboMultiReader(reader, facetHandlers);
    boboReader.facetInit();
    return boboReader;
  }

  @Override
  protected void doClose() throws IOException {
    // do nothing
  }

  /**
   * @param reader
   * @param facetHandlers
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  protected BoboMultiReader(DirectoryReader reader, Collection<FacetHandler<?>> facetHandlers)
      throws IOException {
    super(reader, new BoboSubReaderWrapper(facetHandlers));
    _subReaders = (List<BoboSegmentReader>) getSequentialSubReaders();
  }

  protected void facetInit() throws IOException {
    for (BoboSegmentReader r : _subReaders) {
      r.facetInit();
    }
  }

  public List<BoboSegmentReader> getSubReaders() {
    return _subReaders;
  }

  public final int subReaderBase(int readerIndex) {
    return readerBase(readerIndex);
  }

  public static class BoboSubReaderWrapper extends SubReaderWrapper {

    private final BoboSegmentReader.WorkArea workArea = new BoboSegmentReader.WorkArea();
    private Collection<FacetHandler<?>> _facetHandlers = null;

    /** Constructor */
    public BoboSubReaderWrapper(Collection<FacetHandler<?>> facetHandlers) {
      _facetHandlers = facetHandlers;
    }

    @Override
    public AtomicReader wrap(AtomicReader reader) {
      try {
        return new BoboSegmentReader(reader, _facetHandlers, null, workArea);
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) {
    return in;
  }
}
