package com.browseengine.bobo.test;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.facets.FacetHandler;

public class IndexReaderTestCase extends TestCase {

	private List<FacetHandler<?>> _fconf;
	public IndexReaderTestCase() {
	}

	public IndexReaderTestCase(String name) {
		super(name);
	}
	
	public void testIndexReload()
	{
		try{
			RAMDirectory idxDir=new RAMDirectory();
            Document[] docs=BoboTestCase.buildData();
            BoboIndexReader.WorkArea workArea = new BoboIndexReader.WorkArea();
            BrowseRequest req;
            BrowseSelection sel;
            BoboBrowser browser;
            BrowseResult result; 
            
            IndexWriter writer=new IndexWriter(idxDir,new StandardAnalyzer(Version.LUCENE_CURRENT),MaxFieldLength.UNLIMITED);
            writer.close();

            int dup = 0;
            for(int j = 0; j < 50; j++)
            {
              IndexReader idxReader = IndexReader.open(idxDir,true);
              BoboIndexReader reader = BoboIndexReader.getInstance(idxReader,_fconf,workArea);
              
              req = new BrowseRequest();
              sel = new BrowseSelection("color");
              sel.addValue("red");
              req.addSelection(sel);
              browser = new BoboBrowser(reader);
              result = browser.browse(req);
			
              assertEquals(3*dup, result.getNumHits());
			
              req = new BrowseRequest();
              sel = new BrowseSelection("tag");
              sel.addValue("dog");
              req.addSelection(sel);
              browser = new BoboBrowser(reader);
              result = browser.browse(req);
              
              assertEquals(2*dup, result.getNumHits());
              
              req = new BrowseRequest();
              sel = new BrowseSelection("tag");
              sel.addValue("funny");
              req.addSelection(sel);
              browser = new BoboBrowser(reader);
              result = browser.browse(req);
              
              assertEquals(3*dup, result.getNumHits());

              writer=new IndexWriter(idxDir,new StandardAnalyzer(Version.LUCENE_CURRENT),MaxFieldLength.UNLIMITED);
              for(int k = 0; k <= j; k++)
              {
                for(int i = 0; i < docs.length ; i++)
                {
                  writer.addDocument(docs[i]);
                }
                dup++;
              }
              writer.close();
            }
			idxDir.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();	
		_fconf=BoboTestCase.buildFieldConf();
	}
	
	private static byte[] lookup=new byte[2500000];
	private static byte[] lookup2=new byte[5000000];
	
	public static void test3()
	{
		Arrays.fill(lookup2, (byte)0x0);
		
		for (int i=0;i<250;i+=3)
		{
			lookup2[i]|=(0x1);
		}
		for (int i=1;i<10000;i+=3)
		{
			lookup2[i]|=(0x2);
		}
		
		long start=System.currentTimeMillis();
		for (int i=0;i<5000000;++i)
		{
			int degree;
			if ((lookup2[i] & (0x1)) == 0)
			{
				degree=1;
			}
			else if ((lookup2[i] & (0x2)) == 0)
			{
				degree=2;
			}
			else
			{
				degree=3;
			}
		}

		long end=System.currentTimeMillis();
		System.out.println("test 3 took: "+(end-start));
	}
	
	public static void test2()
	{
		Arrays.fill(lookup, (byte)0x0);
		
		for (int i=0;i<250;i+=3)
		{
			lookup[i>>1]|=(0x1)<<((i&0x1)<<2);
		}
		for (int i=1;i<10000;i+=3)
		{
			lookup[i>>1]|=(0x2)<<((i&0x1)<<2);
		}
		
		long start=System.currentTimeMillis();
		for (int i=0;i<5000000;++i)
		{
			int degree;
			if ((lookup[i>>1] & (0x1)<<((i&0x1)<<2)) == 0)
			{
				degree=1;
			}
			else if ((lookup[i>>1] & (0x2)<<((i&0x1)<<2)) == 0)
			{
				degree=2;
			}
			else
			{
				degree=3;
			}
		}

		long end=System.currentTimeMillis();
		System.out.println("test 2 took: "+(end-start));
	}
	
	public static void test1()
	{
		IntSet firstDegree=new IntOpenHashSet();
		IntSet secondDegree=new IntOpenHashSet();
		
		for (int i=0;i<250;i+=3)
		{
			firstDegree.add(i);
		}
		for (int i=1;i<10000;i+=3)
		{
			secondDegree.add(i);
		}
		
		long start=System.currentTimeMillis();
		for (int i=0;i<5000000;++i)
		{
			int degree;
			if (secondDegree.contains(i))
			{
				degree=2;
			}
			else if (firstDegree.contains(i))
			{
				degree=1;
			}
			else
			{
				degree=3;
			}
		}

		long end=System.currentTimeMillis();
		System.out.println("test 1 took: "+(end-start));
	}
	
	public void testFastMatchAllDocs() throws Exception{
		  File idxFile = new File("/Users/jwang/dataset/idx");
		  Directory idxDir = FSDirectory.getDirectory(idxFile);
		
	      BoboIndexReader reader = BoboIndexReader.getInstance(IndexReader.open(idxDir));
	      IndexSearcher searcher = new IndexSearcher(reader);
	      
	      //Query q = reader.getFastMatchAllDocsQuery();
	      //Query q = new MatchAllDocsQuery();
	      
	      QueryParser qp = new QueryParser("contents",new StandardAnalyzer());
	      Query q = qp.parse("*:*");
	      TopDocs topDocs = searcher.search(q, 100);
	      assertEquals(reader.numDocs(), topDocs.totalHits);
	      reader.close();
		}

	public static void main(String[] args) throws Exception{
		IndexReaderTestCase test = new IndexReaderTestCase();
		test.testFastMatchAllDocs();
	}
	public static void main2(String[] args) {
		for (int i=0;i<20;i++)
		{
			test1();
			test2();
			test3();
		}
	}
	 /*public static void main(String[] args) throws IOException{
			File idx=new File("/Users/jwang/projects/bobo_memory_opt/bobo/cardata/cartag");
			Directory dir=FSDirectory.getDirectory(idx);
			BoboIndexReader reader=new BoboIndexReader(IndexReader.open(dir));
			reader.close();
		  }*/
}
