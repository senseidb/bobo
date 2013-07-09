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

package com.browseengine.bobo.impl;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.browseengine.bobo.api.BoboMultiReader;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.service.BrowseService;
import com.browseengine.bobo.service.BrowseServiceFactory;

public class BrowseServiceImpl implements BrowseService {
  private static final Logger logger = Logger.getLogger(BrowseServiceImpl.class);
  private final File _idxDir;
  private BoboMultiReader _reader;

  public BrowseServiceImpl(File idxDir) {
    super();
    _idxDir = idxDir;
    try {
      _reader = newIndexReader();
    } catch (IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

  public BrowseServiceImpl() {
    this(new File(System.getProperty("index.directory")));
  }

  private BoboMultiReader newIndexReader() throws IOException {
    Directory idxDir = FSDirectory.open(_idxDir);
    return newIndexReader(idxDir);
  }

  public static BoboMultiReader newIndexReader(Directory idxDir) throws IOException {
    if (!DirectoryReader.indexExists(idxDir)) {
      return null;
    }

    long start = System.currentTimeMillis();

    DirectoryReader directoryReader = DirectoryReader.open(idxDir);
    BoboMultiReader reader;
    try {
      reader = BoboMultiReader.getInstance(directoryReader);
    } catch (IOException ioe) {
      throw ioe;
    } finally {
      directoryReader.close();
    }

    long end = System.currentTimeMillis();

    if (logger.isDebugEnabled()) {
      logger.debug("New index loading took: " + (end - start));
    }

    return reader;
  }

  @Override
  public synchronized void close() throws BrowseException {
    try {
      if (_reader != null) {
        _reader.close();
      }
    } catch (IOException e) {
      throw new BrowseException(e.getMessage(), e);
    }
  }

  @Override
  public BrowseResult browse(BrowseRequest req) throws BrowseException {
    return BrowseServiceFactory.createBrowseService(_reader).browse(req);
  }
}
