package com.browseengine.bobo.geosearch.index.impl;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;


import com.browseengine.bobo.geosearch.GeoVersion;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.impl.GeoRecordComparator;
import com.browseengine.bobo.geosearch.impl.GeoUtil;


/*
 * public GeoRecordOutputBTreeAsArray(Set<GeoRecord> tree, Directory directory, GeoSearchConfig config, 
            GeoSegmentInfo geoSegmentInfo) throws IOException {
            
             String segmentName = geoSegmentInfo.getSegmentName();
        indexOutput = directory.createOutput(segmentName + "." + config.getGeoFileExtension());
 */
public class GeoSegmentReaderTest {

    @Test
    public void test_fileNotFoundGivesZeroGeoRecords() throws Exception {
        Directory ramDir = new RAMDirectory();
        String fileName = "abc.geo";
        GeoSegmentReader geoSegmentReader = 
            new GeoSegmentReader(ramDir, fileName, -1, 16*1024);
        GeoRecord minValue = GeoRecord.MIN_VALID_GEORECORD;
        GeoRecord maxValue = GeoRecord.MAX_VALID_GEORECORD;
        Iterator<GeoRecord> iterator = geoSegmentReader.getIterator(minValue, maxValue);
        assertTrue("iterator for FNF had content, but shouldn't have had any", !iterator.hasNext());
    }
    
    @Test
    public void test_Test() {

        for(int i = 0; i < 100; i++) {
            try {
                // Create a random binary tree or records. 
                int len = 10 + (int) (100 * Math.random());
                TreeSet<GeoRecord> tree = getRandomBTreeOrderedByBitMag(len);
                
                GeoSearchConfig geoConf = new GeoSearchConfig();
                geoConf.setGeoFileExtension("gto");
                
                GeoSegmentInfo geoSegmentInfo = new GeoSegmentInfo();
                geoSegmentInfo.setSegmentName("01");
                geoSegmentInfo.setGeoVersion(GeoVersion.CURRENT_VERSION);
                
                
                RAMDirectory dir = new RAMDirectory();
                String fileName = geoSegmentInfo.getSegmentName() + "." + geoConf.getGeoFileExtension();
                GeoSegmentWriter geoOut =
                    new GeoSegmentWriter(tree, dir, fileName, geoSegmentInfo);
                assertTrue("Not a full binary tree. ", 
                        geoOut.getMaxIndex() < geoOut.getArrayLength());
                geoOut.close();
                
                GeoSegmentReader geoRand = 
                    new GeoSegmentReader(dir, fileName, -1, 16*1024);
                test_IteratorFunctionality(geoRand);
                test_CompleteTreeIsOrderedCorrectly(geoRand);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void test_CompleteTreeIsOrderedCorrectly(GeoSegmentReader grbt) throws IOException {
        int len = grbt.getArrayLength();
        for(int index = 0; index < len; index++) {
            if(index > 0) {
                if(grbt.isALeftChild(index)) {
                     assertTrue("Left child incorecctly greater than parent: "
                             , grbt.compareValuesAt(index, grbt.getParentIndex(index)) != 1);       
                } else {
                    assertTrue("Right child incorecctly less than parent: "
                            , grbt.compareValuesAt(index, grbt.getParentIndex(index)) != -1);   
                }
            }
            if(grbt.hasLeftChild(index)) {
                assertTrue("Left child incorecctly greater than parent: "
                        , grbt.compareValuesAt(grbt.getLeftChildIndex(index), index) != 1);  
            } else if (grbt.hasRightChild(index)) {
                assertTrue("Right child incorecctly less than parent: "
                        , grbt.compareValuesAt(grbt.getRightChildIndex(index), index) != 1);    
            }
        }
    }
    
    public void test_IteratorFunctionality(GeoSegmentReader grbt) throws IOException {
        GeoConverter gc = new GeoConverter();
        ArrayList<LatitudeLongitudeDocId> lldida = getArrayListLLDID(2);
        GeoRecord minRecord, maxRecord;
        minRecord = gc.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, lldida.get(0));
        maxRecord = gc.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, lldida.get(1));
        GeoUtil gu = new GeoUtil();
        if(gu.compare(minRecord, maxRecord) == 1) {
            minRecord = gc.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, lldida.get(1));
            maxRecord = gc.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, lldida.get(0));
        }
        Iterator<GeoRecord> gIt = grbt.getIterator(minRecord, maxRecord);
        GeoRecord current=null, next=null;
        while(gIt.hasNext()) {
            if(next != null) {
                current = next;
            }
            next = gIt.next();
            if(current != null) {
                assertTrue("The indexer is out of order.",gu.compare(current, next) != 1);
            }
            assertTrue("Iterator is out of range ",gu.compare(next, minRecord) != -1 ||
                    gu.compare(next, maxRecord) != 1);
        }
    }
    
    public TreeSet<GeoRecord> getRandomBTreeOrderedByBitMag(int len) {
        Iterator <LatitudeLongitudeDocId> lldidIter = getArrayListLLDID(len).iterator();
        TreeSet<GeoRecord> tree = new TreeSet<GeoRecord>(new GeoRecordComparator());
        GeoConverter gc = new GeoConverter();
        while(lldidIter.hasNext()) {
            byte filterByte = GeoRecord.DEFAULT_FILTER_BYTE;
            tree.add(gc.toGeoRecord(filterByte, lldidIter.next()));
        }
        return tree;
    }
    
    public ArrayList<LatitudeLongitudeDocId> getArrayListLLDID(int len) {
        
        ArrayList<LatitudeLongitudeDocId> lldid = new ArrayList<LatitudeLongitudeDocId>();
        int i, docid;
        double lat,lng;
        for(i=0;i<len;i++) {
            lng = Math.random() * 360.0 - 180.0;
            lat = Math.random() * 180.0 - 90.0; 
            docid = (int)(1 + Math.random() * Integer.MAX_VALUE);
            lldid.add(new LatitudeLongitudeDocId(lat, lng, docid));
        }
        return lldid;
    }
}
