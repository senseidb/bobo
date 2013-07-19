package com.browseengine.bobo.test;

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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboMultiReader;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.impl.RangeFacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;
import com.browseengine.bobo.index.BoboIndexer;
import com.browseengine.bobo.index.digest.DataDigester;

public class FacetNotValuesTest extends TestCase {
  static Logger log = Logger.getLogger(FacetNotValuesTest.class);
  private final List<FacetHandler<?>> _facetHandlers;
  private final int _documentSize;
  static private String[] _idRanges = new String[] { "[10 TO 10]" };

  private static class TestDataDigester extends DataDigester {
    private final Document[] _data;

    TestDataDigester(List<FacetHandler<?>> facetHandlers, Document[] data) {
      super();
      _data = data;
    }

    @Override
    public void digest(DataHandler handler) throws IOException {
      for (int i = 0; i < _data.length; ++i) {
        handler.handleDocument(_data[i]);
      }
    }
  }

  public FacetNotValuesTest(String testName) {
    super(testName);
    _facetHandlers = createFacetHandlers();
    _documentSize = 2;
    String confdir = System.getProperty("conf.dir");
    if (confdir == null) confdir = "./resource";
    System.setProperty("log.home", ".");
    org.apache.log4j.PropertyConfigurator.configure(confdir + "/log4j.properties");
  }

  public Document[] createDataTwo() {
    ArrayList<Document> dataList = new ArrayList<Document>();
    String color = "red";
    String ID = Integer.toString(10);
    Document d = new Document();
    d.add(new StringField("id", ID, Field.Store.YES));
    d.add(new StringField("color", color, Field.Store.YES));
    d.add(new IntField("NUM", 10, Field.Store.YES));
    dataList.add(d);

    color = "green";
    ID = Integer.toString(11);
    d = new Document();
    d.add(new StringField("id", ID, Field.Store.YES));
    d.add(new StringField("color", color, Field.Store.YES));
    d.add(new IntField("NUM", 11, Field.Store.YES));
    dataList.add(d);

    return dataList.toArray(new Document[dataList.size()]);
  }

  public Document[] createData() {
    ArrayList<Document> dataList = new ArrayList<Document>();
    for (int i = 0; i < _documentSize; i++) {
      String color = (i % 2 == 0) ? "red" : "green";
      String ID = Integer.toString(i);
      Document d = new Document();
      d.add(new StringField("id", ID, Field.Store.YES));
      d.add(new StringField("color", color, Field.Store.YES));
      dataList.add(d);
    }

    return dataList.toArray(new Document[dataList.size()]);
  }

  private Directory createIndexTwo() {
    Directory dir = new RAMDirectory();
    try {
      Document[] data = createDataTwo();
      TestDataDigester testDigester = new TestDataDigester(_facetHandlers, data);
      BoboIndexer indexer = new BoboIndexer(testDigester, dir);
      indexer.index();
      DirectoryReader r = DirectoryReader.open(dir);
      r.close();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return dir;
  }

  private Directory createIndex() {
    Directory dir = new RAMDirectory();
    try {
      Document[] data = createData();

      TestDataDigester testDigester = new TestDataDigester(_facetHandlers, data);
      BoboIndexer indexer = new BoboIndexer(testDigester, dir);
      indexer.index();
      DirectoryReader r = DirectoryReader.open(dir);
      r.close();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return dir;
  }

  public static List<FacetHandler<?>> createFacetHandlers() {
    List<FacetHandler<?>> facetHandlers = new ArrayList<FacetHandler<?>>();
    facetHandlers.add(new SimpleFacetHandler("id"));
    facetHandlers.add(new SimpleFacetHandler("color"));
    FacetHandler<?> rangeFacetHandler = new RangeFacetHandler("idRange", "id", null);
    facetHandlers.add(rangeFacetHandler);

    return facetHandlers;
  }

  public void testNotValuesForSimpleFacetHandler() throws Exception {
    BrowseRequest br = new BrowseRequest();
    br.setCount(20);
    br.setOffset(0);

    BrowseSelection colorSel = new BrowseSelection("color");
    colorSel.addValue("red");
    br.addSelection(colorSel);

    BrowseSelection idSel = new BrowseSelection("id");
    idSel.addNotValue("0");
    br.addSelection(idSel);

    BrowseResult result = null;
    BoboBrowser boboBrowser = null;
    int expectedHitNum = (_documentSize / 2) - 1;
    try {
      Directory ramIndexDir = createIndex();
      DirectoryReader srcReader = DirectoryReader.open(ramIndexDir);
      boboBrowser = new BoboBrowser(BoboMultiReader.getInstance(srcReader, _facetHandlers));
      result = boboBrowser.browse(br);

      assertEquals(expectedHitNum, result.getNumHits());

      StringBuilder buffer = new StringBuilder();
      BrowseHit[] hits = result.getHits();

      for (int i = 0; i < hits.length; ++i) {
        int expectedID = (i + 1) * 2;
        assertEquals(expectedID, Integer.parseInt(hits[i].getField("id")));
        if (i != 0) {
          buffer.append('\n');
        }
        buffer.append("id=" + hits[i].getField("id") + "," + "color=" + hits[i].getField("color"));
      }
      log.info(buffer.toString());

    } catch (BrowseException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (IOException ioe) {
      fail(ioe.getMessage());
    } finally {
      if (boboBrowser != null) {
        try {
          if (result != null) result.close();
          boboBrowser.close();
        } catch (IOException e) {
          fail(e.getMessage());
        }
      }
    }

  }

  public void testNotValuesForRangeFacetHandler() throws Exception {
    System.out.println("testNotValuesForRangeFacetHandler");
    BrowseResult result = null;
    BoboBrowser boboBrowser = null;

    try {
      Directory ramIndexDir = createIndexTwo();
      DirectoryReader srcReader = DirectoryReader.open(ramIndexDir);
      boboBrowser = new BoboBrowser(BoboMultiReader.getInstance(srcReader, _facetHandlers));

      BrowseRequest br = new BrowseRequest();
      br.setCount(20);
      br.setOffset(0);

      if (_idRanges == null) {
        log.error("_idRanges cannot be null in order to test NOT on RangeFacetHandler");
      }
      BrowseSelection idSel = new BrowseSelection("idRange");
      idSel.addNotValue(_idRanges[0]);
      int expectedHitNum = 1;
      br.addSelection(idSel);
      BooleanQuery q = new BooleanQuery();
      q.add(NumericRangeQuery.newIntRange("NUM", 10, 10, true, true), Occur.MUST_NOT);
      q.add(new MatchAllDocsQuery(), Occur.MUST);
      br.setQuery(q);

      result = boboBrowser.browse(br);

      assertEquals(expectedHitNum, result.getNumHits());
      for (int i = 0; i < result.getNumHits(); i++) {
        System.out.println(result.getHits()[i]);
      }

    } catch (BrowseException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (IOException ioe) {
      fail(ioe.getMessage());
    } finally {
      if (boboBrowser != null) {
        try {
          if (result != null) result.close();
          boboBrowser.close();
        } catch (IOException e) {
          fail(e.getMessage());
        }
      }
    }

  }
}
