/**
 * 
 */
package com.browseengine.bobo.geosearch.index.impl;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FilterDirectoryReader;
import org.apache.lucene.store.Directory;

import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;

/**
 * @author Shane Detsch
 * @author Ken McCracken
 * @author Geoff Cooney
 *
 */
public class GeoIndexReader extends FilterDirectoryReader {
    
    private List<GeoSegmentReader<CartesianGeoRecord>> geoSegmentReaders;
    private GeoSearchConfig geoSearchConfig;
        
    public GeoIndexReader(Directory directory, GeoSearchConfig geoSearchConfig) throws IOException {
        this(initReader(directory, geoSearchConfig), geoSearchConfig);
    }
    
    private GeoIndexReader(DirectoryReader reader, GeoSearchConfig geoSearchConfig) {
        super(reader, new GeoSubReader(geoSearchConfig));
        
        this.geoSearchConfig = geoSearchConfig;
    }
    
    private static DirectoryReader initReader(Directory directory, GeoSearchConfig geoSearchConfig) throws IOException {
        if (null == directory) {
            return null;
        }
        
        DirectoryReader indexReader = DirectoryReader.open(directory);
        return indexReader;
    }
    
    @Override
    protected DirectoryReader doWrapDirectoryReader(DirectoryReader in) {
        return new GeoIndexReader(in, geoSearchConfig);
    }
}
