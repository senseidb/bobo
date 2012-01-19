/**
 * 
 */
package com.browseengine.bobo.geosearch.merge.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * A Buffered and re-ordering iterator that can make local corrections 
 * to the ordering returned by its backing iterator, as long as those 
 * corrections can be made within the buffer window.  
 * To the extent 
 * that it is possible, the ordering is enforced using the input Comparator.
 * Throws a RuntimeException if an out-of-order condition is detected, where a 
 * previously-returned value should have come after an about-to-be-returned value.
 * 
 * @author Ken McCracken
 *
 */
public class BufferedOrderedIterator<V> implements Iterator<V> {

    private Iterator<V> iterator;
    private Comparator<V> comparator;
    private int bufferCapacity;
    private TreeSet<V> buffer;

    public BufferedOrderedIterator(Iterator<V> iterator, Comparator<V> comparator, int bufferCapacity) {
        this.iterator = iterator;
        this.comparator = comparator;
        this.bufferCapacity = bufferCapacity;
        this.buffer = new TreeSet<V>(comparator);
        fill();
    }
    
    private void fill() {
        while (iterator.hasNext() && buffer.size() < bufferCapacity) {
            V next = iterator.next();
            buffer.add(next);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        fill();
        return buffer.size() > 0;
    }
    
    private V prev;

    /**
     * {@inheritDoc}
     */
    @Override
    public V next() {
        fill();
        V current = buffer.pollFirst();
        if (null != prev) {
            int comparison = comparator.compare(prev, current);
            if (comparison > 0) {
                // OUT OF ORDER
                throw new RuntimeException("out-of-order condition detected, prev "
                        +prev+", current "+current+", comparison "+comparison);
            }
        }
        prev = current;
        return current;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
        
    }
    
    
}
