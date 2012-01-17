/**
 * 
 */
package com.browseengine.bobo.test.section;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;

import com.browseengine.bobo.analysis.section.IntMetaDataTokenStream;
import com.browseengine.bobo.analysis.section.SectionTokenStream;
import com.browseengine.bobo.search.section.IntMetaDataQuery;
import com.browseengine.bobo.search.section.SectionSearchQuery;
import com.browseengine.bobo.util.test.IndexReaderWithMetaDataCache;

/**
 *
 */
public class TestSectionSearch extends TestCase
{
  private final static Term intMetaTerm = new Term("metafield", "intmeta");
  private RAMDirectory directory;
  private Analyzer analyzer;
  private IndexWriter writer;
  private IndexSearcher searcher;
  private IndexSearcher searcherWithCache;

  //@Override
  protected void setUp() throws Exception {
    directory = new RAMDirectory();
    analyzer = new WhitespaceAnalyzer();
    writer = new IndexWriter(directory, analyzer, true, MaxFieldLength.UNLIMITED);
    addDoc("1", new String[]{ "aa","bb"}, new String[]{"aaa","aaa"}, new int[]{100,200});
    addDoc("2", new String[]{ "aa","bb"}, new String[]{"aaa","bbb"}, new int[]{200,200});
    addDoc("3", new String[]{ "aa","bb"}, new String[]{"bbb","aaa"}, new int[]{300,300});
    addDoc("3", new String[]{ "bb","aa"}, new String[]{"bbb","bbb"}, new int[]{300,400});
    addDoc("3", new String[]{ "bb","aa"}, new String[]{"aaa","ccc"}, new int[]{300,500});
    writer.commit();
    IndexReader reader = IndexReader.open(directory, true);
    searcher = new IndexSearcher(reader);
    IndexReader readerWithCache = new IndexReaderWithMetaDataCache(reader);
    searcherWithCache = new IndexSearcher(readerWithCache);
  }
  
  //@Override
  protected void tearDown() throws IOException {
    searcher.close();
    writer.close();
    directory.close();
    analyzer = null;
  }

  private void addDoc(String key, String[] f1, String[] f2, int[] meta) throws IOException
  {
    Document doc = new Document();
    addStoredField(doc, "key", key);
    addTextField(doc, "f1", f1);
    addTextField(doc, "f2", f2);
    addMetaDataField(doc, intMetaTerm, meta);
    writer.addDocument(doc);
  }
  
  private void addStoredField(Document doc, String fieldName, String value)
  {
    Field field = new Field(fieldName, value, Store.YES, Index.NO);
    doc.add(field);
  }
  
  private void addTextField(Document doc, String fieldName, String[] sections)
  {
    for(int i = 0; i < sections.length; i++)
    {
      Field field = new Field(fieldName, new SectionTokenStream(analyzer.tokenStream(fieldName, new StringReader(sections[i])), i));
      doc.add(field);
    }
  }
  
  private void addMetaDataField(Document doc, Term term, int[] meta)
  {
    IntMetaDataTokenStream tokenStream = new IntMetaDataTokenStream(term.text());
    tokenStream.setMetaData(meta);
    Field field = new Field(term.field(), tokenStream);
    doc.add(field);
  }
  
  static int getNumHits(Query q,IndexSearcher searcher) throws Exception{
	  TopDocs hits = searcher.search(q, 10);
	  return hits.totalHits;
  }
  
  public void testSimpleSearch() throws Exception
  {
    BooleanQuery bquery;
    SectionSearchQuery squery;
    int count;
    
    // 1. (+f1:aa +f2:aaa)
    bquery = new BooleanQuery();
    bquery.add(new TermQuery(new Term("f1","aa")), BooleanClause.Occur.MUST);
    bquery.add(new TermQuery(new Term("f2","aaa")), BooleanClause.Occur.MUST);

    count = getNumHits(bquery,searcher);
    assertEquals("non-section count mismatch", 4, count);
    
    squery = new SectionSearchQuery(bquery);
    count = getNumHits(squery,searcher);
    assertEquals("seciton count mismatch", 2, count);
    
    // 2. (+f1:bb + f2:aaa)
    bquery = new BooleanQuery();
    bquery.add(new TermQuery(new Term("f1","bb")), BooleanClause.Occur.MUST);
    bquery.add(new TermQuery(new Term("f2","aaa")), BooleanClause.Occur.MUST);

    count = getNumHits(bquery,searcher);
    assertEquals("non-section count mismatch", 4, count);
    
    squery = new SectionSearchQuery(bquery);
    count = getNumHits(squery,searcher);
    assertEquals("seciton count mismatch", 3, count);
    
    // 3. (+f1:aa +f2:bbb)
    bquery = new BooleanQuery();
    bquery.add(new TermQuery(new Term("f1","aa")), BooleanClause.Occur.MUST);
    bquery.add(new TermQuery(new Term("f2","bbb")), BooleanClause.Occur.MUST);

    count = getNumHits(bquery,searcher);
    assertEquals("non-section count mismatch", 3, count);
    
    squery = new SectionSearchQuery(bquery);
    count = getNumHits(squery,searcher);
    assertEquals("seciton count mismatch", 2, count);
    
    // 4. (+f1:aa +(f2:bbb f2:ccc))
    BooleanQuery bquery2 = new BooleanQuery();
    bquery2.add(new TermQuery(new Term("f2","bbb")), BooleanClause.Occur.SHOULD);
    bquery2.add(new TermQuery(new Term("f2","ccc")), BooleanClause.Occur.SHOULD);
    bquery = new BooleanQuery();
    bquery.add(new TermQuery(new Term("f1","aa")), BooleanClause.Occur.MUST);
    bquery.add(bquery2, BooleanClause.Occur.MUST);

    count = getNumHits(bquery,searcher);
    assertEquals("non-section count mismatch", 4, count);
    
    squery = new SectionSearchQuery(bquery);
    count = getNumHits(squery,searcher);
    assertEquals("section count mismatch", 3, count);
  }
  
  public void testMetaData() throws Exception
  {
    metaDataSearch(searcher);
  }

  public void testMetaDataWithCache() throws Exception
  {
    metaDataSearch(searcherWithCache);    
  }
  
  private void metaDataSearch(IndexSearcher searcher) throws Exception
  {
    IndexReader reader = searcher.getIndexReader();
    
    BooleanQuery bquery;
    SectionSearchQuery squery;
    Scorer scorer;
    int count;
    
    // 1.
    bquery = new BooleanQuery();
    bquery.add(new TermQuery(new Term("f1","aa")), BooleanClause.Occur.MUST);
    bquery.add(new IntMetaDataQuery(intMetaTerm, new IntMetaDataQuery.SimpleValueValidator(100)), BooleanClause.Occur.MUST);
    squery = new SectionSearchQuery(bquery);
    scorer = squery.createWeight(searcher).scorer(reader, true, true);
    count = 0;
    while(scorer.nextDoc() != Scorer.NO_MORE_DOCS) count++;
    assertEquals("section count mismatch", 1, count);
    
    // 2.
    bquery = new BooleanQuery();
    bquery.add(new TermQuery(new Term("f1","aa")), BooleanClause.Occur.MUST);
    bquery.add(new IntMetaDataQuery(intMetaTerm, new IntMetaDataQuery.SimpleValueValidator(200)), BooleanClause.Occur.MUST);
    squery = new SectionSearchQuery(bquery);
    scorer = squery.createWeight(searcher).scorer(reader, true, true);
    count = 0;
    while(scorer.nextDoc() != Scorer.NO_MORE_DOCS) count++;
    assertEquals("section count mismatch", 1, count);
    
    // 3.
    bquery = new BooleanQuery();
    bquery.add(new TermQuery(new Term("f1","bb")), BooleanClause.Occur.MUST);
    bquery.add(new IntMetaDataQuery(intMetaTerm, new IntMetaDataQuery.SimpleValueValidator(200)), BooleanClause.Occur.MUST);
    squery = new SectionSearchQuery(bquery);
    scorer = squery.createWeight(searcher).scorer(reader, true, true);
    count = 0;
    while(scorer.nextDoc() != Scorer.NO_MORE_DOCS) count++;
    assertEquals("section count mismatch", 2, count);
    
    // 4.
    bquery = new BooleanQuery();
    bquery.add(new TermQuery(new Term("f1","aa")), BooleanClause.Occur.MUST);
    bquery.add(new IntMetaDataQuery(intMetaTerm, new IntMetaDataQuery.SimpleValueValidator(300)), BooleanClause.Occur.MUST);
    squery = new SectionSearchQuery(bquery);
    scorer = squery.createWeight(searcher).scorer(reader, true, true);
    count = 0;
    while(scorer.nextDoc() != Scorer.NO_MORE_DOCS) count++;
    assertEquals("section count mismatch", 1, count);
    
    // 5.
    bquery = new BooleanQuery();
    bquery.add(new TermQuery(new Term("f1","bb")), BooleanClause.Occur.MUST);
    bquery.add(new IntMetaDataQuery(intMetaTerm, new IntMetaDataQuery.SimpleValueValidator(300)), BooleanClause.Occur.MUST);
    squery = new SectionSearchQuery(bquery);
    scorer = squery.createWeight(searcher).scorer(reader, true, true);
    count = 0;
    while(scorer.nextDoc() != Scorer.NO_MORE_DOCS) count++;
    assertEquals("section count mismatch", 3, count);
  }
}
