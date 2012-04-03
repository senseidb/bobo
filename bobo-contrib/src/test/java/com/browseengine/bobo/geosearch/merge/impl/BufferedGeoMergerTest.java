package com.browseengine.bobo.geosearch.merge.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.lucene.index.LuceneUtils;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.IGeoUtil;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.impl.BTree;
import com.browseengine.bobo.geosearch.impl.GeoRecordBTree;
import com.browseengine.bobo.geosearch.impl.MappedFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.merge.IGeoMergeInfo;

/**
 * 
 * @author Geoff Cooney
 *
 */
@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration( { "/TEST-servlet.xml" }) 
@IfProfileValue(name = "test-suite", values = { "unit", "all" })
public class BufferedGeoMergerTest {
    Mockery context = new Mockery(); 
    
    private static final String SEGMENT_BASE_NAME = "segment";
    
    Directory dir;
    
    GeoSearchConfig geoConfig;
    IGeoMergeInfo geoMergeInfo;
    
    BufferedGeoMerger bufferedGeoMerger;
    
    List<SegmentInfo> segmentsToMerge;
    List<SegmentReader> segmentReaders;
    Map<String, GeoRecordBTree> inputTrees; 
    SegmentInfo newSegment;
    
    GeoRecordBTree outputTree;
    
    IGeoConverter geoConverter;
    IGeoUtil geoUtil;
    
    TreeSet<GeoRecord> expectedOutputTree;
    
    @Before
    public void setUp() {
        geoConfig = new GeoSearchConfig();
        geoMergeInfo = context.mock(IGeoMergeInfo.class);
        
        dir = new RAMDirectory();
        geoConverter = geoConfig.getGeoConverter();
        geoUtil = geoConfig.getGeoUtil();
        
        bufferedGeoMerger = new BufferedGeoMerger() {
            @Override
            public BTree<GeoRecord> getInputBTree(Directory directory, String geoFileName, 
                    int bufferSizePerGeoReader) {
                return inputTrees.get(geoFileName);
            }
            
            @Override
            public BTree<GeoRecord> getOutputBTree(int newSegmentSize, Iterator<GeoRecord> inputIterator,
                    Directory directory, String outputFileName, GeoSegmentInfo geoSegmentInfo) throws IOException {
                outputTree = new GeoRecordBTree(newSegmentSize, inputIterator, directory, outputFileName, geoSegmentInfo);
                return outputTree;
            }
            
            @Override
            public boolean loadFieldNameFilterConverter(Directory directory, String geoFileName,
                    IFieldNameFilterConverter fieldNameFilterConverter) throws IOException {
                return true;
            }
        };
        
        expectedOutputTree = geoUtil.getBinaryTreeOrderedByBitMag();
    }
    
    private void setUpMergeObjects(int[] docsPerSegment, int[] deletedDocsPerSegment) throws IOException {
        assertEquals("Test specification error.  Both arrays should contain one entry per segment and be the same size.", 
                docsPerSegment.length, deletedDocsPerSegment.length);
        
        segmentsToMerge = new Vector<SegmentInfo>();
        segmentReaders = new Vector<SegmentReader>();
        inputTrees = new HashMap<String, GeoRecordBTree>();
        
        int segmentStart = 0;
        for (int i = 0; i < docsPerSegment.length; i++) {
            final int segmentSize = docsPerSegment[i];
            final int deletedDocs = deletedDocsPerSegment[i]; 
            final String name = SEGMENT_BASE_NAME + i;
            
            SegmentReader reader = buildSegmentReader(name, segmentSize, deletedDocs);
            segmentReaders.add(reader);
            
            SegmentInfo segment = LuceneUtils.buildSegmentInfo(name, segmentSize, deletedDocs, dir);
            segmentsToMerge.add(segment);
            
            GeoRecordBTree inputTree = buildInputTree(segmentStart, segmentSize, deletedDocsPerSegment[i]);
            String fileName = geoConfig.getGeoFileName(name);
            inputTrees.put(fileName, inputTree);
            
            segmentStart += (segmentSize - deletedDocs);
        }
        
        String newSegmentName = "newSegment";
        newSegment = LuceneUtils.buildSegmentInfo(newSegmentName, segmentStart, 0, dir);
    }
    
    private SegmentReader buildSegmentReader(final String name, final int segmentSize, final int deletedDocs) {
        SegmentReader reader = new SegmentReader() {
            @Override
            public synchronized boolean isDeleted(int n) {
                return isIdDeleted(n, deletedDocs, segmentSize);
            }
            
            @Override 
            public String getSegmentName() {
                return name;
            }
            
            @Override 
            public int maxDoc() {
                return segmentSize;
            }
        };
        
        return reader;
    }

    private GeoRecordBTree buildInputTree(int segmentStartInNewIndex, int segmentSize, int numberOfDeletes) throws IOException {
        TreeSet<GeoRecord> tree = geoUtil.getBinaryTreeOrderedByBitMag();
        
        int absoluteDocId = segmentStartInNewIndex;
        for (int i = 0; i < segmentSize; i++) {
            int docid = i;
            boolean isDeleted = isIdDeleted(docid, numberOfDeletes, segmentSize);
            int numberOfLocations = (int)(Math.random() * 4);
            for (int j = 0; j < numberOfLocations; j++) {
                double longitude = Math.random();
                double latitude = Math.random();
                LatitudeLongitudeDocId longitudeLatitudeDocId = new LatitudeLongitudeDocId(latitude, longitude, docid);
                GeoRecord geoRecord = geoConverter.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, longitudeLatitudeDocId);
                
                tree.add(geoRecord);
                
                if (!isDeleted) {
                    LatitudeLongitudeDocId absoluteLongitudeLatitudeDocId = new LatitudeLongitudeDocId(latitude, longitude, absoluteDocId);
                    GeoRecord absoluteGeoRecord = geoConverter.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, absoluteLongitudeLatitudeDocId);
                    expectedOutputTree.add(absoluteGeoRecord);
                }
            }
            
            if (!isDeleted) {
                absoluteDocId ++;
            }
        }
        
        return new GeoRecordBTree(tree);
    }
    
    private boolean isIdDeleted(int id, int numberOfDeletes, int totalDocs) {
        if (numberOfDeletes != 0) {
            int previousNumDeleted = Math.round((id * numberOfDeletes) / (float)totalDocs);
            int nextNumDeleted = Math.round(((id + 1) * numberOfDeletes) / (float)totalDocs);
            
            if (nextNumDeleted != previousNumDeleted) {
                return true;
            }
        }
        
        return false;
    }
    
    boolean isVerifyOutputTreeAgainstExpected = true;
    
    private void checkOutputTreeAgainstExpected() throws IOException {
        if (!isVerifyOutputTreeAgainstExpected) {
            return;
        }
        assertEquals("trees sould be equal in size", expectedOutputTree.size(), outputTree.getArrayLength());
        Iterator<GeoRecord> outputIterator = outputTree.getIterator(GeoRecord.MIN_VALID_GEORECORD, GeoRecord.MAX_VALID_GEORECORD);
        Iterator<GeoRecord> expectedIterator = expectedOutputTree.iterator();
        
        int i = 0;
        while (outputIterator.hasNext() && expectedIterator.hasNext()) {
            GeoRecord actualGeoRecord = outputIterator.next();
            GeoRecord expectedGeoRecord = expectedIterator.next(); 
            
            assertEquals("Index " + i + " of tree does not match expected.  Expected LngLatDocId=" +
                        geoConverter.toLongitudeLatitudeDocId(expectedGeoRecord) + 
                        ";  Actual LngLatDocId=" + geoConverter.toLongitudeLatitudeDocId(actualGeoRecord), 
                    expectedGeoRecord, actualGeoRecord);
            
            i++;
        }
    }
    
    private boolean isNoGeoFiles = false;
    
    private void doMerge() throws IOException {
        if (isNoGeoFiles) {
            context.checking(new Expectations() {
                {
                    atLeast(1).of(geoMergeInfo).getSegmentsToMerge();
                    will(returnValue(segmentsToMerge));
                    
                    ignoring(geoMergeInfo).checkAborted(dir);
                    
                    // the expectation is that if there are no geo files,
                    // getNewSegment() will not be called.
                    //atLeast(1).of(geoMergeInfo).getNewSegment();
                    //will(returnValue(newSegment));
                    
                    ignoring(geoMergeInfo).getDirectory();
                    will(returnValue(dir));
                    
                    atLeast(1).of(geoMergeInfo).getReaders();
                    will(returnValue(segmentReaders));
                }
            });

        } else {
            context.checking(new Expectations() {
                {
                    atLeast(1).of(geoMergeInfo).getSegmentsToMerge();
                    will(returnValue(segmentsToMerge));
                
                    ignoring(geoMergeInfo).checkAborted(dir);
                
                    atLeast(1).of(geoMergeInfo).getNewSegment();
                    will(returnValue(newSegment));
                
                    ignoring(geoMergeInfo).getDirectory();
                    will(returnValue(dir));
                
                    atLeast(1).of(geoMergeInfo).getReaders();
                    will(returnValue(segmentReaders));
                }
            });
        }
        
        bufferedGeoMerger.merge(geoMergeInfo, geoConfig);
        checkOutputTreeAgainstExpected();
        
        context.assertIsSatisfied();
    }
    
    @Test
    public void testMerge_no_segment0_GeoFile() throws IOException {
        noGeoFileNames = new HashSet<String>();
        noGeoFileNames.add("segment0.geo");

        verifyMissingGeoFile();
    }
    
    @Test
    public void testMerge_no_segment1_GeoFile() throws IOException {
        noGeoFileNames = new HashSet<String>();
        noGeoFileNames.add("segment1.geo");

        verifyMissingGeoFile();
    }
    
    @Test
    public void testMerge_no_GeoFiles() throws IOException {
        isNoGeoFiles = true;
        
        noGeoFileNames = new HashSet<String>();
        noGeoFileNames.add("segment0.geo");
        noGeoFileNames.add("segment1.geo");

        verifyMissingGeoFile();
    }


    
    private void verifyMissingGeoFile() throws IOException {
        isVerifyOutputTreeAgainstExpected = false;

        initNoGeoFile();
        
        testMergeSimple();
    }
    
    private Set<String> noGeoFileNames;
    
    private void initNoGeoFile() {
        
        bufferedGeoMerger = new BufferedGeoMerger() {
            @Override
            public BTree<GeoRecord> getInputBTree(Directory directory, String geoFileName, 
                    int bufferSizePerGeoReader) throws IOException {
                if (noGeoFileNames.contains(geoFileName)) {
                    // empty TreeSet<GeoRecord> tree
                    return new GeoRecordBTree(new TreeSet<GeoRecord>());
                }
                return inputTrees.get(geoFileName);
            }
            
            @Override
            public BTree<GeoRecord> getOutputBTree(int newSegmentSize, Iterator<GeoRecord> inputIterator,
                    Directory directory, String outputFileName, GeoSegmentInfo geoSegmentInfo) throws IOException {
                outputTree = new GeoRecordBTree(newSegmentSize, inputIterator, directory, outputFileName, geoSegmentInfo);
                return outputTree;
            }
            
            @Override
            public boolean loadFieldNameFilterConverter(Directory directory, String geoFileName,
                    IFieldNameFilterConverter fieldNameFilterConverter) throws IOException {
                return !noGeoFileNames.contains(geoFileName);
            }
        };

    }
    
    @Test
    //10 x 10, no deleted docs
    public void testMergeSimple() throws IOException {
        int[] docsPerSegment = new int[] {10, 10};
        int[] deletedDocsPerSegment = new int[] {0, 0};
        setUpMergeObjects(docsPerSegment, deletedDocsPerSegment);
        
        doMerge();
    }
    
    @Test
    public void test10SmallSegments() throws IOException {
        int[] docsPerSegment = new int[] {10, 10, 10, 10, 10, 10, 10, 10, 10, 10};
        int[] deletedDocsPerSegment = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        setUpMergeObjects(docsPerSegment, deletedDocsPerSegment);
        
        doMerge();
    }
    
    @Test
    public void test10Segments_1000docs() throws IOException {
        int[] docsPerSegment = new int[] {1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000};
        int[] deletedDocsPerSegment = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        setUpMergeObjects(docsPerSegment, deletedDocsPerSegment);
        
        doMerge();
    }
    
    @Test
    public void testSimpleDelete() throws IOException {
        int[] docsPerSegment = new int[] {10};
        int[] deletedDocsPerSegment = new int[] {5};
        setUpMergeObjects(docsPerSegment, deletedDocsPerSegment);
        
        doMerge();
    }
    
    @Test
    public void testDeleteAndMerge_2SmallSegments() throws IOException {
        int[] docsPerSegment = new int[] {10, 10};
        int[] deletedDocsPerSegment = new int[] {2, 1};
        setUpMergeObjects(docsPerSegment, deletedDocsPerSegment);
        
        doMerge();
    }
    
    @Test
    public void testDeleteAndMerge_10LargeSegments_variedDeletes() throws IOException {
        int[] docsPerSegment = new int[] {1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000};
        int[] deletedDocsPerSegment = new int[] {10, 0, 100, 32, 50, 200, 90, 33, 5, 2};
        setUpMergeObjects(docsPerSegment, deletedDocsPerSegment);
        
        doMerge();
    }
    
    @Test
    public void testDelete_WholeSegment() throws IOException {
        int[] docsPerSegment = new int[] {1000};
        int[] deletedDocsPerSegment = new int[] {1000};
        setUpMergeObjects(docsPerSegment, deletedDocsPerSegment);
        
        doMerge();
    }
    
    @Test
    public void testMergeAndDelete_OneWholeSegmentDeleted() throws IOException {
        int[] docsPerSegment = new int[] {1000, 1000};
        int[] deletedDocsPerSegment = new int[] {1000, 0};
        setUpMergeObjects(docsPerSegment, deletedDocsPerSegment);
        
        doMerge();
    }
    
    @Test
    public void testMergeAndDelete_VariedSegmentSize_VariedDeletes() throws IOException {
        int[] docsPerSegment = new int[] {1000, 10, 2000, 2000, 10, 100, 300, 70, 7, 1};
        int[] deletedDocsPerSegment = new int[] {200, 5, 1000, 300, 1, 0, 20, 6, 2, 0};
        setUpMergeObjects(docsPerSegment, deletedDocsPerSegment);
        
        doMerge();
    }
}
