package com.browseengine.bobo.geosearch.codec;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.FilterCodec;
import org.apache.lucene.codecs.PostingsFormat;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;

public class GeoCodec extends FilterCodec {
    private GeoPostingsFormat geoPostingsFormat;
    
    public GeoCodec(GeoSearchConfig geoConfig) {
        super("bobo-geo", Codec.getDefault());
        
        PostingsFormat defaultPostingsFormat = super.postingsFormat();
        geoPostingsFormat = new GeoPostingsFormat(geoConfig, defaultPostingsFormat);
    }
    
    @Override
    public PostingsFormat postingsFormat() {
        return geoPostingsFormat;
    }
}
