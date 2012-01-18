package com.browseengine.bobo.geosearch.solo.search.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author gcooney
 *
 */
public final class GeoOnlyHits {
    private final int totalHits;
    private final List<GeoOnlyHit> hits;

    public GeoOnlyHits(int totalHits, GeoOnlyHit[] hits) {
        this.totalHits = totalHits;
        this.hits = new ArrayList<GeoOnlyHit>(hits.length);
        Collections.addAll(this.hits, hits);
    }
    
    public int totalHits() {
        return totalHits;
    }
    
    public List<GeoOnlyHit> getHits() {
        return hits;
    }
    
}
