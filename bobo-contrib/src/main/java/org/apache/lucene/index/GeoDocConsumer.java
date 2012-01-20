package org.apache.lucene.index;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.index.IGeoIndexer;
import com.browseengine.bobo.geosearch.index.impl.GeoIndexer;

/**
 * 
 * @author Geoff Cooney
 *
 */
public class GeoDocConsumer extends DocConsumer {

    DocConsumer defaultDocConsumer;
    IGeoIndexer geoIndexer;
    
    public GeoDocConsumer(GeoSearchConfig config, DocConsumer defaultDocConsumer) { 
        this.defaultDocConsumer = defaultDocConsumer;
        this.geoIndexer = new GeoIndexer(config);
    }
    
    public void setGeoIndexer(IGeoIndexer geoIndexer) {
        this.geoIndexer = geoIndexer;
    }
    
    @Override
    DocConsumerPerThread addThread(DocumentsWriterThreadState perThread) throws IOException {
        DocConsumerPerThread defaultDocConsumerPerThread = defaultDocConsumer.addThread(perThread);
        return new GeoDocConsumerPerThread(defaultDocConsumerPerThread, perThread, geoIndexer);
    }

    @Override
    //TODO:  Do we need to do anything for documents that have not yet been flushed but are deleted
    //We should make sure to test this case and see if lucene keeps the document or not
    void flush(Collection<DocConsumerPerThread> threads, SegmentWriteState state) throws IOException {
        //flush synchronously for now, we may later wish to perform this two flushes Asynchronously
        
        //because Lucene's DocConsumer implementation performs an unchecked cast and relies on methods
        //that only exist in a specific implementation, we need to build a list of the
        //defaultDocConsumerPerThreads and pass that into the defaultDocConsumer 
        Collection<DocConsumerPerThread> defaultDocConsumerThreads = 
            new HashSet<DocConsumerPerThread>(threads.size());
        
        for (DocConsumerPerThread thread: threads) {
            if (thread instanceof GeoDocConsumerPerThread) {
                GeoDocConsumerPerThread geoThread = (GeoDocConsumerPerThread)thread;
                defaultDocConsumerThreads.add(geoThread.getDefaultDocConsumerPerThread());
            } else {
                defaultDocConsumerThreads.add(thread);
            }
        }
        
        defaultDocConsumer.flush(defaultDocConsumerThreads, state);
        geoIndexer.flush(state.directory, state.segmentName);
    }

    @Override
    void abort() {
        defaultDocConsumer.abort();
        geoIndexer.abort();
    }

    @Override
    boolean freeRAM() {
        //for now just ask the default DocConsumer to freeRAM  
        return defaultDocConsumer.freeRAM();
    }

    public DocConsumer getDefaultDocConsumer() {
        return defaultDocConsumer;
    }
    
}
