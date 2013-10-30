package com.browseengine.bobo.geosearch.index.impl;

import java.io.IOException;
import java.util.Comparator;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FilterAtomicReader;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.store.IOContext;

import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordComparator;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordSerializer;

public class GeoAtomicReader extends FilterAtomicReader {

    private final CartesianGeoRecordSerializer geoRecordSerializer;
    private final Comparator<CartesianGeoRecord> geoRecordComparator;
    private GeoSearchConfig geoSearchConfig;
    
    private final GeoSegmentReader<CartesianGeoRecord> geoSegmentReader;
    
    public GeoAtomicReader(AtomicReader in, GeoSearchConfig geoSearchConfig) throws IOException {
        super(in);
        this.geoSearchConfig = geoSearchConfig;
        
        geoRecordSerializer = new CartesianGeoRecordSerializer();
        geoRecordComparator = new CartesianGeoRecordComparator();
        
        geoSegmentReader = buildGeoSegmentReader(in);
    }

    private GeoSegmentReader<CartesianGeoRecord> buildGeoSegmentReader(AtomicReader in) throws IOException {
        if (in instanceof SegmentReader) {
            SegmentReader segmentReader = (SegmentReader) in;
            int maxDoc = segmentReader.maxDoc();
            String segmentName = segmentReader.getSegmentName();
            String geoSegmentName = geoSearchConfig.getGeoFileName(segmentName);
            GeoSegmentReader<CartesianGeoRecord> geoSegmentReader = new GeoSegmentReader<CartesianGeoRecord>(
                    segmentReader.directory(), geoSegmentName, maxDoc, IOContext.READ,
                    geoRecordSerializer, geoRecordComparator);
            return geoSegmentReader;
        } 
        
        return null;
    }
    
    public GeoSegmentReader<CartesianGeoRecord> getGeoSegmentReader() {
        return geoSegmentReader;
    }
    
}
