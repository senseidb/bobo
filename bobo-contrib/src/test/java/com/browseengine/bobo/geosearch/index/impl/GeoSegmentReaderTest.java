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
import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordComparator;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordSerializer;
import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;
import com.browseengine.bobo.geosearch.solo.impl.IDGeoRecordComparator;
import com.browseengine.bobo.geosearch.solo.impl.IDGeoRecordSerializer;

public class GeoSegmentReaderTest {

    CartesianGeoRecordComparator geoComparator = new CartesianGeoRecordComparator();
    
    @Test
    public void test_fileNotFoundGivesZeroGeoRecords() throws Exception {
        Directory ramDir = new RAMDirectory();
        String fileName = "abc.geo";
        GeoSegmentReader<CartesianGeoRecord> geoSegmentReader = 
            new GeoSegmentReader<CartesianGeoRecord>(ramDir, fileName, -1, 16*1024, 
                    new CartesianGeoRecordSerializer(), new CartesianGeoRecordComparator());
        CartesianGeoRecord minValue = CartesianGeoRecord.MIN_VALID_GEORECORD;
        CartesianGeoRecord maxValue = CartesianGeoRecord.MAX_VALID_GEORECORD;
        Iterator<CartesianGeoRecord> iterator = geoSegmentReader.getIterator(minValue, maxValue);
        assertTrue("iterator for FNF had content, but shouldn't have had any", !iterator.hasNext());
    }
    
    @Test
    public void test_WriteThenRead() {

        for(int i = 0; i < 100; i++) {
            try {
                // Create a random binary tree or records. 
                int len = 10 + (int) (100 * Math.random());
                TreeSet<CartesianGeoRecord> tree = getRandomBTreeOrderedByBitMag(len);
                
                GeoSearchConfig geoConf = new GeoSearchConfig();
                geoConf.setGeoFileExtension("gto");
                GeoSegmentInfo geoSegmentInfo = buildGeoSegmentInfo(GeoVersion.CURRENT_VERSION);
                
                IGeoRecordSerializer<CartesianGeoRecord> geoRecordSerializer = new CartesianGeoRecordSerializer();
                
                RAMDirectory dir = new RAMDirectory();
                String fileName = geoSegmentInfo.getSegmentName() + "." + geoConf.getGeoFileExtension();
                GeoSegmentWriter<CartesianGeoRecord> geoOut = new GeoSegmentWriter<CartesianGeoRecord>(
                        tree, dir, fileName, geoSegmentInfo, geoRecordSerializer);
                assertTrue("Not a full binary tree. ", 
                        geoOut.getMaxIndex() < geoOut.getArrayLength());
                geoOut.close();
                
                GeoSegmentReader<CartesianGeoRecord> geoRand = 
                    new GeoSegmentReader<CartesianGeoRecord>(dir, fileName, -1, 16*1024,
                            new CartesianGeoRecordSerializer(), new CartesianGeoRecordComparator());
                validate_IteratorFunctionality(geoRand);
                validate_CompleteTreeIsOrderedCorrectly(geoRand);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Test
    public void test_WriteThenRead_V1() throws IOException {
        int len = 100;
        int idBytes = 16;
        
        GeoSearchConfig geoConf = new GeoSearchConfig();
        geoConf.setGeoFileExtension("gto");
        
        GeoSegmentInfo geoSegmentInfo = buildGeoSegmentInfo(GeoVersion.CURRENT_GEOONLY_VERSION);
        geoSegmentInfo.setBytesPerRecord(idBytes + IDGeoRecordSerializer.INTERLACE_BYTES);
        
        RAMDirectory dir = new RAMDirectory();
        String fileName = geoSegmentInfo.getSegmentName() + "." + geoConf.getGeoFileExtension();
        IDGeoRecordSerializer geoRecordSerializer = new IDGeoRecordSerializer();
        
        //build data
        TreeSet<IDGeoRecord> tree = new TreeSet<IDGeoRecord>(new IDGeoRecordComparator());
        for (int i = 0 ; i < len; i++) {
            byte[] id = new byte[idBytes];
            for (int idIndex = 0; idIndex < idBytes; idIndex++) {
                id[idIndex] = (byte)(i - idIndex);
            }
            IDGeoRecord geoRecord = new IDGeoRecord(100*i, i, id);
            tree.add(geoRecord);
        }
        
        //write data
        GeoSegmentWriter<IDGeoRecord> geoOut = new GeoSegmentWriter<IDGeoRecord>(
                tree, dir, fileName, geoSegmentInfo, geoRecordSerializer);
        assertTrue("Not a full binary tree. ", 
                geoOut.getMaxIndex() < geoOut.getArrayLength());
        geoOut.close();
        
        //read and verify data
        GeoSegmentReader<CartesianGeoRecord> geoSegmentReader = 
            new GeoSegmentReader<CartesianGeoRecord>(dir, fileName, -1, 16*1024,
                    new CartesianGeoRecordSerializer(), new CartesianGeoRecordComparator());
        validate_IteratorFunctionality(geoSegmentReader);
        validate_CompleteTreeIsOrderedCorrectly(geoSegmentReader);
    }
    
    private GeoSegmentInfo buildGeoSegmentInfo(int version) {
        GeoSegmentInfo geoSegmentInfo = new GeoSegmentInfo();
        geoSegmentInfo.setSegmentName("01");
        geoSegmentInfo.setGeoVersion(version);
        
        return geoSegmentInfo;
    }
    
    public void validate_CompleteTreeIsOrderedCorrectly(GeoSegmentReader<CartesianGeoRecord> grbt) throws IOException {
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
    
    public void validate_IteratorFunctionality(GeoSegmentReader<CartesianGeoRecord> grbt) throws IOException {
        GeoConverter gc = new GeoConverter();
        ArrayList<LatitudeLongitudeDocId> lldida = getArrayListLLDID(2);
        CartesianGeoRecord minRecord, maxRecord;
        minRecord = gc.toCartesianGeoRecord(lldida.get(0), CartesianGeoRecord.DEFAULT_FILTER_BYTE);
        maxRecord = gc.toCartesianGeoRecord(lldida.get(1), CartesianGeoRecord.DEFAULT_FILTER_BYTE);
        if(geoComparator.compare(minRecord, maxRecord) == 1) {
            minRecord = gc.toCartesianGeoRecord(lldida.get(1), CartesianGeoRecord.DEFAULT_FILTER_BYTE);
            maxRecord = gc.toCartesianGeoRecord(lldida.get(0), CartesianGeoRecord.DEFAULT_FILTER_BYTE);
        }
        Iterator<CartesianGeoRecord> gIt = grbt.getIterator(minRecord, maxRecord);
        CartesianGeoRecord current=null, next=null;
        while(gIt.hasNext()) {
            if(next != null) {
                current = next;
            }
            next = gIt.next();
            if(current != null) {
                assertTrue("The indexer is out of order.",geoComparator.compare(current, next) != 1);
            }
            assertTrue("Iterator is out of range ",geoComparator.compare(next, minRecord) != -1 ||
                    geoComparator.compare(next, maxRecord) != 1);
        }
    }
    
    public TreeSet<CartesianGeoRecord> getRandomBTreeOrderedByBitMag(int len) {
        Iterator <LatitudeLongitudeDocId> lldidIter = getArrayListLLDID(len).iterator();
        TreeSet<CartesianGeoRecord> tree = new TreeSet<CartesianGeoRecord>(new CartesianGeoRecordComparator());
        GeoConverter gc = new GeoConverter();
        while(lldidIter.hasNext()) {
            byte filterByte = CartesianGeoRecord.DEFAULT_FILTER_BYTE;
            tree.add(gc.toCartesianGeoRecord(lldidIter.next(), filterByte));
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
