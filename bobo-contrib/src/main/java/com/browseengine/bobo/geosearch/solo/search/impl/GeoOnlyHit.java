package com.browseengine.bobo.geosearch.solo.search.impl;

/**
 * 
 * @author gcooney
 *
 */
public class GeoOnlyHit {

    public final float score;
    public final byte[] uuid;

    public GeoOnlyHit(float score, byte[] uuid) {
        this.score = score;
        this.uuid = uuid;
    }
    
}
