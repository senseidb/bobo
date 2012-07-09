package com.browseengine.bobo.geosearch.index.impl;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordComparator;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordSerializer;
import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.impl.GeoRecordBTree;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinate;
import com.browseengine.bobo.geosearch.query.GeoQuery;
import com.browseengine.bobo.geosearch.query.GeoScorer;
import com.browseengine.bobo.geosearch.query.GeoWeight;
import com.browseengine.bobo.geosearch.score.impl.HaversineComputeDistance;


public class GeoQueryTest {
    private GeoIndexReader geoIndexReader;
    List<GeoSegmentReader<CartesianGeoRecord>> geoSubReaders;
    private static class MyGeoSegmentReader extends GeoSegmentReader<CartesianGeoRecord> {
        private final GeoRecordBTree tree;
        
        public MyGeoSegmentReader(GeoRecordBTree tree, int maxDoc) {
            super(tree.getArrayLength(), maxDoc, new CartesianGeoRecordSerializer(), 
                    new CartesianGeoRecordComparator());
            this.tree = tree;
        }
        
        /**
         * Delegates to the GeoRecordBTree from the constructor.
         * 
         * {@inheritDoc}
         */
        @Override
        protected CartesianGeoRecord getValueAtIndex(int index) {
            return tree.getValueAtIndex(index);
        }
    }

    @Test
    public void test_Test() throws IOException {
        GeoCoordinate gcord = new GeoCoordinate(Math.random() * 140.0 - 70.0,
                Math.random() * 360.0 - 180.0);
        float rangeInKm = (float) (Math.random()*10.0);
        ArrayList<LatitudeLongitudeDocId> indexedDocument = getSegmentOfLongitudeLatitudeDocIds(100 + (int)(Math.random()*1000), gcord);
        
        printAllDocIdsInRange(rangeInKm, indexedDocument, gcord);
        
        TreeSet<CartesianGeoRecord> treeSet = arrayListToTreeSet(indexedDocument); 
        GeoRecordBTree geoRecordBTree = new GeoRecordBTree(treeSet); 
        MyGeoSegmentReader geoSegmentReader = new MyGeoSegmentReader(geoRecordBTree, indexedDocument.size());
        geoSubReaders = new ArrayList<GeoSegmentReader<CartesianGeoRecord>>(); 
        geoSubReaders.add(geoSegmentReader);
        
        GeoConverter gc = new GeoConverter();
        CartesianGeoRecord cgr = gc.toCartesianGeoRecord(new LatitudeLongitudeDocId(gcord.getLongitude(), gcord.getLatitude(), 0), (byte)0);
        CartesianCoordinateDocId ccd = gc.toCartesianCoordinateDocId(cgr);
        GeoQuery geoQuery = new GeoQuery(ccd.x, ccd.y, ccd.z, rangeInKm);
        GeoWeight geoWeight = (GeoWeight)geoQuery.createWeight(null);
        Directory directory = buildEmptyDirectory();
        geoIndexReader = new GeoIndexReader(directory, new GeoSearchConfig());
        geoIndexReader.setGeoSegmentReaders(geoSubReaders);
        boolean scoreInOrder = true, topScorer = true; 
        GeoScorer geoScorer = (GeoScorer)geoWeight.scorer(geoIndexReader, scoreInOrder, topScorer);
        test_Scorer(rangeInKm, geoScorer, gcord, indexedDocument);
        assertTrue("",0==0);
    }
    
    /*
    private void test_TopDocs(TopDocs topDocs) {
        ScoreDoc[] scoreDosArray = topDocs.scoreDocs;   
        for(ScoreDoc scoredoc: scoreDosArray){
           System.out.println("Top documents by id are: "
                         + scoredoc.doc);   
        } 
    }
    */
    private void printAllDocIdsInRange(float rangeInMiles, ArrayList<LatitudeLongitudeDocId> indexedDocument,
            GeoCoordinate gcord) {
        
        
        
        System.err.println("Enter the is doc in range check.");
        Iterator <LatitudeLongitudeDocId> it = indexedDocument.iterator();
        HaversineComputeDistance hcd = new  HaversineComputeDistance();
        LatitudeLongitudeDocId lldid = null;
        while(it.hasNext()) {
            lldid = it.next();
            if(hcd.getDistanceInMiles(gcord.getLongitude(), gcord.getLatitude(), lldid.longitude, lldid.latitude)
                    < rangeInMiles) {
                System.err.println("Document id within range is: " + lldid.docid);
            }
        }
        
        
    }

    private void test_Scorer(float rangeInMiles, 
                             GeoScorer geoScorer,
                             GeoCoordinate gcord,
                             ArrayList<LatitudeLongitudeDocId> indexedDocument) throws IOException {
        
        
        ArrayList<Integer> recordRangeHitsDocIds = new ArrayList<Integer>();
        HaversineComputeDistance hcd = new  HaversineComputeDistance();
        int doc = geoScorer.nextDoc();
        while(doc != geoScorer.NO_MORE_DOCS) {
            recordRangeHitsDocIds.add(new Integer(doc));
//            assertTrue("Hit out of range! ", 
//                    hcd.getDistanceInMiles(gcord.getLongitude(), 
//                                           gcord.getLatitude(),
//                                           indexedDocument.get(doc).longitude,
//                                           indexedDocument.get(doc).latitude)
//                    <= rangeInMiles);
            doc = geoScorer.nextDoc();
        }
        
        Iterator<LatitudeLongitudeDocId> it = indexedDocument.iterator();
        int docid;
        while(it.hasNext()) {
            docid = it.next().docid;
            if(!recordRangeHitsDocIds.contains(new Integer(docid))){
              assertTrue("Document less than rangeInMiles from centroid but not considered a hit ", 
              hcd.getDistanceInMiles(gcord.getLongitude(), 
                                     gcord.getLatitude(),
                                     indexedDocument.get(docid).longitude,
                                     indexedDocument.get(docid).latitude)
              > rangeInMiles);
            }
        }
    }
    
    private Directory buildEmptyDirectory() throws IOException {
        RAMDirectory directory = new RAMDirectory();
        Version version = Version.LUCENE_CURRENT;
        Analyzer analyzer =  new StandardAnalyzer(version);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(version, analyzer);
        IndexWriter writer = new IndexWriter(directory, indexWriterConfig);
        writer.close();
        return directory;
    }
    public TreeSet<CartesianGeoRecord> arrayListToTreeSet(ArrayList<LatitudeLongitudeDocId> indexedDocument) {
        TreeSet<CartesianGeoRecord> treeSet = new  TreeSet<CartesianGeoRecord>(new CartesianGeoRecordComparator());
        GeoConverter gc = new GeoConverter();
        for (int i = 0; i < indexedDocument.size(); i++) {
            treeSet.add(gc.toCartesianGeoRecord(indexedDocument.get(i), (byte)0));
        }
        return treeSet;
    }
    public ArrayList<LatitudeLongitudeDocId> getSegmentOfLongitudeLatitudeDocIds(int len, GeoCoordinate gcord) {
        int docid = 0;
        ArrayList<LatitudeLongitudeDocId> segmentOfLongitudeLatitudeDocIds =
            new ArrayList<LatitudeLongitudeDocId>();
        for(int i = 0; i < len; i++) {
            segmentOfLongitudeLatitudeDocIds.add(
                    getNewRandomLongitudeLatitudeDocId(docid++, gcord));
        }
        return segmentOfLongitudeLatitudeDocIds;
    }
    public LatitudeLongitudeDocId getNewRandomLongitudeLatitudeDocId(int docid, GeoCoordinate gcord) {
        return new LatitudeLongitudeDocId(
                gcord.getLatitude() + Math.random()*0.4 - 0.2, //Math.random() * 360.0 - 180.0,
                gcord.getLongitude() + Math.random()*0.4 - 0.2, //Math.random() * 180.0 - 90.0,
                docid);
    }
}
