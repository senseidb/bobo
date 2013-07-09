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

package com.browseengine.bobo.index;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import com.browseengine.bobo.index.digest.DataDigester;

public class BoboIndexer {
  private final Directory _index;
  private final DataDigester _digester;
  private IndexWriter _writer;
  private Analyzer _analyzer;

  private static class MyDataHandler implements DataDigester.DataHandler {
    private final IndexWriter _writer;

    MyDataHandler(IndexWriter writer) {
      _writer = writer;
    }

    @Override
    public void handleDocument(Document doc) throws IOException {
      _writer.addDocument(doc);
    }
  }

  public void setAnalyzer(Analyzer analyzer) {
    _analyzer = analyzer;
  }

  private Analyzer getAnalyzer() {
    return _analyzer == null ? new StandardAnalyzer(Version.LUCENE_43) : _analyzer;
  }

  public BoboIndexer(DataDigester digester, Directory index) {
    super();
    _index = index;
    _digester = digester;
  }

  public void index() throws IOException {
    _writer = null;
    try {
      IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, getAnalyzer());
      _writer = new IndexWriter(_index, config);
      MyDataHandler handler = new MyDataHandler(_writer);
      _digester.digest(handler);
      _writer.forceMerge(1);
    } finally {
      if (_writer != null) {
        _writer.close();
      }
    }
  }
}
