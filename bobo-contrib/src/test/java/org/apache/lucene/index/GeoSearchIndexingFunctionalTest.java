package org.apache.lucene.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.LockObtainFailedException;
import org.junit.Before;
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
public class GeoSearchIndexingFunctionalTest extends GeoSearchFunctionalTezt {
    @Before
    public void setUp() throws CorruptIndexException, LockObtainFailedException, IOException {
//        ClassLoader classLoader = new ClassLoader() {
//            public String getSystemProperty(String propertyName) { 
//                super.getSystemResource(name)
//            }
//            
//        };
        
        
        
        buildGeoIndexWriter(); 
        try {
            addDocuments();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
    
    @Test
    public void testGeoFileIsCorrectSize() throws IOException {
        int maxDoc = 10;
        
        String geoFileName = getGeoFileName();
        
        GeoSegmentReader<CartesianGeoRecord> reader = new GeoSegmentReader<CartesianGeoRecord>(directory, 
                geoFileName, maxDoc, IOContext.READ, geoRecordSerializer, geoComparator);
        assertEquals("Expected 2 locations per doc * 10 docs", 2 * maxDoc, reader.getArrayLength());
        reader.close();
    }
    
    @Test
    public void testGeoFilter() throws IOException {
        int maxDocs = 10;
        
        String geoFileName = getGeoFileName(); 
        
        verifyFilter(geoFileName, maxDocs);
    }

    protected String getGeoFileName() throws IOException {
        String[] fileNames = directory.listAll();
        
        String geoFileName = null;
        for (String fileName: fileNames) { 
            if (fileName.endsWith(".geo")) {
                geoFileName = fileName;
            }
        }
        
        assertNotNull(geoFileName);
        
        return geoFileName;
    }
    
    @Test
    public void testGeoFileExists() throws IOException {
        assertEquals(1, countExtensions(directory, "geo"));
    }
    
    @Test
    /**
     * In the new setup geo fields are passed to lucene as well
     */
    public void testLuceneFieldNames() throws CorruptIndexException, IOException {
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
    public void testFreeTextSearch() throws IOException {
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));
        Term term = new Term(TEXT_FIELD, "man");
        TermQuery query = new TermQuery(term);
        
        TopDocs topDocs = searcher.search(query, 10);
        
        List<String> expectedResults = new Vector<String>();
        expectedResults.add(titles[8]);
        expectedResults.add(titles[1]);
        verifyExpectedResults(expectedResults, topDocs, searcher);
    }
   
    
    @Test
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
        expectedResults.add(titles[1]);
        verifyExpectedResults(expectedResults, topDocs, searcher);
    }
    
    @Test
    public void testGeoSearch_onehit() throws IOException {
        GeoIndexReader reader = new GeoIndexReader(directory, geoConfig);
        IndexSearcher searcher = new IndexSearcher(reader);
         
        GeoQuery query = new GeoQuery(LATTITUDE_MIN_VALUE, LONGITUDE_MIN_VALUE, 0.0f);
        TopDocs topDocs = searcher.search(query, 10);
        
        List<String> expectedResults = new Vector<String>();
        expectedResults.add(titles[0]);
        verifyExpectedResults(expectedResults, topDocs, searcher);
    }
}
