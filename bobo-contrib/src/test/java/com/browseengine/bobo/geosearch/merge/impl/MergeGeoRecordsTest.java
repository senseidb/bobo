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

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.impl.BTree;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordComparator;
import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.impl.GeoRecordBTree;
import com.browseengine.bobo.geosearch.score.impl.Conversions;

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
    
    CartesianGeoRecordComparator geoRecordCompareByBitMag;
    
    private CartesianCoordinateDocId[] originalRaws;
    private CartesianGeoRecord[] originalGeoRecordsSortedArrayA;
    private CartesianGeoRecord[] originalGeoRecordsSortedArrayB;
    
    private GeoRecordBTree a;
    private BitVector aDelete;
    private GeoRecordBTree b;
    private BitVector bDelete;
    
    @Before
    public void setUp() throws Exception {
        geoRecordCompareByBitMag = new CartesianGeoRecordComparator();
        
        geoConverter = new GeoConverter();
        originalRaws = new CartesianCoordinateDocId[] {
                new CartesianCoordinateDocId(-700000000, -700000000, -700000000, 0), // 0   0
                new CartesianCoordinateDocId(-600000000, -600000000, -600000000, 1), // 1   1
                new CartesianCoordinateDocId(-500000000, -500000000, -500000000, 2), // 2   2
                new CartesianCoordinateDocId(-400000000, -400000000, -400000000, 3), // X   X
                new CartesianCoordinateDocId(-300000000, -300000000, -300000000, 4), // 3   X
                new CartesianCoordinateDocId(-200000000, -200000000, -200000000, 5), // X   X
                new CartesianCoordinateDocId(-100000000, -100000000, -100000000, 6), // 4   3       
        };
        byte filterByte = CartesianGeoRecord.DEFAULT_FILTER_BYTE;
        
        originalGeoRecordsSortedArrayA = new CartesianGeoRecord[originalRaws.length];
        for (int i = 0; i < originalRaws.length; i++) {
            originalGeoRecordsSortedArrayA[i] = geoConverter.toCartesianGeoRecord(originalRaws[i], filterByte);
        }
        
        originalGeoRecordsSortedArrayB = new CartesianGeoRecord[] {
                geoConverter.toCartesianGeoRecord(new CartesianCoordinateDocId(-650000000, -650000000, -650000000, 0), filterByte),
                geoConverter.toCartesianGeoRecord(new CartesianCoordinateDocId(-550000000, -550000000, -550000000, 1), filterByte),
                geoConverter.toCartesianGeoRecord(new CartesianCoordinateDocId(-450000000, -450000000, -450000000, 2), filterByte),
                geoConverter.toCartesianGeoRecord(new CartesianCoordinateDocId(-350000000, -350000000, -350000000, 3), filterByte),
                geoConverter.toCartesianGeoRecord(new CartesianCoordinateDocId(-250000000, -250000000, -250000000, 4), filterByte),
                geoConverter.toCartesianGeoRecord(new CartesianCoordinateDocId(-150000000, -150000000, -150000000, 5), filterByte),
                geoConverter.toCartesianGeoRecord(new CartesianCoordinateDocId(-50000000, -50000000, -50000000, 6), filterByte),
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
    TreeSet<CartesianGeoRecord> expectedTreeSetInMergedPartition = null;
    Iterator<CartesianGeoRecord> expectedGeoRecordIteratorInMergedPartition = null;
    
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
    
    private final long SEED = 746606409241482554L;
    
    private PreMergedState randomGeoRecordBTreeAsArray(int maxDoc, float deletionRatio) throws IOException {
        BitVector bitVector = new BitVector(maxDoc);
        int numberOfRecordsSurvivingDeletion = 0;
        TreeSet<CartesianGeoRecord> treeSet = new TreeSet<CartesianGeoRecord>(comparator);
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

    
    private void addSource(TreeSet<CartesianGeoRecord> treeSet, int docid, boolean survives) {
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
        CartesianGeoRecord geoRecord = geoConverter.toCartesianGeoRecord(raw, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
        addSourceWithoutIncrementingAbsoluteDocIdOffsetInMergedPartition(treeSet, docid, survives, geoRecord);
    }
    
    private void addSourceWithoutIncrementingAbsoluteDocIdOffsetInMergedPartition(TreeSet<CartesianGeoRecord> treeSet, int docid, boolean survives, CartesianGeoRecord geoRecord) {
        treeSet.add(geoRecord);
        if (survives && null != expectedTreeSetInMergedPartition) {
            CartesianCoordinateDocId rawBeforeMerge = geoConverter.toCartesianCoordinateDocId(geoRecord);
            CartesianCoordinateDocId mergedRaw = new CartesianCoordinateDocId(rawBeforeMerge.x, rawBeforeMerge.y, rawBeforeMerge.z, absoluteDocIdOffsetInMergedPartition);
            geoRecord = geoConverter.toCartesianGeoRecord(mergedRaw, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
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
        List<BTree<CartesianGeoRecord>> partitionList = new ArrayList<BTree<CartesianGeoRecord>>(2);
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
            CartesianGeoRecord[] geoRecordsSortedArray) throws IOException {
        CartesianGeoRecord[] treeSorted = new CartesianGeoRecord[geoRecordsSortedArray.length];
        long[] highOrders = new long[treeSorted.length];
        long[] lowOrders = new long[treeSorted.length];
        byte[] filterBytes = new byte[treeSorted.length];
        GeoRecordBTree tree = new GeoRecordBTree(highOrders, lowOrders, filterBytes);
        int height = tree.getHeight();
        int treeIndex = ((int)Math.pow(2, (height-1))) - 1;
        int i = 0;
        while (treeIndex >= 0) {
            CartesianGeoRecord geoRecord = geoRecordsSortedArray[i++];
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
    
    //List of dense x,y,z values.  DocId has no meaning in this list and is always 0.
    List<CartesianCoordinateDocId> denseListOfLongitudeLatitudePairs;
    
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
        denseListOfLongitudeLatitudePairs = new ArrayList<CartesianCoordinateDocId>();
        for (int x = 0; x < max; x++) {
            for (int y = 0; y < max; y++) {
                for (int z = 0; z < max; z++) {
                    CartesianCoordinateDocId cartesianCoordinateDocId = new CartesianCoordinateDocId(x, y, z, 0);
                    denseListOfLongitudeLatitudePairs.add(cartesianCoordinateDocId);
                }
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
    
    private void addSource(TreeSet<CartesianGeoRecord> treeSet, boolean survives, 
            int docid, int x, int y, int z) {
        CartesianGeoRecord geoRecord = geoConverter.toCartesianGeoRecord(new CartesianCoordinateDocId(x, y, z, docid), (byte) 0);
        verifyCycle(geoRecord);
        treeSet.add(geoRecord);
        addSourceWithoutIncrementingAbsoluteDocIdOffsetInMergedPartition(treeSet, docid, survives, geoRecord);
        absoluteDocIdOffsetInMergedPartition++;
    }
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "unit", "all" }) 
    public void test_dense_testItself() throws Exception {
        
        verify_dense_testItself(1023, 40, 40, 40);

        verify_dense_testItself(0, 4, 2, 0);
        
        verify_dense_testItself(0, 2, 2, 2);

        verify_dense_testItself(0, 4, 4, 4);
        verify_dense_testItself(0, 1, 1, 1);
        
        final int max = 8;
        for (int docid = 0; docid < max; docid++) {
            for (int x = 0; x < max; x++) {
                for (int y = 0; y < max; y++) {
                    for (int z = 0; z < max; z++) {
                        verify_dense_testItself(docid, x, y, z);
                    }
                }
            }
        }
    }

    private void verify_dense_testItself(int docid, int x, int y, int z) {
        resetMergedPartition();
        TreeSet<CartesianGeoRecord> treeSet = new TreeSet<CartesianGeoRecord>(comparator);
        boolean survives = true;

        addSource( treeSet,  survives, 
                 docid,  x,  y, z);
        CartesianGeoRecord geoRecord = treeSet.pollFirst();
        CartesianCoordinateDocId raw = geoConverter.toCartesianCoordinateDocId(geoRecord);
        assertTrue("docid "+docid+" didn't match obtained raw.docid "+raw.docid, docid == raw.docid);
        if (0 == y) {
//            assertTrue("longitudeCodedInt 0 didn't have longitude -180.", -180. == raw.longitude);
           assertTrue("longitudeCodedInt 0 didn't have longitude -180.", -180 != (int)Conversions.r2d(Math.atan((double)raw.y/(double)raw.x)));
        }
        if (0 == z) {
//            assertTrue("latitudeCodedInt 0 didn't have latitude -90.", -90. == raw.latitude);
            assertTrue("latitudeCodedInt 0 didn't have latitude -90.", -90 != (int)Conversions.r2d(Math.asin((double)raw.z/(double)Conversions.EARTH_RADIUS_INTEGER_UNITS)));
        }
    }
    
    private PreMergedState getDensePartition(int maxDoc) throws IOException {
        BitVector bitVector = new BitVector(maxDoc);
        int numberOfRecordsSurvivingDeletion = maxDoc;
        TreeSet<CartesianGeoRecord> treeSet = new TreeSet<CartesianGeoRecord>(comparator);
        final boolean survives = true;
        for (int docid = 0; docid < maxDoc; docid++) {
            CartesianCoordinateDocId coordinate = denseListOfLongitudeLatitudePairs.remove(
                    random.nextInt(denseListOfLongitudeLatitudePairs.size()));
            addSource(treeSet, survives, docid, coordinate.x, coordinate.y, coordinate.z);
        }
        
        GeoRecordBTree geoRecordBTreeAsArray = new GeoRecordBTree(treeSet);
        PreMergedState preMergedState = new PreMergedState(geoRecordBTreeAsArray, bitVector, numberOfRecordsSurvivingDeletion);
        return preMergedState;
    }
    
    private void verifyCycle(CartesianGeoRecord geoRecord) {
        CartesianCoordinateDocId raw = geoConverter.toCartesianCoordinateDocId(geoRecord);
        CartesianGeoRecord geoRecord2 = geoConverter.toCartesianGeoRecord(raw, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
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
        CartesianGeoRecord geoRecord = geoConverter.toCartesianGeoRecord(longitudeLatitudeDocId, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
        this.verifyCycle(geoRecord);
        for (int i = 0; i < 32; i++) {
            geoRecord = new CartesianGeoRecord(0, i, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
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
        
        Iterator<CartesianGeoRecord> iteratorA = 
            new ConvertedGeoRecordIterator(geoConverter, a, 0, aDelete);
        assertTrue("iteratorA.hasNext() was false", iteratorA.hasNext());
        
        int i = 0;
        while (iteratorA.hasNext()) {
            CartesianGeoRecord geoRecord = iteratorA.next();
            CartesianCoordinateDocId raw = geoConverter.toCartesianCoordinateDocId(geoRecord);
            CartesianCoordinateDocId original = this.originalRaws[i++];
            assertTrue("raw "+raw+" did not match original "+original, raw.equals(original));
        }
    }
    
    @Test
    @IfProfileValue(name = "test-suite", values = { "merge", "unit", "all" }) 
    public void test_iteratorA_withDeletions() throws IOException {
        setUpA();
        
        aDelete.set(3);
        aDelete.set(5);
        Iterator<CartesianGeoRecord> iteratorA = 
            new ConvertedGeoRecordIterator(geoConverter, a, 0, aDelete);
        assertTrue("iteratorA.hasNext() was false", iteratorA.hasNext());
        
        int i = 0;
        while (iteratorA.hasNext()) {
            CartesianGeoRecord geoRecord = iteratorA.next();
            CartesianCoordinateDocId raw = geoConverter.toCartesianCoordinateDocId(geoRecord);
            // 3 and 5 have been deleted
            if (i == 3 || i == 5) {
                i++;
            }
            CartesianCoordinateDocId original = this.originalRaws[i++];
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
        Iterator<CartesianGeoRecord> iteratorA = 
            new ConvertedGeoRecordIterator(geoConverter, a, 0, aDelete);
        assertTrue("iteratorA.hasNext() was false", iteratorA.hasNext());
        
        int i = 0;
        boolean skipped3 = false;
        while (iteratorA.hasNext()) {
            CartesianGeoRecord geoRecord = iteratorA.next();
            CartesianCoordinateDocId raw = geoConverter.toCartesianCoordinateDocId(geoRecord);
            // 3, 4, and 5 have been deleted
            if (i == 3) {
                i = 6;
                skipped3 = true;
            }
            CartesianCoordinateDocId original = this.originalRaws[i++];
            if (skipped3) {
                original = original.clone();
                original.docid -= 3;
            }
           
            assertTrue("raw "+raw+" did not match original "+original, raw.equals(original));
        }
    }
    
    CartesianGeoRecordComparator comparator = new CartesianGeoRecordComparator();
    
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
        
        Iterator<CartesianGeoRecord> iteratorB = 
            new ConvertedGeoRecordIterator( geoConverter, b, minDocIdInMergedPartitionFromB, bDelete);
        
        assertTrue("minDocIdInMergedPartitionFromB "+minDocIdInMergedPartitionFromB
                +", iteratorB.hasNext() was false, b.getArrayLength() "+b.getArrayLength(), 
                iteratorB.hasNext());
        
        int numDocsInB = b.getArrayLength();
        int i = 0;
        CartesianGeoRecord previous = iteratorB.next();
        print(previous);
        i++;
        while (iteratorB.hasNext()) {
            CartesianGeoRecord geoRecord = iteratorB.next();
            print(geoRecord);
            int comparison = comparator.compare(geoRecord, previous);
            assertTrue("i "+i+", compare(geoRecord "+geoRecord+", previous "+previous+"), comparison "+comparison, 
                    comparison > 0);
            i++;
            previous = geoRecord;
        }
        assertTrue("didn't get "+numDocsInB+" records from iteratorB, got "+i, i == numDocsInB);

    }

    private void print(CartesianGeoRecord geoRecord) {
        CartesianCoordinateDocId geoRecordRaw = geoConverter.toCartesianCoordinateDocId(geoRecord);
        System.out.println("geoRecord "+geoRecord+", geoRecordRaw: "+geoRecordRaw);
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
        expectedTreeSetInMergedPartition = new TreeSet<CartesianGeoRecord>(comparator);
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
        CartesianCoordinateDocId raw = new CartesianCoordinateDocId(450000000, -650000000, 450000000, 0);
        CartesianGeoRecord geoRecordOne = geoConverter.toCartesianGeoRecord(raw, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
        OneGeoRecordIterator one = new OneGeoRecordIterator(geoRecordOne);
        raw = new CartesianCoordinateDocId(460000000, -660000000, 450000000, 1);
        CartesianGeoRecord geoRecordTwo = geoConverter.toCartesianGeoRecord(raw, CartesianGeoRecord.DEFAULT_FILTER_BYTE);
        OneGeoRecordIterator two = new OneGeoRecordIterator(geoRecordTwo);
        List<Iterator<CartesianGeoRecord>> list = new ArrayList<Iterator<CartesianGeoRecord>>();
        list.add(one);
        list.add(two);
        OrderedIteratorChain<CartesianGeoRecord> chain = new OrderedIteratorChain<CartesianGeoRecord>(
                list,
               comparator
                );
        assertTrue("chain.hasNext() was false", chain.hasNext());
        CartesianGeoRecord actual = chain.next();
        assertTrue("actual "+actual+" did not match geoRecordOne "+geoRecordOne, null != actual && actual.equals(geoRecordOne));
        assertTrue("chain.hasNext() was false", chain.hasNext());
        actual = chain.next();
        assertTrue("actual "+actual+" did not match geoRecordTwo "+geoRecordTwo, null != actual && actual.equals(geoRecordTwo));
        assertTrue("chain.hasNext() was true", !chain.hasNext());
        
    }
    
    private static class OneGeoRecordIterator implements Iterator<CartesianGeoRecord> {
        private CartesianGeoRecord one;
        
        public OneGeoRecordIterator(CartesianGeoRecord one) {
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
        public CartesianGeoRecord next() {
            CartesianGeoRecord tmp = one;
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
    
    private void verifyNextGeoRecordInExpectedMergeIndex(CartesianGeoRecord geoRecord) {
        if (null == expectedTreeSetInMergedPartition) {
            return;
        }
        if (null == expectedGeoRecordIteratorInMergedPartition) {
            expectedGeoRecordIteratorInMergedPartition = expectedTreeSetInMergedPartition.iterator();
        }
        assertTrue("geoRecord found, but expected hasNext() was false", 
                expectedGeoRecordIteratorInMergedPartition.hasNext());
        CartesianGeoRecord expected = expectedGeoRecordIteratorInMergedPartition.next();
        CartesianCoordinateDocId expectedRaw = geoConverter.toCartesianCoordinateDocId(expected);
        CartesianCoordinateDocId actualRaw = geoConverter.toCartesianCoordinateDocId(geoRecord);
        assertTrue("expected "+expected+" didn't match actual "+geoRecord+"; expectedRaw "+expectedRaw+", actualRaw "+actualRaw, 
                null != expected && expected.equals(geoRecord));
    }
    
    private void verifyMerge(int expectedCount) throws Exception {
        
        assertTrue("mergeGeoRecords.hasNext() was false", mergeGeoRecords.hasNext());
        CartesianGeoRecord geoRecord = mergeGeoRecords.next();
        print(geoRecord);
        
        verifyNextGeoRecordInExpectedMergeIndex(geoRecord);

        assertTrue("geoRecord first hit was null", geoRecord != null);
        int count = 1;
        while (mergeGeoRecords.hasNext()) {
            CartesianGeoRecord next = mergeGeoRecords.next();
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
