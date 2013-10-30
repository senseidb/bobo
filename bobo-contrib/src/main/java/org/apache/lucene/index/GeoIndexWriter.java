package org.apache.lucene.index;

import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.codec.GeoCodec;


/**
 * Extends Lucene's IndexWriter to provide functionality for indexing, merging, and deleting
 * Coordinate based fields.
 * 
 * @author Geoff Cooney
 * @see IndexWriter
 */
public class GeoIndexWriter extends IndexWriter {

    GeoSearchConfig geoConfig;
    
    public GeoIndexWriter(Directory d, IndexWriterConfig indexWriterConfig, GeoSearchConfig geoConfig) throws CorruptIndexException, LockObtainFailedException,
            IOException {
        super(d, initializeConfig(indexWriterConfig, geoConfig));
        
        this.geoConfig = geoConfig;
    }

    public static IndexWriterConfig initializeConfig(IndexWriterConfig indexWriterConfig, GeoSearchConfig geoConfig) {
        
        return indexWriterConfig.setIndexingChain(new GeoIndexingChain(geoConfig, indexWriterConfig.getIndexingChain()))
                .setCodec(new GeoCodec(geoConfig));
    }
    
}
