/**
 * 
 */
package com.browseengine.bobo.test;

import junit.framework.TestCase;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.Version;

import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.protobuf.BrowseProtobufConverter;
import com.browseengine.bobo.protobuf.BrowseRequestBPO;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;


/**
 * @author java6
 *
 */
public class ProtoBufConvertTest extends TestCase {
	public void testConvertMatchAllDocsQuery() throws ParseException {
		doConvert(new MatchAllDocsQuery());
	}

	public void testTermQuery() throws ParseException {
		doConvert(new TermQuery(new Term("tags", "hybrid")));
	}

	public void testFuzzyQuery() throws ParseException {
		doConvert(new FuzzyQuery(new Term("tags", "hybrid")));
	}
	
	public void testWildcardQuery() throws ParseException {
		doConvert(new WildcardQuery(new Term("*", "m**")));
	}

	public void testPrefixQuery() throws ParseException {
		doConvert(new PrefixQuery(new Term("tags", "h")));
	}

	public void testBooleanQuery() throws ParseException {
		BooleanQuery bq = new BooleanQuery();
		Query q1 = new TermQuery(new Term("tags", "h*"));
		BooleanClause bc = new BooleanClause(q1, BooleanClause.Occur.MUST);
		bq.add(bc);
		doConvert(bq);
	}

	public void testPhraseQuery() throws ParseException {
		PhraseQuery pq = new PhraseQuery();
		pq.add(new Term("tags", "h*"));
		pq.add(new Term("tags", "x*"));
		doConvert(pq);
	}

	public void testRangeQuery() throws ParseException {
		doConvert(new TermRangeQuery("tags", "a", "x", false, false));
		doConvert(new TermRangeQuery("tags", "a", "x", true, true));
	}

	public void doConvert(Query query) throws ParseException {
		BrowseRequest boboReqBefore = new BrowseRequest();
		boboReqBefore.setQuery(query);
		BrowseRequestBPO.Request req = (BrowseRequestBPO.Request) BrowseProtobufConverter.convert(boboReqBefore);
//		System.out.println("request after conversion to msg:\n" + req.toString());
		String reqString = TextFormat.printToString(req);
		reqString = reqString.replace('\r', ' ').replace('\n', ' ');
//		System.out.println(reqString);
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		QueryParser _qparser = new QueryParser(Version.LUCENE_CURRENT,"", analyzer);
//		System.out.println("msg to req");
		BrowseRequest boboReqAfter = BrowseProtobufConverter.convert(req, _qparser);
//		System.out.println("get query 2: "
//				+ (boboReqAfter.getQuery() != null ? boboReqAfter.getQuery().getClass()
//						: "null query"));
//		System.out.println("----------");
		assertEquals(query.getClass(), boboReqAfter.getQuery().getClass());
	}
}
