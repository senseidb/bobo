package com.browseengine.bobo.geosearch.solo.search.impl;

/**
 * 
 * @author gcooney
 *
 */
public class GeoOnlyHit {

    public final double score;
    public final byte[] uuid;

    public GeoOnlyHit(double score, byte[] uuid) {
        this.score = score;
        this.uuid = uuid;
    }
    
}
