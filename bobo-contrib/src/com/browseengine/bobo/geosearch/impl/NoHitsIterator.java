/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class NoHitsIterator<T> implements Iterator<T> {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T next() {
        throw new NoSuchElementException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
        
    }
    
}