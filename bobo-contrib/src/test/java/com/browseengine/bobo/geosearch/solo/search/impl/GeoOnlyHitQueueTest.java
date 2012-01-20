package com.browseengine.bobo.geosearch.solo.search.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 
 * @author gcooney
 *
 */
public class GeoOnlyHitQueueTest {
    
    @Test
    public void testRetrieveInOrder() {
        int queueSize = 10;
        int hitSize = 10;
        
        queueHitsAndVerify(queueSize, hitSize);
    }
    
    @Test
    public void testRetrieveInOrder_FewerHitsThanQueue() {
        int queueSize = 10;
        int hitSize = 5;
        
        queueHitsAndVerify(queueSize, hitSize);
    }
    
    @Test
    public void testRetrieveInOrder_MoreHitsThanQueue() {
        int queueSize = 10;
        int hitSize = 20;
        
        queueHitsAndVerify(queueSize, hitSize);
    }
    
    private void queueHitsAndVerify(int queueSize, int hitSize) {
        GeoOnlyHitQueue hitQueue = new GeoOnlyHitQueue(queueSize);
        
        for (int i = 0; i < hitSize; i++) {
            byte[] uuid = new byte[] {(byte)i};
            double score = i;
            GeoOnlyHit hit = new GeoOnlyHit(score, uuid);
            hitQueue.insertWithOverflow(hit);
        }
        
        GeoOnlyHit lastHit = hitQueue.pop();
        assertEquals(Math.min(hitSize - 1, queueSize - 1), lastHit.score, 0.0000001);
        
        GeoOnlyHit currentHit = hitQueue.pop();
        int hitCount = 1;
        while (currentHit != null) {
            assertTrue("Hits should be in descending order", lastHit.score > currentHit.score);
            hitCount++;
            
            lastHit = currentHit;
            currentHit = hitQueue.pop();
        }

        assertEquals(Math.min(queueSize, hitSize), hitCount);
    }
}
