package com.browseengine.bobo.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboMultiReader;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;

public class BasicIndexingTest {
  private static final String BLUE = "blue";
  private static final String RED = "red";

  private IndexWriter m_indexWriter;

  public BasicIndexingTest() {
    // TODO Auto-generated constructor stub
  }

  @Before
  public void setUp() throws Exception {
    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, new StandardAnalyzer(
        Version.LUCENE_43));
    config.setMaxBufferedDocs(1000);
    m_indexWriter = new IndexWriter(new RAMDirectory(), config);
  }

  @After
  public void tearDown() throws Exception {
    m_indexWriter.close();
  }

  @Test
  public void testWithInterleavedCommitsUsingBobo() throws Exception {
    String text = "text";

    Document doc1 = new Document();
    doc1.add(new TextField(text, "Foo1", Store.YES));
    m_indexWriter.addDocument(doc1);
    m_indexWriter.commit();

    Document doc2 = new Document();
    doc2.add(new TextField(text, "Foo2", Store.YES));
    m_indexWriter.addDocument(doc2);
    m_indexWriter.commit();

    Document doc3 = new Document();
    doc3.add(new TextField(text, "Foo3", Store.YES));
    m_indexWriter.addDocument(doc3);
    m_indexWriter.commit();

    List<FacetHandler<?>> handlerList = Arrays.asList(new FacetHandler<?>[] {});

    DirectoryReader reader = BoboMultiReader.open(m_indexWriter, true);

    BoboMultiReader boboMultiReader = BoboMultiReader.getInstance(reader, handlerList);

    BrowseRequest br = new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);

    QueryParser parser = new QueryParser(Version.LUCENE_43, "text", new StandardAnalyzer(
        Version.LUCENE_43));
    Query q = parser.parse("Foo*");
    br.setQuery(q);

    BoboBrowser browser = new BoboBrowser(boboMultiReader);
    BrowseResult result = browser.browse(br);

    int totalHits = result.getNumHits();
    BrowseHit[] hits = result.getHits();

    assertEquals("should be 3 hits", 3, totalHits);
    assertEquals("should be doc 0", 0, hits[0].getDocid());
    assertEquals("should be doc 1", 1, hits[1].getDocid());
    assertEquals("should be doc 2", 2, hits[2].getDocid());

    result.close();
  }

  @Test
  public void testWithSingleCommit() throws Exception {
    String text = "text";

    Document doc1 = new Document();
    doc1.add(new TextField(text, "Foo1", Store.YES));
    m_indexWriter.addDocument(doc1);

    Document doc2 = new Document();
    doc2.add(new TextField(text, "Foo2", Store.YES));
    m_indexWriter.addDocument(doc2);

    Document doc3 = new Document();
    doc3.add(new TextField(text, "Foo3", Store.YES));
    m_indexWriter.addDocument(doc3);

    m_indexWriter.commit();

    List<FacetHandler<?>> handlerList = Arrays.asList(new FacetHandler<?>[] {});

    DirectoryReader reader = BoboMultiReader.open(m_indexWriter, true);
    BoboMultiReader boboMultiReader = BoboMultiReader.getInstance(reader, handlerList);

    BrowseRequest br = new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);

    QueryParser parser = new QueryParser(Version.LUCENE_43, "text", new StandardAnalyzer(
        Version.LUCENE_43));
    Query q = parser.parse("Foo*");
    br.setQuery(q);

    BoboBrowser browser = new BoboBrowser(boboMultiReader);
    BrowseResult result = browser.browse(br);

    int totalHits = result.getNumHits();
    BrowseHit[] hits = result.getHits();

    assertEquals("should be 3 hits", 3, totalHits);
    assertEquals("should be doc 0", 0, hits[0].getDocid());
    assertEquals("should be doc 1", 1, hits[1].getDocid());
    assertEquals("should be doc 2", 2, hits[2].getDocid());

    result.close();
  }

  @Test
  public void testWithInterleavedCommitsUsingLuceneQuery() throws Exception {
    String text = "text";

    Document doc1 = new Document();
    doc1.add(new TextField(text, "Foo1", Store.YES));
    m_indexWriter.addDocument(doc1);
    m_indexWriter.commit();

    Document doc2 = new Document();
    doc2.add(new TextField(text, "Foo2", Store.YES));
    m_indexWriter.addDocument(doc2);
    m_indexWriter.commit();

    Document doc3 = new Document();
    doc3.add(new TextField(text, "Foo3", Store.YES));
    m_indexWriter.addDocument(doc3);
    m_indexWriter.commit();

    DirectoryReader reader = DirectoryReader.open(m_indexWriter, true);
    IndexSearcher searcher = new IndexSearcher(reader);
    TopScoreDocCollector docCollector = TopScoreDocCollector.create(100, true);
    QueryParser queryParser = new QueryParser(Version.LUCENE_43, "text", new StandardAnalyzer(
        Version.LUCENE_43));
    Query query = queryParser.parse("Foo*");
    searcher.search(query, docCollector);
    TopDocs docs = docCollector.topDocs();
    ScoreDoc[] scoreDocs = docs.scoreDocs;

    assertEquals("should be doc 0", 0, scoreDocs[0].doc);
    assertEquals("should be doc 1", 1, scoreDocs[1].doc);
    assertEquals("should be doc 2", 2, scoreDocs[2].doc);

    reader.close();
  }

  @Test
  public void testWithInterleavedCommitsUsingBoboWithFacets() throws Exception {
    String text = "text";
    String color = "color";

    Document doc1 = new Document();
    doc1.add(new TextField(text, "Foo1", Store.YES));
    doc1.add(new StringField(color, RED, Store.YES));
    m_indexWriter.addDocument(doc1);
    m_indexWriter.commit();

    Document doc2 = new Document();
    doc2.add(new TextField(text, "Foo2", Store.YES));
    doc2.add(new StringField(color, RED, Store.YES));
    m_indexWriter.addDocument(doc2);
    m_indexWriter.commit();

    Document doc3 = new Document();
    doc3.add(new TextField(text, "Foo3", Store.YES));
    doc3.add(new StringField(color, BLUE, Store.YES));
    m_indexWriter.addDocument(doc3);
    m_indexWriter.commit();

    SimpleFacetHandler colorHandler = new SimpleFacetHandler(color);

    List<FacetHandler<?>> handlerList = Arrays.asList(new FacetHandler<?>[] { colorHandler });

    DirectoryReader reader = DirectoryReader.open(m_indexWriter, true);

    BoboMultiReader boboMultiReader = BoboMultiReader.getInstance(reader, handlerList);

    BrowseRequest br = new BrowseRequest();
    br.setCount(10);
    br.setOffset(0);

    BrowseSelection red = new BrowseSelection(color);
    red.addValue(RED);
    red.addValue(BLUE);
    br.addSelection(red);

    QueryParser parser = new QueryParser(Version.LUCENE_43, "text", new StandardAnalyzer(
        Version.LUCENE_43));
    Query q = parser.parse("Foo*");
    br.setQuery(q);

    BoboBrowser browser = new BoboBrowser(boboMultiReader);
    BrowseResult result = browser.browse(br);

    int totalHits = result.getNumHits();
    BrowseHit[] hits = result.getHits();

    assertEquals("should be 3 hits", 3, totalHits);
    assertEquals("should be doc 0", 0, hits[0].getDocid());
    assertEquals("should be doc 1", 1, hits[1].getDocid());
    assertEquals("should be doc 2", 2, hits[2].getDocid());

    result.close();
  }
}
