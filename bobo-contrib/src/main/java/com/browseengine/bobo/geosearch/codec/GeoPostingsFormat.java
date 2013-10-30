package com.browseengine.bobo.geosearch.codec;

import java.io.IOException;

import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.TermsConsumer;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.merge.IGeoMerger;

public class GeoPostingsFormat extends PostingsFormat {
    private final GeoSearchConfig geoConfig;
    private PostingsFormat defaultPostingsFormat;

    protected GeoPostingsFormat(GeoSearchConfig geoConfig, PostingsFormat defaultPostingsFormat) {
        super("bobo-geo");
        this.geoConfig = geoConfig;
        this.defaultPostingsFormat = defaultPostingsFormat;
    }

    @Override
    public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
        return new GeoFieldsConsumer(state, defaultPostingsFormat.fieldsConsumer(state));
    }

    @Override
    public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
        return defaultPostingsFormat.fieldsProducer(state);
    }
    
    public class GeoFieldsConsumer extends FieldsConsumer {

        private SegmentWriteState segmentWriteState;
        private FieldsConsumer defaultFieldsConsumer;

        public GeoFieldsConsumer(SegmentWriteState segmentWriteState, FieldsConsumer defaultFieldsConsumer) {
            this.segmentWriteState = segmentWriteState;
            this.defaultFieldsConsumer = defaultFieldsConsumer;
        }
        
        @Override
        public TermsConsumer addField(FieldInfo field) throws IOException {
            return defaultFieldsConsumer.addField(field);
        }

        @Override
        public void close() throws IOException {
            defaultFieldsConsumer.close();
        }

        @Override
        public void merge(MergeState mergeState, Fields fields) throws IOException {
            //first merge GeoIndex
            IGeoMerger geoMerger = geoConfig.getGeoMerger();
          
            geoMerger.merge(segmentWriteState, mergeState, geoConfig);
            
            mergeState.checkAbort.work(10000);
            
            //now everything else
            defaultFieldsConsumer.merge(mergeState, fields);
        }
    }
}
