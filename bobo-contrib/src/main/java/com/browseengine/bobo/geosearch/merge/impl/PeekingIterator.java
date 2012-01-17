/**
 * 
 */
package com.browseengine.bobo.geosearch.merge.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class PeekingIterator<T> implements Iterator<T> {
    private T next;
    private Iterator<? extends T> iterator;
    
    public PeekingIterator(Iterator<? extends T> iterator) {
        this.iterator = iterator;
        advance();
    }
    
    private void advance() {
        this.next = iterator.hasNext() ? iterator.next() : null;
    }
    
    public T peek() {
        return next;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return next != null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public T next() {
        T localNext = next;
        if (localNext == null) {
            throw new NoSuchElementException();
        }
        advance(); 
        return localNext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    
}