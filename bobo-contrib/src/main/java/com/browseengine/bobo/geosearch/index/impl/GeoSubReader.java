package com.browseengine.bobo.geosearch.index.impl;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FilterDirectoryReader.SubReaderWrapper;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;

public final class GeoSubReader extends SubReaderWrapper {
    private GeoSearchConfig geoSearchConfig;

    public GeoSubReader(GeoSearchConfig geoSearchConfig) {
        this.geoSearchConfig = geoSearchConfig;
    }
    
    @Override
    public AtomicReader wrap(AtomicReader reader) {
        try {
            return new GeoAtomicReader(reader, geoSearchConfig);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}