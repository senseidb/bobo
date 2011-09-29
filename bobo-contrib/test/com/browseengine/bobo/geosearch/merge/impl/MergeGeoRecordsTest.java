/**
 * 
 */
package com.browseengine.bobo.geosearch.merge.impl;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.lucene.util.BitVector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.impl.BTree;
import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.impl.GeoRecordBTree;
import com.browseengine.bobo.geosearch.impl.GeoRecordComparator;

/**
 * @author Ken McCracken
 *
 */
@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration( { "/TEST-servlet.xml" }) 
public class MergeGeoRecordsTest {
    
    private static final Logger LOGGER = Logger.getLogger(MergeGeoRecordsTest.class);

    IGeoConverter geoConverter;
    ChainedConvertedGeoRecordIterator mergeGeoRecords;
    
    GeoRecordComparator geoRecordCompareByBitMag;
    
    private LatitudeLongitudeDocId[] originalRaws;
    private GeoRecord[] originalGeoRecordsSortedArrayA;
    private GeoRecord[] originalGeoRecordsSortedArrayB;
    
    private GeoRecordBTree a;
    private BitVector aDelete;
    private GeoRecordBTree b;
    private BitVector bDelete;
    
    @Before
    public void setUp() throws Exception {
        geoRecordCompareByBitMag = new GeoRecordComparator();
        
        geoConverter = new GeoConverter();
        originalRaws = new LatitudeLongitudeDocId[] {
                new LatitudeLongitudeDocId(-70, -170, 0), // 0   0
                new LatitudeLongitudeDocId(-60, -160, 1), // 1   1
                new LatitudeLongitudeDocId(-50, -150, 2), // 2   2
                new LatitudeLongitudeDocId(-40, -140, 3), // X   X
                new LatitudeLongitudeDocId(-30, -130, 4), // 3   X
                new LatitudeLongitudeDocId(-20, -120, 5), // X   X
                new LatitudeLongitudeDocId(-10, -110, 6), // 4   3       
        };
        byte filterByte = GeoRecord.DEFAULT_FILTER_BYTE;
        
        originalGeoRecordsSortedArrayA = new GeoRecord[originalRaws.length];
        for (int i = 0; i < originalRaws.length; i++) {
            originalGeoRecordsSortedArrayA[i] = geoConverter.toGeoRecord(filterByte, originalRaws[i]);
        }
        
        originalGeoRecordsSortedArrayB = new GeoRecord[] {
                geoConverter.toGeoRecord(filterByte, new LatitudeLongitudeDocId(-65, -165, 0)),
                geoConverter.toGeoRecord(filterByte, new LatitudeLongitudeDocId(-55, -155, 1)),
                geoConverter.toGeoRecord(filterByte, new LatitudeLongitudeDocId(-45, -145, 2)),
                geoConverter.toGeoRecord(filterByte, new LatitudeLongitudeDocId(-35, -135, 3)),
                geoConverter.toGeoRecord(filterByte, new LatitudeLongitudeDocId(-25, -125, 4)),
                geoConverter.toGeoRecord(filterByte, new LatitudeLongitudeDocId(-15, -115, 5)),
                geoConverter.toGeoRecord(filterByte, new LatitudeLongitudeDocId(-5, -105, 6)),
        };
        
        random = new Random(SEED);
        
    }
    
    private void setUpA() throws IOException {
        a = getGeoRecordBTreeAsArray(originalGeoRecordsSortedArrayA);
        aDelete = new BitVector(a.getArrayLength());
    }
    
    private void setUpB() throws IOException {
        b = getGeoRecordBTreeAsArray(originalGeoRecordsSortedArrayB);
        bDelete = new BitVector(b.getArrayLength());
    }

    Random random ;
    
    private static final int MAX_NUMBER_OF_SOURCES_PER_DOCUMENT = 5;
    
    private int bNumberOfRecordsSurvivingDeletion;
    private int aNumberOfRecordsSurvivingDeletion;
    
    int absoluteDocIdOffsetInMergedPartition = 0;
    TreeSet<GeoRecord> expectedTreeSetInMergedPartition = null;
    Iterator<GeoRecord> expectedGeoRecordIteratorInMergedPartition = null;
    
    private void setUpRandomB(int maxDoc, float deletionRatio) throws IOException {
        PreMergedState preMergedState = randomGeoRecordBTreeAsArray(maxDoc, deletionRatio);
        b = preMergedState.geoRecordBTreeAsArray;
        bDelete = preMergedState.deletionVector;
        bNumberOfRecordsSurvivingDeletion = preMergedState.numberOfRecordsSurvivingDeletion;
    }
    
    private void setUpRandomA(int maxDoc, float deletionRatio) throws IOException {
        PreMergedState preMergedState = randomGeoRecordBTreeAsArray(maxDoc, deletionRatio);
        a = preMergedState.geoRecordBTreeAsArray;
        aDelete = preMergedState.deletionVector;
        aNumberOfRecordsSurvivingDeletion = preMergedState.numberOfRecordsSurvivingDeletion;
    }
    
    private static class PreMergedState {
        GeoRecordBTree geoRecordBTreeAsArray;
        BitVector deletionVector;
        int numberOfRecordsSurvivingDeletion;
        
        public PreMergedState(GeoRecordBTree geoRecordBTreeAsArray,
        BitVector deletionVector,
        int numberOfRecordsSurvivingDeletion) {
            this.geoRecordBTreeAsArray = geoRecordBTreeAsArray;
            this.deletionVector = deletionVector;
            this.numberOfRecordsSurvivingDeletion = numberOfRecordsSurvivingDeletion;
        }
    }
    
    private long SEED = 746606409241482554L;
    
    private PreMergedState randomGeoRecordBTreeAsArray(int maxDoc, float deletionRatio) throws IOException {
        BitVector bitVector = new BitVector(maxDoc);
        int numberOfRecordsSurvivingDeletion = 0;
        TreeSet<GeoRecord> treeSet = new TreeSet<GeoRecord>(comparator);
        for (int docid = 0; docid < maxDoc; docid++) {
            if (docid > 0 && deletionRatio > 0f) {
                double deleteValue = random.nextDouble();
                if (deleteValue <= deletionRatio) {
                    // delete
                    bitVector.set(docid);
                }
            }
            int numberOfSources = 1;
            if (docid > 0) {
                numberOfSources = random.nextInt(MAX_NUMBER_OF_SOURCES_PER_DOCUMENT);
            }
            boolean survives = !bitVector.get(docid);
            for (int sourceNumber = 0; sourceNumber < numberOfSources; sourceNumber++) {
                if (survives) {
                    numberOfRecordsSurvivingDeletion++;
                }
                addSource(treeSet, docid, survives);
            }
            if (survives) {
                // keep
                absoluteDocIdOffsetInMergedPartition++;
            }
        }
        
        GeoRecordBTree geoRecordBTreeAsArray = new GeoRecordBTree(treeSet);
        PreMergedState preMergedState = new PreMergedState(geoRecordBTreeAsArray, bitVector, numberOfRecordsSurvivingDeletion);
        return preMergedState;
    }
    
    private boolean useCannedGeoPositions = false;
    private int canned = 0;
    
    private final double[] LONGITUDES = {
            -65,
            -65.1,
            -65.2,
            -65.3,
            -65.4,
            -65.5,
            -65.6,
            -65.7,
    };
    
    private final double[] LATITUDES = {
            44,
            44.1,
            44.2,
            44.3,
            44.4,
            44.5,
            44.6,
            44.7,
    };

    
    private void addSource(TreeSet<GeoRecord> treeSet, int docid, boolean survives) {
        double longitude;
        double latitude;
        if (useCannedGeoPositions) {
            longitude = LONGITUDES[canned % LONGITUDES.length];
            latitude = LATITUDES[canned % LATITUDES.length];
            canned++;
        } else {
            longitude = (random.nextDouble() * 360.) - 180.;
            latitude = (random.nextDouble() * 180.) - 90.;
        }
        LatitudeLongitudeDocId raw = new LatitudeLongitudeDocId(latitude, 
                longitude, docid);
        GeoRecord geoRecord = geoConverter.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, raw);
        addSourceWithoutIncrementingAbsoluteDocIdOffsetInMergedPartition(treeSet, docid, survives, geoRecord);
    }
    
    private void addSourceWithoutIncrementingAbsoluteDocIdOffsetInMergedPartition(TreeSet<GeoRecord> treeSet, int docid, boolean survives, GeoRecord geoRecord) {
        treeSet.add(geoRecord);
        if (survives && null != expectedTreeSetInMergedPartition) {
            LatitudeLongitudeDocId rawBeforeMerge = geoConverter.toLongitudeLatitudeDocId(geoRecord);
            double longitude = rawBeforeMerge.longitude;
            double latitude = rawBeforeMerge.latitude;
            LatitudeLongitudeDocId mergedRaw = new LatitudeLongitudeDocId(latitude, longitude, absoluteDocIdOffsetInMergedPartition);
            geoRecord = geoConverter.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, mergedRaw);
            //verifyCycle(geoRecord);
            expectedTreeSetInMergedPartition.add(geoRecord);
        }
    }
    
    private void setUpABMergeGeoRecords() throws IOException {
        setUpA();

        setUpB();

        setUpMergedGeoRecords();
    }
    
    private void setUpMergedGeoRecords() throws IOException  {
        List<BTree<GeoRecord>> partitionList = new ArrayList<BTree<GeoRecord>>(2);
        List<BitVector> partitionDeletionList = new ArrayList<BitVector>(2);
        partitionList.add(a);
        partitionDeletionList.add(aDelete);
        partitionList.add(b);
        partitionDeletionList.add(bDelete);
        int totalBufferCapacity = 10000;
        mergeGeoRecords = new ChainedConvertedGeoRecordIterator(geoConverter, 
                partitionList, partitionDeletionList, totalBufferCapacity);
    }
    
    private static GeoRecordBTree getGeoRecordBTreeAsArray(
            GeoRecord[] geoRecordsSortedArray) throws IOException {
        GeoRecord[] treeSorted = new GeoRecord[geoRecordsSortedArray.length];
        long[] highOrders = new long[treeSorted.length];
        int[] lowOrders = new int[treeSorted.length];
        byte[] filterBytes = new byte[treeSorted.length];
        GeoRecordBTree tree = new GeoRecordBTree(highOrders, lowOrders, filterBytes);
        int height = tree.getHeight();
        int treeIndex = ((int)Math.pow(2, (height-1))) - 1;
        int i = 0;
        while (treeIndex >= 0) {
            GeoRecord geoRecord = geoRecordsSortedArray[i++];
            treeSorted[treeIndex] = geoRecord;
            highOrders[treeIndex] = geoRecord.highOrder;
            lowOrders[treeIndex] = geoRecord.lowOrder;
            filterBytes[treeIndex] = geoRecord.filterByte;
            
            treeIndex = tree.getNextIndex(treeIndex);
        }
        return tree;
    }
    
    @After
    public void tearDown() throws Exception {
        
    }
    
    List<Pair<Integer,Integer>> denseListOfLongitudeLatitudePairs;
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge" , "unit", "all" }) 
    public void test_dense_low_merges() throws Exception {
        this.resetMergedPartition();
        
        verifyDense(997, 47);
        verifyDense(1, 1);
        verifyDense(3, 3);
        verifyDense(13, 256);
        verifyDense(256, 13);
        verifyDense(509, 521);
        verifyDense(521, 509);
           }
    
    private void resetListOfDenseCoordinates(int max) {
        denseListOfLongitudeLatitudePairs = new ArrayList<Pair<Integer,Integer>>();
        for (int longitude = 0; longitude < max; longitude++) {
            for (int latitude = 0; latitude < max; latitude++) {
                Pair<Integer, Integer> pair = new Pair<Integer, Integer>(longitude, latitude);
                denseListOfLongitudeLatitudePairs.add(pair);
            }
        }

    }
    
    private void verifyDense(int maxDocA, int maxDocB) throws Exception {
        resetMergedPartition();
        
        int max = (int)(Math.sqrt(maxDocA + maxDocB)) + 1;
        resetListOfDenseCoordinates(max);
        
        PreMergedState preMerged;
        preMerged = getDensePartition(maxDocA);
        a = preMerged.geoRecordBTreeAsArray;
        aDelete = preMerged.deletionVector;
        aNumberOfRecordsSurvivingDeletion = preMerged.numberOfRecordsSurvivingDeletion;
        preMerged = getDensePartition(maxDocB);
        b = preMerged.geoRecordBTreeAsArray;
        bDelete = preMerged.deletionVector;
        bNumberOfRecordsSurvivingDeletion = preMerged.numberOfRecordsSurvivingDeletion;

        setUpMergedGeoRecords();

        int expectedCount = aNumberOfRecordsSurvivingDeletion 
            + bNumberOfRecordsSurvivingDeletion;
        verifyMerge(expectedCount);

    }
    
    private void addSource(TreeSet<GeoRecord> treeSet, boolean survives, 
            int docid, int longitudeCodedInt, int latitudeCodedInt) {
        int lowOrder = 0;
        int sourceBitNumber = 0;
        int lowOrderBitNumber = 0;
        if (getBitNumber(longitudeCodedInt, sourceBitNumber)) {
            lowOrder = setBitNumber(lowOrder, lowOrderBitNumber);
        }
        lowOrderBitNumber++;
        if (getBitNumber(docid, sourceBitNumber)) {
            lowOrder = setBitNumber(lowOrder, lowOrderBitNumber);
        }
        lowOrderBitNumber++;
        // loop
        while (sourceBitNumber < 10) {
            if (getBitNumber(latitudeCodedInt, sourceBitNumber-1)) {
                lowOrder = setBitNumber(lowOrder, lowOrderBitNumber);
            }
            lowOrderBitNumber++;

            sourceBitNumber++;
            if (getBitNumber(longitudeCodedInt, sourceBitNumber)) {
                lowOrder = setBitNumber(lowOrder, lowOrderBitNumber);
            }
            lowOrderBitNumber++;
            if (getBitNumber(docid, sourceBitNumber)) {
                lowOrder = setBitNumber(lowOrder, lowOrderBitNumber);
            }
            lowOrderBitNumber++;
        }
        System.out.println("lowOrder: "+GeoRecord.lpad(lowOrder));
        GeoRecord geoRecord = new GeoRecord(0L, lowOrder, (byte)0);
        verifyCycle(geoRecord);
        treeSet.add(geoRecord);
        addSourceWithoutIncrementingAbsoluteDocIdOffsetInMergedPartition(treeSet, docid, survives, geoRecord);
        absoluteDocIdOffsetInMergedPartition++;
    }
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "unit", "all" }) 
    public void test_dense_testItself() throws Exception {
        
        verify_dense_testItself(1023, 40, 40);

        verify_dense_testItself(0, 2, 0);
        
        verify_dense_testItself(0, 2, 2);

        verify_dense_testItself(0, 4, 4);
        verify_dense_testItself(0, 1, 1);
        
        final int max = 8;
        for (int docid = 0; docid < max; docid++) {
            for (int longitudeCodedInt = 0; longitudeCodedInt < max; longitudeCodedInt++) {
                for (int latitudeCodedInt = 0; latitudeCodedInt < max; latitudeCodedInt++) {
                    verify_dense_testItself(docid, longitudeCodedInt, latitudeCodedInt);
                }
            }
        }
    }
    
    private void verify_dense_testItself(int docid, int longitudeCodedInt, int latitudeCodedInt) {
        resetMergedPartition();
        TreeSet<GeoRecord> treeSet = new TreeSet<GeoRecord>(comparator);
        boolean survives = true;

        addSource( treeSet,  survives, 
                 docid,  longitudeCodedInt,  latitudeCodedInt);
        GeoRecord geoRecord = treeSet.pollFirst();
        LatitudeLongitudeDocId raw = geoConverter.toLongitudeLatitudeDocId(geoRecord);
        assertTrue("docid "+docid+" didn't match obtained raw.docid "+raw.docid, docid == raw.docid);
        if (0 == longitudeCodedInt) {
            assertTrue("longitudeCodedInt 0 didn't have longitude -180.", -180. == raw.longitude);
        }
        if (0 == latitudeCodedInt) {
            assertTrue("latitudeCodedInt 0 didn't have latitude -90.", -90. == raw.latitude);
        }
    }
    
    private PreMergedState getDensePartition(int maxDoc) throws IOException {
        BitVector bitVector = new BitVector(maxDoc);
        int numberOfRecordsSurvivingDeletion = maxDoc;
        TreeSet<GeoRecord> treeSet = new TreeSet<GeoRecord>(comparator);
        final boolean survives = true;
        for (int docid = 0; docid < maxDoc; docid++) {
            Pair<Integer, Integer> pair = denseListOfLongitudeLatitudePairs.remove(
                    random.nextInt(denseListOfLongitudeLatitudePairs.size()));
            int longitudeCodedInt = pair.one;
            int latitudeCodedInt = pair.two;
            addSource(treeSet, survives, docid, longitudeCodedInt, latitudeCodedInt);
         }
        
        GeoRecordBTree geoRecordBTreeAsArray = new GeoRecordBTree(treeSet);
        PreMergedState preMergedState = new PreMergedState(geoRecordBTreeAsArray, bitVector, numberOfRecordsSurvivingDeletion);
        return preMergedState;
    }
    
    private void verifyCycle(GeoRecord geoRecord) {
        LatitudeLongitudeDocId raw = geoConverter.toLongitudeLatitudeDocId(geoRecord);
        GeoRecord geoRecord2 = geoConverter.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, raw);
        boolean trueTest = null != geoRecord2 && geoRecord2.equals(geoRecord);
        if (!trueTest) {
        assertTrue("geoRecord2 "+geoRecord2+" != geoRecord "+geoRecord, 
        false);
        }
    }

    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "unit", "all" }) 
    public void test_geoConverter() {
        int docid = 0;
        LatitudeLongitudeDocId longitudeLatitudeDocId = new LatitudeLongitudeDocId(-90.,-180., docid);
        GeoRecord geoRecord = geoConverter.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, longitudeLatitudeDocId);
        this.verifyCycle(geoRecord);
        for (int i = 0; i < 32; i++) {
            geoRecord = new GeoRecord(0, i, GeoRecord.DEFAULT_FILTER_BYTE);
            verifyCycle(geoRecord);
        }
    }
    
    private boolean getBitNumber(int value, int bitNumber) {
        int bitMask = 1 << bitNumber;
        return (value & bitMask) != 0;
    }
    
    private int setBitNumber(int value, int bitNumber) {
        int bitMask = 1 << bitNumber;
        return (value ^ bitMask);
    }
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "unit", "all" }) 
    public void test_iteratorA() throws IOException {
        setUpABMergeGeoRecords();
        
        Iterator<GeoRecord> iteratorA = 
            new ConvertedGeoRecordIterator(geoConverter, a, 0, aDelete);
        assertTrue("iteratorA.hasNext() was false", iteratorA.hasNext());
        
        int i = 0;
        while (iteratorA.hasNext()) {
            GeoRecord geoRecord = iteratorA.next();
            LatitudeLongitudeDocId raw = geoConverter.toLongitudeLatitudeDocId(geoRecord);
            LatitudeLongitudeDocId original = this.originalRaws[i++];
            assertTrue("raw "+raw+" did not match original "+original, raw.equals(original));
        }
    }
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "unit", "all" }) 
    public void test_iteratorA_withDeletions() throws IOException {
        setUpA();
        
        aDelete.set(3);
        aDelete.set(5);
        Iterator<GeoRecord> iteratorA = 
            new ConvertedGeoRecordIterator(geoConverter, a, 0, aDelete);
        assertTrue("iteratorA.hasNext() was false", iteratorA.hasNext());
        
        int i = 0;
        while (iteratorA.hasNext()) {
            GeoRecord geoRecord = iteratorA.next();
            LatitudeLongitudeDocId raw = geoConverter.toLongitudeLatitudeDocId(geoRecord);
            // 3 and 5 have been deleted
            if (i == 3 || i == 5) {
                i++;
            }
            LatitudeLongitudeDocId original = this.originalRaws[i++];
            if (original.docid == 4) {
                original = original.clone();
                original.docid = 3;
            } else if (original.docid >= 6) {
                original = original.clone();
                original.docid -= 2;
            }
           
            assertTrue("raw "+raw+" did not match original "+original, raw.equals(original));
        }

    }
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "unit", "all" }) 
    public void test_iteratorA_withDeletions3() throws IOException {
        setUpA();
        
        aDelete.set(3);
        aDelete.set(4);
        aDelete.set(5);
        Iterator<GeoRecord> iteratorA = 
            new ConvertedGeoRecordIterator(geoConverter, a, 0, aDelete);
        assertTrue("iteratorA.hasNext() was false", iteratorA.hasNext());
        
        int i = 0;
        boolean skipped3 = false;
        while (iteratorA.hasNext()) {
            GeoRecord geoRecord = iteratorA.next();
            LatitudeLongitudeDocId raw = geoConverter.toLongitudeLatitudeDocId(geoRecord);
            // 3, 4, and 5 have been deleted
            if (i == 3) {
                i = 6;
                skipped3 = true;
            }
            LatitudeLongitudeDocId original = this.originalRaws[i++];
            if (skipped3) {
                original = original.clone();
                original.docid -= 3;
            }
           
            assertTrue("raw "+raw+" did not match original "+original, raw.equals(original));
        }

    }
    
    GeoRecordComparator comparator = new GeoRecordComparator();
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "performance", "all" }) 
    public void test_random_iteratorB_increases() throws Exception {
        for (int numDocsInB = 1; numDocsInB < 19; numDocsInB++) {
            verifyRandom(numDocsInB);
        }
        for (int i = 0; i < PRIMES.length; i++) {
            int numDocsInB = PRIMES[i] - 1;
            verifyRandom(numDocsInB);
            numDocsInB ++;
            verifyRandom(numDocsInB);
            numDocsInB ++;
            verifyRandom(numDocsInB);
        }
    }
    
    private final int[] PRIMES = {
            2,   3,   5,   7,  11,  13,  17,  19,  23,  29 
            , 31,  37,  41,  43,  47,  53,  59,  61,  67,  71 
            , 73,  79,  83,  89,  97, 101, 103, 107, 109, 113 
            , 127, 131, 137, 139, 149, 151, 157, 163, 167, 173 
            , 179, 181, 191, 193, 197, 199, 211, 223, 227, 229 
            , 233, 239, 241, 251, 257, 263, 269, 271, 277, 281 
            , 283, 293, 307, 311, 313, 317, 331, 337, 347, 349 
            , 353, 359, 367, 373, 379, 383, 389, 397, 401, 409 
            , 419, 421, 431, 433, 439, 443, 449, 457, 461, 463 
            , 467, 479, 487, 491, 499, 503, 509, 521, 523, 541 
            , 547, 557, 563, 569, 571, 577, 587, 593, 599, 601 
            , 607, 613, 617, 619, 631, 641, 643, 647, 653, 659 
            , 661, 673, 677, 683, 691, 701, 709, 719, 727, 733 
            , 739, 743, 751, 757, 761, 769, 773, 787, 797, 809 
            , 811, 821, 823, 827, 829, 839, 853, 857, 859, 863 
            , 877, 881, 883, 887, 907, 911, 919, 929, 937, 941 
            , 947, 953, 967, 971, 977, 983, 991, 997, 1009, 1013 
 };

    private void verifyRandom(int numDocsInB) throws IOException {
        setUpRandomB(numDocsInB, 0f);
            
        for (int numDocsInA = 1; numDocsInA < 19; numDocsInA++) {
            verifyBIncreases(numDocsInA);
        }
        
        for (int powerOf2 = 32; powerOf2 < 65536; powerOf2 *= 2) {
            int numDocsInA = random.nextInt(powerOf2) + powerOf2;
            verifyBIncreases(numDocsInA);
        }
        
        LOGGER.debug("no errors verifyRandom numDocsInB = "+numDocsInB);
    }
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "unit", "all" }) 
    public void test_fixedIteratorB_increases() throws Exception {
        setUpB();
        
        verifyBIncreases(7);
    }
    
    private void verifyBIncreases(int maxDocIdInA) throws IOException {
        int minDocIdInMergedPartitionFromB = maxDocIdInA + 1;
        
        Iterator<GeoRecord> iteratorB = 
            new ConvertedGeoRecordIterator( geoConverter, b, minDocIdInMergedPartitionFromB, bDelete);
        
        assertTrue("minDocIdInMergedPartitionFromB "+minDocIdInMergedPartitionFromB
                +", iteratorB.hasNext() was false, b.getArrayLength() "+b.getArrayLength(), 
                iteratorB.hasNext());
        
        int numDocsInB = b.getArrayLength();
        int i = 0;
        GeoRecord previous = iteratorB.next();
        print(previous);
        i++;
        while (iteratorB.hasNext()) {
            GeoRecord geoRecord = iteratorB.next();
            print(geoRecord);
            int comparison = comparator.compare(geoRecord, previous);
            assertTrue("i "+i+", compare(geoRecord "+geoRecord+", previous "+previous+"), comparison "+comparison, 
                    comparison > 0);
            i++;
            previous = geoRecord;
        }
        assertTrue("didn't get "+numDocsInB+" records from iteratorB, got "+i, i == numDocsInB);

    }

    private void print(GeoRecord geoRecord) {
        /*
        LongitudeLatitudeDocId geoRecordRaw = geoConverter.toLongitudeLatitudeDocId(geoRecord);
        System.out.println("geoRecord "+geoRecord+", geoRecordRaw: "+geoRecordRaw);
        */
    }
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "unit", "all" }) 
    public void test_random_merge() throws Exception {
        verifyRandomMerges(true);
    }
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "performance", "all" }) 
    public void test_random_merge_performance() throws Exception {
        verifyRandomMerges(false);
    }

    private void verifyRandomMerges(boolean unit) throws Exception {
        int ndocs = 1023;
        float deletionRatio = 0f;
        verifyRandomMerge(ndocs, deletionRatio, ndocs, deletionRatio);
        deletionRatio = 0.01f;
        verifyRandomMerge(ndocs, deletionRatio, ndocs, deletionRatio);
        if (unit) {
            return;
        }
        ndocs = 65335;
        verifyRandomMerge(ndocs, deletionRatio, ndocs, deletionRatio);
        verifyRandomMerge(ndocs, deletionRatio, ndocs, 0.1f);

        useCannedGeoPositions = true;
        verifyRandomMerge(ndocs, deletionRatio, ndocs, 0.1f);

    }
    
    private void resetMergedPartition() {
        expectedTreeSetInMergedPartition = new TreeSet<GeoRecord>(comparator);
        absoluteDocIdOffsetInMergedPartition = 0;
        expectedGeoRecordIteratorInMergedPartition = null;

    }
    
    private void verifyRandomMerge(int maxDocA, float deletionRatioA, int maxDocB, float deletionRatioB) throws Exception {
        resetMergedPartition();
        
        setUpRandomA(maxDocA, deletionRatioA);
        setUpRandomB(maxDocB, deletionRatioB);
        setUpMergedGeoRecords();

        int expectedCount = aNumberOfRecordsSurvivingDeletion 
            + bNumberOfRecordsSurvivingDeletion;
        verifyMerge(expectedCount);
    }
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "unit", "all" }) 
    public void test_merge() throws Exception {
        setUpABMergeGeoRecords();
        
        int expectedCount = 14;

        verifyMerge(expectedCount);
    }
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "unit", "all" }) 
    public void test_orderedIteratorChain() throws Exception {
        LatitudeLongitudeDocId raw = new LatitudeLongitudeDocId(45, -65, 0);
        GeoRecord geoRecordOne = geoConverter.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, raw);
        OneGeoRecordIterator one = new OneGeoRecordIterator(geoRecordOne);
        raw = new LatitudeLongitudeDocId(46, -66, 1);
        GeoRecord geoRecordTwo = geoConverter.toGeoRecord(GeoRecord.DEFAULT_FILTER_BYTE, raw);
        OneGeoRecordIterator two = new OneGeoRecordIterator(geoRecordTwo);
        List<Iterator<GeoRecord>> list = new ArrayList<Iterator<GeoRecord>>();
        list.add(one);
        list.add(two);
        OrderedIteratorChain<GeoRecord> chain = new OrderedIteratorChain<GeoRecord>(
                list,
               comparator
                );
        assertTrue("chain.hasNext() was false", chain.hasNext());
        GeoRecord actual = chain.next();
        assertTrue("actual "+actual+" did not match geoRecordOne "+geoRecordOne, null != actual && actual.equals(geoRecordOne));
        assertTrue("chain.hasNext() was false", chain.hasNext());
        actual = chain.next();
        assertTrue("actual "+actual+" did not match geoRecordTwo "+geoRecordTwo, null != actual && actual.equals(geoRecordTwo));
        assertTrue("chain.hasNext() was true", !chain.hasNext());
        
    }
    
    private static class OneGeoRecordIterator implements Iterator<GeoRecord> {
        private GeoRecord one;
        
        public OneGeoRecordIterator(GeoRecord one) {
            this.one = one;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return one != null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public GeoRecord next() {
            GeoRecord tmp = one;
            one = null;
            return tmp;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
        
    }
    
    private void verifyNextGeoRecordInExpectedMergeIndex(GeoRecord geoRecord) {
        if (null == expectedTreeSetInMergedPartition) {
            return;
        }
        if (null == expectedGeoRecordIteratorInMergedPartition) {
            expectedGeoRecordIteratorInMergedPartition = expectedTreeSetInMergedPartition.iterator();
        }
        assertTrue("geoRecord found, but expected hasNext() was false", 
                expectedGeoRecordIteratorInMergedPartition.hasNext());
        GeoRecord expected = expectedGeoRecordIteratorInMergedPartition.next();
        LatitudeLongitudeDocId expectedRaw = geoConverter.toLongitudeLatitudeDocId(expected);
        LatitudeLongitudeDocId actualRaw = geoConverter.toLongitudeLatitudeDocId(geoRecord);
        assertTrue("expected "+expected+" didn't match actual "+geoRecord+"; expectedRaw "+expectedRaw+", actualRaw "+actualRaw, 
                null != expected && expected.equals(geoRecord));
    }
    
    private void verifyMerge(int expectedCount) throws Exception {
        
        assertTrue("mergeGeoRecords.hasNext() was false", mergeGeoRecords.hasNext());
        GeoRecord geoRecord = mergeGeoRecords.next();
        print(geoRecord);
        
        verifyNextGeoRecordInExpectedMergeIndex(geoRecord);

        assertTrue("geoRecord first hit was null", geoRecord !=null);
        int count = 1;
        while (mergeGeoRecords.hasNext()) {
            GeoRecord next = mergeGeoRecords.next();
            count++;
            print(next);

            assertTrue("geoRecord count "+count+" on iterator was null", null != next);
            int comparison = geoRecordCompareByBitMag.compare(geoRecord, next);
            assertTrue("geoRecord count "+count+" "+geoRecord+" was not less than next "+next, 
                    comparison < 0);
            verifyNextGeoRecordInExpectedMergeIndex(next);

           geoRecord = next;
        }
        assertTrue("expectedCount "+expectedCount+" != actual count "+count, 
                expectedCount == count);
        
        System.out.println("successful merge of "+expectedCount+" simulated");
    }
}
