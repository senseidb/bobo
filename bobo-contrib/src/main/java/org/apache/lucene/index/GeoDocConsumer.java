package org.apache.lucene.index;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.lucene.index.DocumentsWriterPerThread.DocState;
import org.apache.lucene.index.FieldInfos.Builder;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.index.IGeoIndexer;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinateField;
import com.browseengine.bobo.geosearch.index.impl.GeoIndexer;

/**
 * 
 * @author Geoff Cooney
 *
 */
public class GeoDocConsumer extends DocConsumer {

    DocConsumer defaultDocConsumer;
    IGeoIndexer geoIndexer;
    private DocState docState;
    
    public GeoDocConsumer(GeoSearchConfig config, DocConsumer defaultDocConsumer, DocumentsWriterPerThread documentsWriter) {
        this.defaultDocConsumer = defaultDocConsumer;
        this.geoIndexer = new GeoIndexer(config);
        docState = documentsWriter.docState;
    }
    
    public void setGeoIndexer(IGeoIndexer geoIndexer) {
        this.geoIndexer = geoIndexer;
    }
    
    @Override
    //TODO:  Do we need to do anything for documents that have not yet been flushed but are deleted
    //We should make sure to test this case and see if lucene keeps the document or not
    void flush(SegmentWriteState state) throws IOException {
        //flush synchronously for now, we may later wish to perform this two flushes Asynchronously

        defaultDocConsumer.flush(state);
        geoIndexer.flush(state);
    }

    @Override
    void abort() {
        defaultDocConsumer.abort();
        geoIndexer.abort();
    }

    public DocConsumer getDefaultDocConsumer() {
        return defaultDocConsumer;
    }

    @Override
    void processDocument(Builder fieldInfos) throws IOException {
        //this is where we process the geo-search components of the document
        int docID = docState.docID;
        Builder unconsumedFields = new Builder();
        
        List<GeoCoordinateField> geoFields = new Vector<GeoCoordinateField>();
        
        Set<String> geoFieldNames = new HashSet<String>();
        for (IndexableField field: docState.doc) {
            if (field instanceof GeoCoordinateField) {
                geoFields.add((GeoCoordinateField)field);
                geoFieldNames.add(field.name());
            } 
        }
        
        for (FieldInfo fieldInfo : fieldInfos.finish()) {
            if (!geoFieldNames.contains(fieldInfo.name)) {
                unconsumedFields.add(fieldInfo);
            }
        }

        for (GeoCoordinateField geoField: geoFields) {
            //process field into GeoIndex here
            geoIndexer.index(docID, geoField);
        }
        
        defaultDocConsumer.processDocument(fieldInfos);
//        defaultDocConsumer.processDocument(unconsumedFields);
    }

    @Override
    void finishDocument() throws IOException {
        defaultDocConsumer.finishDocument();
    }

    @Override
    void doAfterFlush() {
        defaultDocConsumer.doAfterFlush();
    }
    
}
