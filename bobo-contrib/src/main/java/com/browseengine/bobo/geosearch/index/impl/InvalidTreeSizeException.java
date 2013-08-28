package com.browseengine.bobo.geosearch.index.impl;

/**
 * 
 * This exception indicates that the size of the compact binary tree,
 * does not match the number of elements actually written to it.
 * 
 * @author Geoff Cooney
 *
 */
public class InvalidTreeSizeException extends Exception {
    private static final long serialVersionUID = -586521581062650233L;
    private final int treeSize;
    private final int recordSize;

    public InvalidTreeSizeException(int treeSize, int recordSize) {
        super("Explicit tree size(" + treeSize + ") does not match the number of records attempted to be written to the tree(" 
                + recordSize + ")");
        this.treeSize = treeSize;
        this.recordSize = recordSize;
    }

    /**
     * @return  The explicit size of the tree that this exception was generated for
     */
    public int getTreeSize() {
        return treeSize;
    }

    /**
     * @return  The actual number of records in the tree this exception was generated for
     */
    public int getRecordSize() {
        return recordSize;
    }
}
