package org.apache.lucene.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.index.IndexReader.FieldOption;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.LockObtainFailedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinate;
import com.browseengine.bobo.geosearch.index.impl.GeoIndexReader;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;
import com.browseengine.bobo.geosearch.query.GeoQuery;

/**
 * Class to run GeoSearch Indexing functional tests.  These tests run 
 * above Lucene and use a RAMDirectory so that they still run quickly
 * 
 * @author Geoff Cooney
 *
 */
@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration( { "/TEST-servlet.xml" }) 
@IfProfileValue(name = "test-suite", values = { "unit", "functional", "all" })
public class GeoSearchMergingFunctionalTest extends GeoSearchFunctionalTezt {
    int numberOfSegments;
    
    @Before
    public void setUp() throws CorruptIndexException, LockObtainFailedException, IOException {
        numberOfSegments = 10;
        
        buildGeoIndexWriter(); 
        for (int i = 0; i < numberOfSegments; i++) { 
            addDocuments();
            //force a commit and thus a new segment
            writer.commit();  
        }
        
        //now merge
        writer.optimize();
    }
    
    @After
    public void tearDown() throws CorruptIndexException, IOException {
        if (writer != null) {
            writer.close();
        }
    }
    
    @Test
    public void testMergeHappened() {
        assertEquals(1, writer.getSegmentCount());
    }
    
    @Test
    public void testConfirmMergedGeoFile() throws IOException {
        int maxDocs = numberOfSegments * titles.length;
        verifySegment(maxDocs);
    }
    
    private void verifySegment(int maxDocs) throws IOException {
        String geoFileName = getMergedGeoFileName();
        assertTrue(directory.fileExists(geoFileName));
        
        GeoSegmentReader<GeoRecord> reader = new GeoSegmentReader<GeoRecord>(directory, 
                geoFileName, maxDocs, 1024, geoRecordSerializer, geoComparator);
        
        assertEquals(maxDocs * 2, reader.getArrayLength());
        
        GeoRecord previousRecord = GeoRecord.MIN_VALID_GEORECORD;
        Iterator<GeoRecord> geoIter = reader.getIterator(GeoRecord.MIN_VALID_GEORECORD, GeoRecord.MAX_VALID_GEORECORD);
        while (geoIter.hasNext()) {
            GeoRecord currentRecord = geoIter.next();
            System.out.println(currentRecord);
            assertTrue(geoComparator.compare(currentRecord, previousRecord) >= 0);
            previousRecord = currentRecord;
        }
    }
    
    private String getMergedGeoFileName() {
        SegmentInfos segmentInfos = writer.segmentInfos;

        assertEquals(1, segmentInfos.size());
        
        SegmentInfo segmentInfo = segmentInfos.info(0);
        
        String geoFileName = geoConfig.getGeoFileName(segmentInfo.name);
        
        return geoFileName;
    }
    
    @Test
    public void testFilter() throws IOException {
        int maxDocs = numberOfSegments * titles.length;
        String geoFileName = getMergedGeoFileName();
        
        verifyFilter(geoFileName, maxDocs);
    }
    
    @Test
    /**
     * Verified that the non-geo fields are passed to lucene and that the geo fields
     * are not
     */
    public void testLuceneFieldNames() throws CorruptIndexException, IOException {
        IndexSearcher searcher = new IndexSearcher(directory);
        Collection<String> fieldNames = searcher.getIndexReader().getFieldNames(FieldOption.ALL);
        
        assertEquals("Expected exactly 2 fieldNames, got: " + fieldNames.toString(), 2, fieldNames.size());
        assertTrue("Expected text to be a lucene field", fieldNames.contains("text"));
        assertTrue("Expected title to be a lucene field", fieldNames.contains("title"));
    }
    
    @Test
    public void testFreeTextSearch() throws IOException {
        IndexSearcher searcher = new IndexSearcher(directory);
        Term term = new Term("text", "man");
        TermQuery query = new TermQuery(term);
        
        TopDocs topDocs = searcher.search(query, 50);
        
        List<String> expectedResults = new Vector<String>();
        for (int i = 0; i < numberOfSegments; i++) {
            expectedResults.add(titles[8]);
        }
        for (int i = 0; i < numberOfSegments; i++) {
            expectedResults.add(titles[1]);
        }
        
        verifyExpectedResults(expectedResults, topDocs, searcher);
    }
    
    @Test
    public void testOldIndicesDeleted() throws IOException {
        writer.close();
        
        assertEquals(1, countExtensions(directory, "cfs"));
        assertEquals(1, countExtensions(directory, "geo"));
    }
    
    @Test
    public void testDeleteByTitle1() throws CorruptIndexException, IOException {
        Term deleteTerm = new Term(TITLE_FIELD, "pride");
        writer.deleteDocuments(deleteTerm);
        writer.commit();
        
        writer.optimize();
        
        int maxDocs = numberOfSegments * titles.length - numberOfSegments;
        verifySegment(maxDocs);
        
        IndexSearcher searcher = new IndexSearcher(directory);
        Term term = new Term(TEXT_FIELD, "man");
        TermQuery query = new TermQuery(term);
        
        TopDocs topDocs = searcher.search(query, 50);
        
        List<String> expectedResults = new Vector<String>();
        for (int i = 0; i < numberOfSegments; i++) {
            expectedResults.add(titles[8]);
        }
        
        verifyExpectedResults(expectedResults, topDocs, searcher);
    }
    
    @Test
    public void testGeoSearch() throws IOException {
        GeoIndexReader reader = new GeoIndexReader(directory, geoConfig);
        IndexSearcher searcher = new IndexSearcher(reader);
        GeoCoordinate coordinate = calculateGeoCoordinate(3, 21);
        double longitude = coordinate.getLongitude();
        double lattitude = coordinate.getLatitude();
        float miles = 500;
         
        GeoQuery query = new GeoQuery(longitude, lattitude, miles, null);
        TopDocs topDocs = searcher.search(query, 10);
        
        List<String> expectedResults = new Vector<String>();
        for (int i = 0; i < numberOfSegments; i++) {
            expectedResults.add(titles[1]);
        }
        verifyExpectedResults(expectedResults, topDocs, searcher);
    }
}
