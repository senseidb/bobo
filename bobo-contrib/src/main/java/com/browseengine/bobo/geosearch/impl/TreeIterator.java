/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

class TreeIterator<T> implements Iterator<T> {
    private final int rightIndex;
    private int index;
    private BTree<T> treeAsArray;
    
    public TreeIterator(BTree<T> treeAsArray, int leftIndex, int rightIndex) {
        this.treeAsArray = treeAsArray;
        this.index = leftIndex;
        this.rightIndex = rightIndex;
    }

    @Override
    public boolean hasNext() {
        return index != BTree.INDEX_OUT_OF_BOUNDS;
    }

    @Override
    public T next() {
        
        
        try {
            if (index == BTree.INDEX_OUT_OF_BOUNDS) {
                throw new NoSuchElementException();
            }
            T value;
            value = treeAsArray.getValueAtIndex(index);
            if (index == rightIndex) {
                index = BTree.INDEX_OUT_OF_BOUNDS;
            } else {
                index = treeAsArray.getNextIndex(index);
            }
            return value;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
        
    }
}