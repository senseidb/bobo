/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * A BTree that does not use pointers, and attempts to preserve 
 * locality of reference as we move down the tree and amongst sibling subtrees.
 * 
 * <p>
 * A good description of our Array representation can be found 
 * <a href="http://en.wikipedia.org/wiki/Binary_tree#Arrays">here on wikipedia</a>.
 * 
 * @author Ken McCracken
 * @author Shane Detsch
 */
public abstract class BTree<V> implements Closeable {
    protected static final int INDEX_OUT_OF_BOUNDS = -1;
    protected static final int ROOT_INDEX = 0;
    
    /*
     * For a tree with 9 nodes.
     * You have height of 4.
     * You have n - 2(h-1) + 1 = 2 level 0 leaves. 
     */
    protected int arrayLength;
    protected boolean nullCheckChecksValues;
    final int height;
    final int leftMostLeafIndex;

    public BTree(int arrayLength, boolean nullCheckChecksValues) {
        init(arrayLength, nullCheckChecksValues);
        this.arrayLength = arrayLength;
        this.nullCheckChecksValues = nullCheckChecksValues;
        this.height = getDepthOf(arrayLength-1)+1;
        this.leftMostLeafIndex = (1 << (height-1)) - 1; // 2^(h-1) - 1 if root is 0;
    }
    
    protected void init(int arrayLength, boolean nullCheckChecksValues) {
        this.arrayLength = arrayLength;
        this.nullCheckChecksValues = nullCheckChecksValues;
    }

    public int getNextIndex(int index) throws IOException {

        if (hasRightChild(index)) {
            index = getRightChildIndex(index);
            while (hasLeftChild(index)) {
                index = getLeftChildIndex(index);
            }
            return index;
        } else {
            while (isARightChild(index)) {
                index = getParentIndex(index);
            }
            if (isALeftChild(index)) {
                index = getParentIndex(index);
                return index;
            }
        }
        return INDEX_OUT_OF_BOUNDS;
    }
    
    public int getHeight() {
        return this.height;
    }
    
    public abstract int getArrayLength();
    
    public int getLeftChildIndex(int index) {
        return (index+1)*2 - 1;
    }
    
    public int getRightChildIndex(int index) {
        return getLeftChildIndex(index)+1;
    }
    
    public int getParentIndex(int index) {
        return (index-1) / 2;
    }
    
    public boolean isALeftChild(int index) {
        if (index == 0) {
            return false;
        }
        return (index % 2) == 1;
    }
    
    public boolean isARightChild(int index) {
        if (index == 0) {
            return false;
        }
        return !isALeftChild(index);
    }
    
    public boolean hasRightChild(int index) throws IOException {
        int rightChildIndex = getRightChildIndex(index);
        return !isNull(rightChildIndex);
    }
    
    public boolean hasLeftChild(int index) throws IOException {
        int leftChildIndex = getLeftChildIndex(index);
        return !isNull(leftChildIndex);
    }
    
    /**
     * Gets an iterator of values between minValue and maxValue (minimumcode and 
     * maximum code).
     * This is a naïve implementation, and follows 
     * http://www.vision-tools.com/h-tropf/multidimensionalrangequery.pdf
     * "Section 4 Range Search", the first approach that does NOT use 
     * LITMAX and BIGMIN.
     * 
     * @param minValue
     * @param maxValue
     * @return
     */
    public Iterator<V> getIterator(V minValue, V maxValue) throws IOException {
        final int leftIndex = getIndexOfSmallestNodeGreaterThanOrEqualTo(minValue);
        final int rightIndex = getIndexOfLargestNodeLessThanOrEqualTo(maxValue);
        if (leftIndex >= 0 && rightIndex >= 0 
                && compareValuesAt(leftIndex, rightIndex) <= 0) {
            return new TreeIterator<V>(this, leftIndex, rightIndex);
        } else {
            return new NoHitsIterator<V>();
        }
    }
    
    /**
     * Returns the depth of the tree to get to the node stored at array index 
     * <tt>index</tt>.
     * It is as though you are counting the number of edges you cross to get from 
     * root to the specified <tt>index</tt> during top-down tree traversal.
     * 
     * <p>
     * The depth of the root node is 0.
     * The depth of the L child is 1.
     * The depth of the R child is 1.
     * The depth of LL is 2.
     * And so on.
     * 
     * @param index
     * @return
     */
    protected int getDepthOf(int index) {
        int depth = 0;
        index++;
        while (index > 1) {
            index /= 2;
            depth++;
        }
        return depth;
    }
    
    protected int getLeftMostLeafIndex() {
        return leftMostLeafIndex;
    }
    
    private class Path {
        List<Integer> treeIndexesInRange;
        List<Integer> depthsInRange;
        
        public Path() {
            this.treeIndexesInRange = new ArrayList<Integer>();
            this.depthsInRange = new ArrayList<Integer>();
        }
        
        public void add(int indexInTreeArray) {
            treeIndexesInRange.add(indexInTreeArray);
            int depth = getDepthOf(indexInTreeArray);
            depthsInRange.add(depth);
        }
        
        public boolean isConnected() {
            if (treeIndexesInRange.size() < 2) {
                return true;
            }
            int i = 0;
            int prev = depthsInRange.get(i++);
            while (i < treeIndexesInRange.size()) {
                int current = depthsInRange.get(i++);
                if (current - prev != 1) {
                    return false;
                }
                prev = current;
            }
            return true;
        }
        
        public int get(int indexInVisitedNodesInRange) {
            return treeIndexesInRange.get(indexInVisitedNodesInRange);
        }
        
        public boolean contains(int treeIndex) {
            return treeIndexesInRange.contains(treeIndex);
        }
        
        public int size() {
            return treeIndexesInRange.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Path [treeIndexesInRange=" + treeIndexesInRange + "]";
        }

    }
    
    /**
     * Gets an upper bound on the number of nodes in range in the tree, 
     * assuming the all occupied depths are full and no nodes are null.
     * NOTE: we might further improve accuracy here later on.
     * 
     * @param minValue
     * @param maxValue
     * @return
     */
    public int getUpperBoundOnNumberOfNodes(V minValue, V maxValue) throws IOException {
        Path leftPath = new Path();
        Path rightPath = new Path();
        final int leftIndex = getIndexOfSmallestNodeGreaterThanOrEqualTo(minValue, leftPath, maxValue);
        final int rightIndex = getIndexOfLargestNodeLessThanOrEqualTo(maxValue, rightPath, minValue);
        if (leftIndex >= 0 && rightIndex >= 0 
                && compareValuesAt(leftIndex, rightIndex) <= 0) {
            // case 1: R = L.  return 1;
            if (leftIndex == rightIndex) {
                return 1;
            }
            
            if (leftPath.isConnected() && rightPath.isConnected()) {
                // case 2: R is on the path from L to root node
                // start with sum = 2.
                // for each node visited from [L, R), add a score for the whole subtree rooted at 
                // right child of current.
                // return sum;
                if (leftPath.contains(rightIndex)) {
                    return getCase2(leftIndex, rightIndex);
                }
                
                // case 3: L is on the path from L to root node
                if (rightPath.contains(leftIndex)) {
                    return getCase2(rightIndex, leftIndex);
                }
                
                // case 4: L and R are both connected and both meet at some node on their path to the root
                // the "some node" is at leftPath.get(0) and rightPath.get(0).
                // remove the nodes in leftPath that don't satisfy maxValue
                int someNodeIndex = leftPath.get(0);
                if (leftPath.isConnected() && rightPath.isConnected()) {
                    return getCase2(leftIndex, someNodeIndex) 
                        + getCase2(rightIndex, someNodeIndex) 
                        // correct for double-counting "some node"
                        - 1;
                }
            }
            
            // case 5: L or R is disconnected.
            int someNodeIndex = leftPath.get(0);
            return getCountDisconnected(leftPath, rightPath, leftIndex, rightIndex, someNodeIndex);
        }
        return 0;

    }
    
    private int getCountDisconnected(Path leftPath, Path rightPath, 
            int leftIndex, int rightIndex, int someNodeIndex) {
        int sum = 0;
        boolean highestHitCounted = false;
        if (leftPath.isConnected()) {
            // includes the highest hit
            highestHitCounted = true;
            sum += getCase2(leftIndex, someNodeIndex);
        } else {
            // for every node in the path except the first, 
            // add that node plus 
            // its left child subtree in entirety.
            for (int i = 1; i < leftPath.size(); i++) {
                int currentNodeIndex = leftPath.get(i);
                sum += getNodesInTreeRootedAt(getDepthOf(currentNodeIndex) + 1) + 1;
            }
        }
        if (rightPath.isConnected()) {
            // includes the highest hit
            sum += getCase2(rightIndex, someNodeIndex);
            if (highestHitCounted) {
                // this should be a degenerate case
                sum--;
            } else {
                highestHitCounted = true;
            }
        } else {
            // for every node in the path except the first, 
            // add that node plus 
            // its left child subtree in entirety.
            for (int i = 1; i < rightPath.size(); i++) {
                int currentNodeIndex = rightPath.get(i);
                sum += getNodesInTreeRootedAt(getDepthOf(currentNodeIndex) + 1) + 1;
            }
        }
        if (!highestHitCounted) {
            sum++;
        }
        return sum;

    }
    
    private int getCase2(int indexLowerInTheTree, int indexHigherInTheTree) {
        int sum = 1;
        int depthOfLower = getDepthOf(indexLowerInTheTree);
        int depthOfHigher = getDepthOf(indexHigherInTheTree);
        for (int depth = depthOfLower; depth > depthOfHigher; depth--) {
            sum += getNodesInTreeRootedAt(depth + 1) + 1;
        }
        return sum;
    }
    
    private static int[] NODES_OF_HEIGHT;
    
    static {
        NODES_OF_HEIGHT = new int[32];
        int pow = 1;
        for (int i = 0; i < NODES_OF_HEIGHT.length; i++) {
            NODES_OF_HEIGHT[i] = pow - 1;
            pow <<= 1;
        }
    }
    
    /**
     * Assumes a full tree.
     * 
     * @param index
     * @return
     */
    private int getNodesInTreeRootedAt(int depth) {
        int heightOfSubtree = height-depth;
        if (heightOfSubtree >= 0) {
            return NODES_OF_HEIGHT[heightOfSubtree];
        }
        return 0;
    }
    
   
    
    protected int getIndexOfSmallestNodeGreaterThanOrEqualToStartingFrom(V minValue, int index, int candidateIndex, Path candidateIndexes, V maxValue) throws IOException{
        if (isNull(index)) {
            // we are at a null node terminus
            return candidateIndex;
        }
        final int comparison = compare(index, minValue);
        
        if (comparison > 0) {
            int leftChildIndex = getLeftChildIndex(index);
            addToCandidateIndexes(candidateIndexes, index, maxValue, true);
            return getIndexOfSmallestNodeGreaterThanOrEqualToStartingFrom(minValue, leftChildIndex, index, candidateIndexes, maxValue);
        } else if (comparison < 0) {
            int rightChildIndex = getRightChildIndex(index);
            return getIndexOfSmallestNodeGreaterThanOrEqualToStartingFrom(minValue, rightChildIndex, candidateIndex, candidateIndexes, maxValue);
        } else {
            addToCandidateIndexes(candidateIndexes, index, maxValue, true);
            return index;
        }
    }
    
    
    
    protected int getIndexOfSmallestNodeGreaterThanOrEqualTo(V minValue, Path candidateIndexes, V maxValue) throws IOException{
        return getIndexOfSmallestNodeGreaterThanOrEqualToStartingFrom(minValue, ROOT_INDEX, INDEX_OUT_OF_BOUNDS, candidateIndexes, maxValue);
    }
    
    public int getIndexOfSmallestNodeGreaterThanOrEqualTo(V minValue) throws IOException {
        return getIndexOfSmallestNodeGreaterThanOrEqualToStartingFrom(minValue, ROOT_INDEX, INDEX_OUT_OF_BOUNDS, null, null);
    }
    
    protected int getIndexOfLargestNodeLessThanOrEqualTo(V maxValue, Path candidateIndexes, V minValue) throws IOException{
        return getIndexOfLargestNodeLessThanOrEqualToStartingFrom(maxValue, ROOT_INDEX, INDEX_OUT_OF_BOUNDS, candidateIndexes, minValue);
    }
    

    
    public int getIndexOfLargestNodeLessThanOrEqualTo(V maxValue) throws IOException {
        return getIndexOfLargestNodeLessThanOrEqualToStartingFrom(maxValue, ROOT_INDEX, INDEX_OUT_OF_BOUNDS, null, null);
    }

    protected int getIndexOfLargestNodeLessThanOrEqualToStartingFrom(V maxValue, int index, int candidateIndex, Path candidateIndexes, V minValue) throws IOException{
        if (isNull(index)) {
            // we are at a null node terminus
            return candidateIndex;
        }
        final int comparison = compare(index, maxValue);

        if (comparison > 0) {
            int leftChildIndex = getLeftChildIndex(index);
            return getIndexOfLargestNodeLessThanOrEqualToStartingFrom(maxValue, leftChildIndex, candidateIndex, candidateIndexes, minValue);
        } else if (comparison < 0) {
            int rightChildIndex = getRightChildIndex(index);
            addToCandidateIndexes(candidateIndexes, index, minValue, false);
            return getIndexOfLargestNodeLessThanOrEqualToStartingFrom(maxValue, rightChildIndex, index, candidateIndexes, minValue);
        } else {
            addToCandidateIndexes(candidateIndexes, index, minValue, false);
            return index;
        }
    }
    
    private void addToCandidateIndexes(Path candidateIndexes, int index, V value, boolean valueIsMaxValue) throws IOException {
        if (null != candidateIndexes) {
            int comparison;
            try {
                comparison = compare(index, value);
                
                if (valueIsMaxValue) {
                    if (comparison <= 0) {
                        candidateIndexes.add(index);
                    }
                } else {
                    if (comparison >= 0) {
                        candidateIndexes.add(index);
                    }
                
                } 
            }catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    public boolean isNull(int index) throws IOException {
        if (index >= arrayLength) {
            return true;
        }
        if (nullCheckChecksValues) {
            return isNullNoRangeCheck(index);
        }
        return false;
    }

    
    protected abstract int compareValuesAt(int leftIndex, int rightIndex) throws IOException;
    
    protected abstract int compare(int index, V value) throws IOException;
    
    protected abstract boolean isNullNoRangeCheck(int index) throws IOException;

    protected abstract V getValueAtIndex(int index) throws IOException;
    
    protected abstract void setValueAtIndex(int index, V value) throws IOException;
    
}
