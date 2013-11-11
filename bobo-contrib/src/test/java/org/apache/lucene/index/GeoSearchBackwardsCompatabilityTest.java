package org.apache.lucene.index;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.browseengine.bobo.geosearch.index.bo.GeoCoordinate;
import com.browseengine.bobo.geosearch.index.impl.GeoIndexReader;
import com.browseengine.bobo.geosearch.query.GeoQuery;
import com.browseengine.bobo.geosearch.score.impl.Conversions;

public class GeoSearchBackwardsCompatabilityTest extends GeoSearchFunctionalTezt {
    @Before
    public void setUp() throws IOException {
        initDirectory();
        
        Directory fsDirectory = FSDirectory.open(new File("/usr/local/scbe/bobo/bobo-contrib/src/test/resources/lucene3xIndex"));
        
        //copy the data in legacy directory to our test directory 
        for (String file : fsDirectory.listAll()) {
            fsDirectory.copy(directory, file, file, IOContext.DEFAULT);
        }
        
        buildGeoIndexWriter(true, false);
    }

    @After
    public void tearDown() throws IOException {
        writer.close();
    }
    
    @Test
    public void testGeoSearch_merge() throws IOException {
        writer.forceMerge(1);
        writer.commit();
        
        List<String> expectedResults = new Vector<String>();
        for (int i = 0; i < 10; i++) {
            expectedResults.add(titles[1]);
        }
        testGeoSearch(expectedResults);
    }
    
    @Test
    public void testGeoSearch_basic() throws IOException {
        
        List<String> expectedResults = new Vector<String>();
        for (int i = 0; i < 10; i++) {
            expectedResults.add(titles[1]);
        }
        testGeoSearch(expectedResults);
    }
    
    @Test
    public void testGeoSearch_addNewDocuments() throws IOException {

        addDocuments();
        writer.commit();
        
        List<String> expectedResults = new Vector<String>();
        for (int i = 0; i < 11; i++) {
            expectedResults.add(titles[1]);
        }
        testGeoSearch(expectedResults);
    }
    
    public void testGeoSearch(List<String> expectedResults) throws IOException {
        GeoIndexReader reader = new GeoIndexReader(directory, geoConfig);
        IndexSearcher searcher = new IndexSearcher(reader);
        GeoCoordinate coordinate = calculateGeoCoordinate(3, 21);
        double longitude = coordinate.getLongitude();
        double lattitude = coordinate.getLatitude();
        float kilometers = Conversions.mi2km(500);
         
        GeoQuery query = new GeoQuery(lattitude, longitude, kilometers);
        TopDocs topDocs = searcher.search(query, 10);
        
        verifyExpectedResults(expectedResults, topDocs, searcher);
    }
}
