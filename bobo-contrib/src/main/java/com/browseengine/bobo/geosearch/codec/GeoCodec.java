package com.browseengine.bobo.geosearch.codec;

import org.apache.lucene.codecs.FilterCodec;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.lucene42.Lucene42Codec;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;

public class GeoCodec extends FilterCodec {
    private GeoPostingsFormat geoPostingsFormat;
    
    //TODO:  This current implementation is loading a default geo config.  This is probably something best
    //stored with the segmentInfo
    public GeoCodec() {
        this(new GeoSearchConfig());
    }
    
    public GeoCodec(GeoSearchConfig geoConfig) {
        super("GeoBobo", new Lucene42Codec());
        
        PostingsFormat defaultPostingsFormat = super.postingsFormat();
        geoPostingsFormat = new GeoPostingsFormat(geoConfig, defaultPostingsFormat);
    }
    
    @Override
    public PostingsFormat postingsFormat() {
        return geoPostingsFormat;
    }
}
