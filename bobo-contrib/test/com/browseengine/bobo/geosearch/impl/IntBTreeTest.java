/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.browseengine.bobo.geosearch.impl.IntBTree;

/**
 * @author Ken McCracken
 *
 */
public class IntBTreeTest {
    
    private int[] tree = {
      7,
      3,
      11,
      1,
      5,
      9,
      13,
      0,
      2,
      4,
      6,
      8,
      10,
      12,
      14
    };
    
    private IntBTree treeAsArray;
    
    @Before
    public void setUp() {
        treeAsArray = new IntBTree(tree);
    }
    
    @Test
    public void test_ranges_allLeavesNull() throws IOException{
        for (int i = 7; i < tree.length; i++) {
            tree[i] = IntBTree.NULL_NODE_VALUE;
        }
        verifyRanges();
    }
    
    @Test
    public void test_depthOf() {
        verifyDepthOf(1, 1);

        verifyDepthOf(0, 0);
        verifyDepthOf(1, 1);
        verifyDepthOf(2, 1);
        verifyDepthOf(3, 2);
        verifyDepthOf(4, 2);
        verifyDepthOf(5, 2);
        verifyDepthOf(6, 2);
        verifyDepthOf(7, 3);
        verifyDepthOf(8, 3);
        verifyDepthOf(9, 3);
        verifyDepthOf(10, 3);
        verifyDepthOf(11, 3);
        verifyDepthOf(12, 3);
        verifyDepthOf(13, 3);
        verifyDepthOf(14, 3);
             
    }
    
    private void verifyDepthOf(int index, final int expectedDepth) {
        int actualDepth = treeAsArray.getDepthOf(index);
        assertTrue("index "+index+", expectedDepth "+expectedDepth+", actualDepth "+actualDepth,
                expectedDepth == actualDepth);
    }
    
    @Test
    public void test_countNodes() throws IOException {
        verifyCounts();
    }
    
    @Test
    public void test_ranges_rightLeavesNull()throws IOException {
        for (int i = 11; i < tree.length; i++) {
            tree[i] = IntBTree.NULL_NODE_VALUE;
        }
        verifyRanges();
    }
    
    @Test
    public void test_ranges_leftLeavesNull()throws IOException {
        for (int i = 7; i < 10; i++) {
            tree[i] = IntBTree.NULL_NODE_VALUE;
        }
        verifyRanges();
    }


    
    int nullIndex;
    
    @Test
    public void test_ranges_7null() throws IOException{
        nullIndex = 7;
        setNullIndexAndVerifyRanges();
    }
    
    @Test
    public void test_ranges_8null()throws IOException {
        nullIndex = 8;
        setNullIndexAndVerifyRanges();
    }
    
    @Test
    public void test_ranges_9null() throws IOException{
        nullIndex = 9;
        setNullIndexAndVerifyRanges();
    }

    @Test
    public void test_ranges_10null()throws IOException {
        nullIndex = 10;
        setNullIndexAndVerifyRanges();
    }

    @Test
    public void test_ranges_11null() throws IOException{
        nullIndex = 11;
        setNullIndexAndVerifyRanges();
    }

    
    @Test
    public void test_ranges_12null() throws IOException{
        nullIndex = 12;
        setNullIndexAndVerifyRanges();
    }

    @Test
    public void test_ranges_13null()throws IOException {
        nullIndex = 13;
        setNullIndexAndVerifyRanges();
    }

    @Test
    public void test_ranges_14null() throws IOException {
        nullIndex = 14;
        setNullIndexAndVerifyRanges();
    }

    private void setNullIndexAndVerifyRanges() throws IOException {
        tree[nullIndex] = IntBTree.NULL_NODE_VALUE;
        verifyRanges();
    }

 
    @Test
    public void test_arrayListIterator() {
        List<Integer> list = new ArrayList<Integer>();
        Iterator<Integer> iterator = list.iterator();
        assertTrue("iterator.next() should have been false on an empty list", !iterator.hasNext());
    }
    
    @Test
    public void test_ranges() throws IOException {
        verifyRanges();
    }
    
    private void verifyCounts() throws IOException {
        // if the ranges are correct, then the counts should also be correct
        // otherwise, we should revisit our testing mechanism
        fillValidInts();
        verifyCount(2, 4);
        
        final int lengthOfTree = tree.length;
        for (int minValue = 0; minValue < lengthOfTree; minValue++) {
            for (int maxValue = minValue; maxValue < lengthOfTree; maxValue++) {
                verifyCount(minValue, maxValue);
            }
        }

    }
    
    private void verifyCount(int minValue, int maxValue) throws IOException {
        verifyRange(minValue, maxValue);
        verifyCountAssumingRangeIteratorIsCorrect(minValue, maxValue);
    }
    
    private void verifyCountAssumingRangeIteratorIsCorrect(int minValue, int maxValue) throws IOException {
        Iterator<Integer> iterator = treeAsArray.getIterator(minValue, maxValue);

        int expectedCount = 0;
        while (iterator.hasNext()) {
            expectedCount++;
            iterator.next();
        }
        
        int actualCountUpperBound = treeAsArray.getUpperBoundOnNumberOfNodes(minValue, maxValue);
        assertTrue("counts didn't match, minValue "+minValue+", maxValue "+maxValue+", expectedCount "+expectedCount+", actualCountUpperBound "+actualCountUpperBound, 
                expectedCount == actualCountUpperBound);
    }
    
    private void verifyRanges() throws IOException {
        fillValidInts();
        verifyRange(2, 2);
        final int lengthOfTree = tree.length;
        for (int minValue = 0; minValue < lengthOfTree; minValue++) {
            for (int maxValue = minValue; maxValue < lengthOfTree; maxValue++) {
                verifyRange(minValue, maxValue);
            }
        }
    }
    
    @Test
    public void test_leftIndex() throws IOException {
        final int lengthOfTree = tree.length;
        for (int minValue = 0; minValue < lengthOfTree; minValue++) {
            for (int maxValue = minValue; maxValue < lengthOfTree; maxValue++) {
                verifyLeftIndex(minValue, maxValue);
            }
        }

    }
    
    @Test
    public void test_rightIndex() throws IOException {
        final int lengthOfTree = tree.length;
        for (int minValue = 0; minValue < lengthOfTree; minValue++) {
            for (int maxValue = minValue; maxValue < lengthOfTree; maxValue++) {
                verifyRightIndex(minValue, maxValue);
            }
        }

    }

    private void verifyRightIndex(int minValue, int maxValue) throws IOException {
        int rightIndex = treeAsArray.getIndexOfLargestNodeLessThanOrEqualTo(maxValue);
        int valueAtRightIndex = tree[rightIndex];
        assertTrue("maxValue "+maxValue+", rightIndex "+rightIndex+", valueAtRightIndex "+valueAtRightIndex, 
                valueAtRightIndex == maxValue);
    }
    
    private void verifyLeftIndex(int minValue, int maxValue) throws IOException {
        int leftIndex = treeAsArray.getIndexOfSmallestNodeGreaterThanOrEqualTo(minValue);
        int valueAtLeftIndex = tree[leftIndex];
        assertTrue("minValue "+minValue+", leftIndex "+leftIndex+", valueAtLeftIndex "+valueAtLeftIndex, 
                valueAtLeftIndex == minValue);
    }
    
    Set<Integer> allValidInts;
    private void fillValidInts() {
        allValidInts = new HashSet<Integer>();
        for (int i = 0; i < tree.length; i++) {
            if (tree[i] != IntBTree.NULL_NODE_VALUE) {
                allValidInts.add(tree[i]);
            }
        }
    }
    
    private void verifyRange(int minValue, int maxValue) throws IOException {
        Iterator<Integer> iterator = treeAsArray.getIterator(minValue, maxValue);
        List<Integer> expectedValues = new ArrayList<Integer>();
        
        for (int i = minValue; i <= maxValue; i++) {
            if (allValidInts.contains(i)) {
                expectedValues.add(i);
            }
        }
        Iterator<Integer> expectedIterator = expectedValues.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            assertTrue("minValue "+minValue+", maxValue "+maxValue+", iterator.hasNext() was true while expectedIterator.hasNext() was false, i "+i, 
                    expectedIterator.hasNext());
            int expectedValue = expectedIterator.next();
            int actualValue = iterator.next();
            assertTrue("minValue "+minValue+", maxValue "+maxValue+", expectedValue "+expectedValue+", actualValue "+actualValue+", i "+i, 
                    expectedValue == actualValue);
            i++;
        }
        assertTrue("minValue "+minValue+", maxValue "+maxValue+", expectedIterator.hasNext() should have been false, but wasn't", 
                !expectedIterator.hasNext());
    }
}
