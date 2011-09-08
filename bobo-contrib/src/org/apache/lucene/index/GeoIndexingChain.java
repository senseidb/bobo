package org.apache.lucene.index;

import org.apache.lucene.index.DocumentsWriter.IndexingChain;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;


/**
 * Implementation of lucene IndexingChain class.  The GeoIndexingChain class 
 * takes another indexing chain in the constructor.  It adds a custom GeoDocConsumer
 * which wraps the default indexing chain's consumer to pull out geo components 
 * from the document and index them independently, before returning control to the
 * default Consumer. 
 * 
 * @author Geoff Cooney
 *
 */
public class GeoIndexingChain extends IndexingChain {
    
    IndexingChain defaultIndexingChain;
    GeoSearchConfig config;
    
    public GeoIndexingChain(GeoSearchConfig config, IndexingChain defaultIndexingChain) {
        this.defaultIndexingChain = defaultIndexingChain;
        this.config = config;
    }
    
    @Override
    DocConsumer getChain(DocumentsWriter documentsWriter) {
        DocConsumer defaultDocConsumer = defaultIndexingChain.getChain(documentsWriter);
        return new GeoDocConsumer(config, defaultDocConsumer);
    }
}
