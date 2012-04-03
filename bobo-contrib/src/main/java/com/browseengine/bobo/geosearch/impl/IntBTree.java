/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import java.io.IOException;


/**
 * Requires:
 * a total ordering on tree[] where tree[0] is the root node, tree[1] is the left child of root, 
 * tree[2] is right child of root, and so on.
 * The value for a null node should be {{@link #NULL_NODE_VALUE}.
 * 
 * @author Ken McCracken
 *
 */
public class IntBTree extends BTree<Integer> {
    public static final int NULL_NODE_VALUE = -1;
    
    private final int[] tree;
    
    public IntBTree(int[] tree) {
        super(tree.length, true);
        this.tree = tree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getArrayLength() {
        return tree.length;
    }
    
    @Override
    protected boolean isNullNoRangeCheck(int index) {
        int value = tree[index];
        return value == NULL_NODE_VALUE;
    }
    
    @Override
    protected Integer getValueAtIndex(int index) {
        return tree[index];
    }
    
    @Override
    protected int compare(int index, Integer value) {
        int valueAsInt = value;
        return tree[index] - valueAsInt;
    }
    
    @Override
    protected int compareValuesAt(int leftIndex, int rightIndex) {
        return tree[leftIndex] - tree[rightIndex];
    }

    @Override
    protected void setValueAtIndex(int index, Integer value) throws IOException {
        tree[index] = value;
    }

    @Override
    public void close() throws IOException {
    }
    
    
}
