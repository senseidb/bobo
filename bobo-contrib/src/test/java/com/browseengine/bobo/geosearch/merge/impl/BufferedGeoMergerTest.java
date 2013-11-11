package com.browseengine.bobo.geosearch.merge.impl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocMapExposer;
import org.apache.lucene.index.LuceneUtils;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.MergeState.CheckAbort;
import org.apache.lucene.index.MergeState.DocMap;
import org.apache.lucene.index.MergeStateExposer;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentWriteState;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IOContext.Context;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.IGeoUtil;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.impl.BTree;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordComparator;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordSerializer;
import com.browseengine.bobo.geosearch.impl.GeoRecordBTree;
import com.browseengine.bobo.geosearch.impl.MappedFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;
import com.browseengine.bobo.geosearch.util.StubAtomicReader;

/**
 * 
 * @author Geoff Cooney
 *
 */
@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration( { "/TEST-servlet.xml" }) 
@IfProfileValue(name = "test-suite", values = { "unit", "all" })
public class BufferedGeoMergerTest {
    
    private static final String SEGMENT_BASE_NAME = "segment";
    
    Directory dir;
    
    GeoSearchConfig geoConfig;
    
    BufferedGeoMerger bufferedGeoMerger;
    
    List<SegmentInfo> segmentsToMerge;
    List<AtomicReader> segmentReaders;
    Map<String, GeoRecordBTree> inputTrees; 
    SegmentInfo newSegment;
    
    GeoRecordBTree outputTree;
    
    IGeoConverter geoConverter;
    IGeoUtil geoUtil;
    
    TreeSet<CartesianGeoRecord> expectedOutputTree;
    
    @Before
    public void setUp() {
        geoConfig = new GeoSearchConfig();
        
        dir = new RAMDirectory();
        geoConverter = geoConfig.getGeoConverter();
        geoUtil = geoConfig.getGeoUtil();
        
        bufferedGeoMerger = new BufferedGeoMerger() {
            @Override
            public BTree<CartesianGeoRecord> getInputBTree(AtomicReader reader, GeoSearchConfig config) {
                return inputTrees.get(getSegmentName(reader) + "." + config.getGeoFileExtension());
            }
            
            @Override
            public BTree<CartesianGeoRecord> getOutputBTree(int newSegmentSize, Iterator<CartesianGeoRecord> inputIterator,
                    Directory directory, IOContext ioContext, String outputFileName, GeoSegmentInfo geoSegmentInfo) throws IOException {
                outputTree = new GeoRecordBTree(newSegmentSize, inputIterator, directory, outputFileName, geoSegmentInfo);
                return outputTree;
            }
            
            @Override
            public boolean loadFieldNameFilterConverter(Directory directory, String geoFileName,
                    IFieldNameFilterConverter fieldNameFilterConverter, IOContext ioContext) throws IOException {
                return true;
            }
            
            @Override
            public String getSegmentName(AtomicReader reader) {
                return ((StubAtomicReader)reader).getSegmentName();
            }
            
            @Override
            public Directory getDirectory(AtomicReader reader, GeoSearchConfig config) throws IOException {
                return dir;
            }
        };
        
        expectedOutputTree = geoUtil.getBinaryTreeOrderedByBitMag();
    }
    
    private void setUpMergeObjects(int[] docsPerSegment, int[] deletedDocsPerSegment) throws IOException {
        assertEquals("Test specification error.  Both arrays should contain one entry per segment and be the same size.", 
                docsPerSegment.length, deletedDocsPerSegment.length);
        
        segmentsToMerge = new Vector<SegmentInfo>();
        segmentReaders = new Vector<AtomicReader>();
        inputTrees = new HashMap<String, GeoRecordBTree>();
        
        int segmentStart = 0;
        DocMap[] docMaps = new DocMap[docsPerSegment.length];
        int[] docBase = new int[docsPerSegment.length];
        int previousDocBase = 0;
        for (int i = 0; i < docsPerSegment.length; i++) {
            final int segmentSize = docsPerSegment[i];
            final int deletedDocs = deletedDocsPerSegment[i]; 
            final String name = SEGMENT_BASE_NAME + i;
            
            SegmentInfo segment = LuceneUtils.buildSegmentInfo(name, segmentSize, geoConfig, dir);
            segmentsToMerge.add(segment);

            docMaps[i] = buildDocMap(segmentSize, deletedDocs);
            docBase[i] = previousDocBase;
            previousDocBase += segmentSize - deletedDocs;
            
            AtomicReader reader = buildSegmentReader(segment.name);
            segmentReaders.add(reader);
            
            GeoRecordBTree inputTree = buildInputTree(segmentStart, segmentSize, deletedDocsPerSegment[i]);
            String fileName = geoConfig.getGeoFileName(name);
            inputTrees.put(fileName, inputTree);
            
            segmentStart += (segmentSize - deletedDocs);
        }
        
        CheckAbort checkAbort = new CheckAbort(null, dir) {
            @Override
            public void work(double units) throws MergePolicy.MergeAbortedException {
                //do nothing
            }
        };
        
        
        newSegmentName = "newSegment";
        newSegment = LuceneUtils.buildSegmentInfo(newSegmentName, segmentStart, geoConfig, dir);
        mergeState = new MergeStateExposer(segmentReaders, newSegment, null, checkAbort);
        mergeState.docMaps = docMaps;
        mergeState.docBase = docBase;
        
        segmentWriteState = new SegmentWriteState(null, dir, newSegment, null, 1, null, new IOContext(Context.MERGE));
    }
    
    private AtomicReader buildSegmentReader(String segmentName) throws IOException {
        AtomicReader reader = new StubAtomicReader(segmentName);
        
        return reader;
    }

    private GeoRecordBTree buildInputTree(int segmentStartInNewIndex, int segmentSize, int numberOfDeletes) throws IOException {
        TreeSet<CartesianGeoRecord> tree = geoUtil.getBinaryTreeOrderedByBitMag();
        
        int absoluteDocId = segmentStartInNewIndex;
        for (int i = 0; i < segmentSize; i++) {
            int docid = i;
            boolean isDeleted = isIdDeleted(docid, numberOfDeletes, segmentSize);
            int numberOfLocations = (int)(Math.random() * 4);
            for (int j = 0; j < numberOfLocations; j++) {
                double longitude = Math.random();
                double latitude = Math.random();
                LatitudeLongitudeDocId longitudeLatitudeDocId = new LatitudeLongitudeDocId(latitude, longitude, docid);
                CartesianGeoRecord geoRecord = geoConverter.toCartesianGeoRecord(longitudeLatitudeDocId, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
                
                tree.add(geoRecord);
                
                if (!isDeleted) {
                    LatitudeLongitudeDocId absoluteLongitudeLatitudeDocId = new LatitudeLongitudeDocId(latitude, longitude, absoluteDocId);
                    CartesianGeoRecord absoluteGeoRecord = geoConverter.toCartesianGeoRecord(absoluteLongitudeLatitudeDocId, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
                    expectedOutputTree.add(absoluteGeoRecord);
                }
            }
            
            if (!isDeleted) {
                absoluteDocId ++;
            }
        }
        
        return new GeoRecordBTree(tree);
    }
    
    private DocMap buildDocMap(final int segmentSize, final int numberOfDeletes) {
        final List<Integer> docMap = new ArrayList<Integer>();
        int del = 0;
        for (int i = 0; i < segmentSize; ++i) {
            if (isIdDeleted(i, numberOfDeletes, segmentSize)) {
                docMap.add(-1);
                ++del;
            } else {
                docMap.add(i - del);
            }
        }
        
        return new DocMapExposer(docMap, del);
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
        checkTreeAgainstExpected(outputTree);
    }
    
    private void checkTreeAgainstExpected(BTree<CartesianGeoRecord> outputTree) throws IOException {
        assertEquals("trees sould be equal in size", expectedOutputTree.size(), outputTree.getArrayLength());
        Iterator<CartesianGeoRecord> outputIterator = outputTree.getIterator(CartesianGeoRecord.MIN_VALID_GEORECORD, CartesianGeoRecord.MAX_VALID_GEORECORD);
        Iterator<CartesianGeoRecord> expectedIterator = expectedOutputTree.iterator();
        
        int i = 0;
        while (outputIterator.hasNext() && expectedIterator.hasNext()) {
            CartesianGeoRecord actualGeoRecord = outputIterator.next();
            CartesianGeoRecord expectedGeoRecord = expectedIterator.next(); 
            
            assertEquals("Index " + i + " of tree does not match expected.  Expected CartCoordDocId=" +
                        geoConverter.toCartesianCoordinateDocId(expectedGeoRecord) + 
                        ";  Actual LngLatDocId=" + geoConverter.toCartesianCoordinateDocId(actualGeoRecord), 
                    expectedGeoRecord, actualGeoRecord);
            
            i++;
        }
    }
    
    private void doMerge() throws IOException {
        bufferedGeoMerger.merge(segmentWriteState, mergeState, geoConfig);
        checkOutputTreeAgainstExpected();
        
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

    private String newSegmentName;

    private SegmentWriteState segmentWriteState;

    private MergeState mergeState;

    private void initNoGeoFile() {
        
        bufferedGeoMerger = new BufferedGeoMerger() {
            @Override
            public BTree<CartesianGeoRecord> getInputBTree(AtomicReader reader, GeoSearchConfig config) throws IOException {
                String geoFileName = getSegmentName(reader) + "." + config.getGeoFileExtension();
                if (noGeoFileNames.contains(geoFileName)) {
                    // empty TreeSet<GeoRecord> tree
                    return new GeoRecordBTree(new TreeSet<CartesianGeoRecord>());
                }
                return inputTrees.get(geoFileName);
            }
            
            @Override
            public BTree<CartesianGeoRecord> getOutputBTree(int newSegmentSize, Iterator<CartesianGeoRecord> inputIterator,
                    Directory directory, IOContext ioContext, String outputFileName, GeoSegmentInfo geoSegmentInfo) 
                            throws IOException {
                outputTree = new GeoRecordBTree(newSegmentSize, inputIterator, directory, outputFileName, geoSegmentInfo);
                return outputTree;
            }
            
            @Override
            public boolean loadFieldNameFilterConverter(Directory directory, String geoFileName,
                    IFieldNameFilterConverter fieldNameFilterConverter, IOContext ioContext) throws IOException {
                return !noGeoFileNames.contains(geoFileName);
            }
            
            @Override
            public String getSegmentName(AtomicReader reader) {
                return ((StubAtomicReader)reader).getSegmentName();
            }
            
            @Override
            public Directory getDirectory(AtomicReader reader, GeoSearchConfig config) throws IOException {
                return dir;
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
    
    @Test
    public void testIncorrectNewSegmentSize() throws IOException {
        bufferedGeoMerger = new BufferedGeoMerger();
        
        int[] docsPerSegment = new int[] {10, 10};
        int[] deletedDocsPerSegment = new int[] {0, 0};
        setUpMergeObjects(docsPerSegment, deletedDocsPerSegment);
        
        
        List<BTree<CartesianGeoRecord>> mergeInputBTrees = new ArrayList(inputTrees.values());
        
        int newSegmentSize = 21;
        GeoSearchConfig config = geoConfig;
        MappedFieldNameFilterConverter fieldNameFilterConverter = new MappedFieldNameFilterConverter();
        fieldNameFilterConverter.addFieldBitMask("test", (byte) 1);
        bufferedGeoMerger.buildMergedSegmentWithRetry(mergeInputBTrees, mergeState, newSegmentSize, 
                segmentWriteState, config, fieldNameFilterConverter);
        
        GeoSegmentReader<CartesianGeoRecord> myTree = new GeoSegmentReader<CartesianGeoRecord>(dir,
                newSegmentName + ".geo", 20, new IOContext(Context.MERGE),
                new CartesianGeoRecordSerializer(), new CartesianGeoRecordComparator());
        checkTreeAgainstExpected(myTree);
    }
    
    
}
