package com.browseengine.bobo.test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandler.FacetDataNone;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.sort.DocComparatorSource;

public class FacetHandlerTest extends TestCase {
	private Directory _ramDir;
	private static class NoopFacetHandler extends FacetHandler<FacetDataNone>
	{

		public NoopFacetHandler(String name) {
			super(name);
		}

		public NoopFacetHandler(String name, Set<String> dependsOn) {
			super(name, dependsOn);
		}



		@Override
		public RandomAccessFilter buildRandomAccessFilter(String value,
				Properties selectionProperty) throws IOException {
			return null;
		}

		@Override
		public FacetCountCollectorSource getFacetCountCollectorSource(BrowseSelection sel,
				FacetSpec fspec) {
			return null;
		}

		@Override
		public String[] getFieldValues(BoboIndexReader reader,int id) {
			return null;
		}

		@Override
		public DocComparatorSource getDocComparatorSource() {
			return null;
		}

		@Override
		public FacetDataNone load(BoboIndexReader reader) throws IOException {
			return FacetDataNone.instance;
		}

		@Override
		public Object[] getRawFieldValues(BoboIndexReader reader,int id) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	public FacetHandlerTest(String testname)
	{
		super(testname);
		_ramDir = new RAMDirectory();
		try
		{
			IndexWriter writer = new IndexWriter(_ramDir,new StandardAnalyzer(Version.LUCENE_CURRENT),MaxFieldLength.UNLIMITED);
			writer.close();
		}
		catch(Exception ioe)
		{
			fail("unable to load test");
		}
	}
	
	public void testFacetHandlerLoad() throws Exception
	{
		IndexReader reader = IndexReader.open(_ramDir,true);
		
		List<FacetHandler<?>> list = new LinkedList<FacetHandler<?>>();
		NoopFacetHandler h1 = new NoopFacetHandler("A");
		list.add(h1);
		
		HashSet<String> s2 = new HashSet<String>();
		s2.add("A");
		s2.add("C");
		s2.add("D");
		NoopFacetHandler h2 = new NoopFacetHandler("B",s2);
		list.add(h2);
		
		HashSet<String> s3 = new HashSet<String>();
		s3.add("A");
		s2.add("D");
		NoopFacetHandler h3 = new NoopFacetHandler("C",s3);
		list.add(h3);
		
		HashSet<String> s4 = new HashSet<String>();
		s4.add("A");
		NoopFacetHandler h4 = new NoopFacetHandler("D",s4);
		list.add(h4);
		
		HashSet<String> s5 = new HashSet<String>();
		s5.add("E");
		NoopFacetHandler h5 = new NoopFacetHandler("E",s5);
		list.add(h5);
		
		
		BoboIndexReader boboReader = BoboIndexReader.getInstance(reader,list, null);
		
		BoboBrowser browser = new BoboBrowser(boboReader);
		HashSet<String> s6 = new HashSet<String>();
		s6.add("A");
		s6.add("B");
		s6.add("C");
		s6.add("D");
		browser.setFacetHandler(new NoopFacetHandler("runtime",s6));
		
		Set<String> expected = new  HashSet<String>();
		expected.add("A");
		expected.add("B");
		expected.add("C");
		expected.add("D");
		expected.add("E");
		expected.add("runtime");
		
		Set<String> facetsLoaded = browser.getFacetNames();
		
		Iterator<String> iter = facetsLoaded.iterator();
		while(iter.hasNext())
		{
			String name = iter.next();
			if (expected.contains(name))
			{
				expected.remove(name);
			}
			else
			{
				fail(name+" is not in expected set.");
			}
		}
		
		if (expected.size() > 0)
		{
			fail("some facets not loaded: "+expected);
		}
		
		boboReader.close();
		browser.close();
	}
	
	public void testNegativeLoadTest() throws Exception
	{
		IndexReader reader = IndexReader.open(_ramDir,true);
		
		List<FacetHandler<?>> list = new LinkedList<FacetHandler<?>>();
		HashSet<String> s1 = new HashSet<String>();
		s1.add("E");
		NoopFacetHandler h1 = new NoopFacetHandler("A",s1);
		list.add(h1);
		
		HashSet<String> s2 = new HashSet<String>();
		s2.add("A");
		s2.add("C");
		s2.add("D");
		NoopFacetHandler h2 = new NoopFacetHandler("B",s2);
		list.add(h2);
		
		HashSet<String> s3 = new HashSet<String>();
		s3.add("A");
		s2.add("D");
		NoopFacetHandler h3 = new NoopFacetHandler("C",s3);
		list.add(h3);
		
		HashSet<String> s4 = new HashSet<String>();
		s4.add("A");
		NoopFacetHandler h4 = new NoopFacetHandler("D",s4);
		list.add(h4);
		
		HashSet<String> s5 = new HashSet<String>();
		s5.add("E");
		NoopFacetHandler h5 = new NoopFacetHandler("E",s5);
		list.add(h5);
		
		
		BoboIndexReader boboReader = BoboIndexReader.getInstance(reader,list, null);
		
		BoboBrowser browser = new BoboBrowser(boboReader);
		
		
		Set<String> expected = new  HashSet<String>();
		expected.add("A");
		expected.add("B");
		expected.add("C");
		expected.add("D");
		expected.add("E");
		
		Set<String> facetsLoaded = browser.getFacetNames();
		
		Iterator<String> iter = facetsLoaded.iterator();
		while(iter.hasNext())
		{
			String name = iter.next();
			if (expected.contains(name))
			{
				expected.remove(name);
			}
			else
			{
				fail(name+" is not in expected set.");
			}
		}
		
		if (expected.size() > 0)
		{
			if (expected.size() == 4)
			{
				expected.remove("A");
				expected.remove("B");
				expected.remove("C");
				expected.remove("D");
				if (expected.size() > 0)
				{
					fail("some facets not loaded: "+expected);
				}
			}
			else
			{
			  fail("incorrect number of left over facets: "+expected);
			}
		}
		
		boboReader.close();
		browser.close();
	}
}
