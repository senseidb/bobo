package com.browseengine.bobo.geosearch.solo.search.impl;

import org.apache.lucene.util.PriorityQueue;

/**
 * 
 * @author gcooney
 *
 */
public class GeoOnlyHitQueue extends PriorityQueue<GeoOnlyHit> {

    public GeoOnlyHitQueue(int size) {
        initialize(size);
    }
    
    @Override
    protected boolean lessThan(GeoOnlyHit hitA, GeoOnlyHit hitB) {
        return hitA.score > hitB.score;
    }

}
