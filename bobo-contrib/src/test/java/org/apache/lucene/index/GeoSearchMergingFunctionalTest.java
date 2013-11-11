package org.apache.lucene.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.LockObtainFailedException;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinate;
import com.browseengine.bobo.geosearch.index.impl.GeoIndexReader;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;
import com.browseengine.bobo.geosearch.query.GeoQuery;
import com.browseengine.bobo.geosearch.score.impl.Conversions;

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
//TODO:  Add tests using CFS
public class GeoSearchMergingFunctionalTest extends GeoSearchFunctionalTezt {
    int numberOfSegments;
    
    public void setUpIndex(boolean useCompoundFileFormat) throws CorruptIndexException, LockObtainFailedException, IOException {
        numberOfSegments = 10;
        
        buildGeoIndexWriter(useCompoundFileFormat); 
        for (int i = 0; i < numberOfSegments; i++) { 
            addDocuments();
            //force a commit and thus a new segment
            writer.commit();  
        }
        
        //now merge
        writer.forceMerge(1);
    }
    
    @After
    public void tearDown() throws CorruptIndexException, IOException {
        if (writer != null) {
            writer.close();
        }
    }
    
    @Test
    public void testMergeHappened() throws IOException {
        setUpIndex(false);
        assertEquals(1, writer.getSegmentCount());
    }
    
    @Test
    public void testConfirmMergedGeoFile() throws IOException {
        setUpIndex(false);
        int maxDocs = numberOfSegments * titles.length;
        verifySegment(maxDocs);
    }
    
    private void verifySegment(int maxDocs) throws IOException {
        String geoFileName = getMergedGeoFileName();
        assertTrue(directory.fileExists(geoFileName));
        
        GeoSegmentReader<CartesianGeoRecord> reader = new GeoSegmentReader<CartesianGeoRecord>(directory, 
                geoFileName, maxDocs, IOContext.READ, geoRecordSerializer, geoComparator);
        
        assertEquals(maxDocs * 2, reader.getArrayLength());
        
        CartesianGeoRecord previousRecord = CartesianGeoRecord.MIN_VALID_GEORECORD;
        Iterator<CartesianGeoRecord> geoIter = reader.getIterator(CartesianGeoRecord.MIN_VALID_GEORECORD, CartesianGeoRecord.MAX_VALID_GEORECORD);
        while (geoIter.hasNext()) {
            CartesianGeoRecord currentRecord = geoIter.next();
            System.out.println(currentRecord);
            assertTrue(geoComparator.compare(currentRecord, previousRecord) >= 0);
            previousRecord = currentRecord;
        }
    }
    
    private String getMergedGeoFileName() {
        SegmentInfos segmentInfos = writer.segmentInfos;

        assertEquals(1, segmentInfos.size());
        
        SegmentInfo segmentInfo = segmentInfos.info(0).info;
        
        String geoFileName = geoConfig.getGeoFileName(segmentInfo.name);
        
        return geoFileName;
    }
    
    @Test
    public void testFilter() throws IOException {
        setUpIndex(false);
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
        setUpIndex(false);
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));
        Set<String> fieldNames = new HashSet<String>();
        for (AtomicReaderContext readerContext : searcher.getIndexReader().getContext().leaves()) {
            for (FieldInfo field : readerContext.reader().getFieldInfos()) {
                fieldNames.add(field.name);
            }
        }
        
        assertEquals("Expected exactly 4 fieldNames, got: " + fieldNames.toString(), 4, fieldNames.size());
        assertTrue("Expected text to be a lucene field", fieldNames.contains("text"));
        assertTrue("Expected title to be a lucene field", fieldNames.contains("title"));
    }
    
    @Test
    public void testFreeTextSearch_nocfs() throws IOException {
        setUpIndex(false);
        testFreeTextSearch();
    }
    
    @Test
    public void testFreeTextSearch_cfs() throws IOException {
        setUpIndex(true);
        testFreeTextSearch();
    }
    
    public void testFreeTextSearch() throws IOException {
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));
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
        setUpIndex(false);
        writer.close();
        
        assertEquals(1, countExtensions(directory, "pos"));
        assertEquals(1, countExtensions(directory, "geo"));
    }
    
    @Test
    public void testDeleteByTitle() throws CorruptIndexException, IOException {
        setUpIndex(false);
        Term deleteTerm = new Term(TITLE_FIELD, "pride");
        writer.deleteDocuments(deleteTerm);
        writer.commit();
        
        writer.forceMerge(1);
        
        int maxDocs = numberOfSegments * titles.length - numberOfSegments;
        verifySegment(maxDocs);
        
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));
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
    public void testGeoSearch_nocfs() throws IOException {
        setUpIndex(false);
        testGeoSearch();
    }
    
    @Test
    public void testGeoSearch_cfs() throws IOException {
        setUpIndex(true);
        testGeoSearch();
    }
    
    public void testGeoSearch() throws IOException {
        GeoIndexReader reader = new GeoIndexReader(directory, geoConfig);
        IndexSearcher searcher = new IndexSearcher(reader);
        GeoCoordinate coordinate = calculateGeoCoordinate(3, 21);
        double longitude = coordinate.getLongitude();
        double lattitude = coordinate.getLatitude();
        float kilometers = Conversions.mi2km(500);
         
        GeoQuery query = new GeoQuery(lattitude, longitude, kilometers);
        TopDocs topDocs = searcher.search(query, 10);
        
        List<String> expectedResults = new Vector<String>();
        for (int i = 0; i < numberOfSegments; i++) {
            expectedResults.add(titles[1]);
        }
        verifyExpectedResults(expectedResults, topDocs, searcher);
    }
}
