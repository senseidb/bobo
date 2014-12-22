package com.browseengine.bobo.sort;

import junit.framework.TestCase;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.SortField;

/**
 * @author mdelapenya
 */
public class SortCollectorTest extends TestCase {

    public void testBuildSortFieldFromQuery() {
        assertEquals(SortCollector.buildSortFieldFromQuery(null), SortField.FIELD_DOC);
        assertEquals(SortCollector.buildSortFieldFromQuery(new MatchAllDocsQuery()), SortField.FIELD_DOC);
        assertEquals(SortCollector.buildSortFieldFromQuery(new BooleanQuery()), SortField.FIELD_SCORE);
    }
}
