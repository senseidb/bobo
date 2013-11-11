package com.browseengine.bobo.geosearch.index.impl;

import java.io.IOException;
import java.util.Comparator;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.FilterAtomicReader;
import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.store.CompoundFileDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;

import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordComparator;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordSerializer;

public class GeoAtomicReader extends FilterAtomicReader {

    
    private final GeoSegmentReader<CartesianGeoRecord> geoSegmentReader;
    
    public GeoAtomicReader(AtomicReader in, GeoSearchConfig geoSearchConfig) throws IOException {
        this(in, buildGeoSegmentReader(in, geoSearchConfig));
    }
    
    public GeoAtomicReader(AtomicReader in, GeoSegmentReader<CartesianGeoRecord> geoSegmentReader) {
        super(in);
        this.geoSegmentReader = geoSegmentReader;
    }

    private static GeoSegmentReader<CartesianGeoRecord> buildGeoSegmentReader(AtomicReader in, GeoSearchConfig geoSearchConfig) throws IOException {
        if (in instanceof SegmentReader) {
            CartesianGeoRecordSerializer geoRecordSerializer = new CartesianGeoRecordSerializer();
            Comparator<CartesianGeoRecord> geoRecordComparator = new CartesianGeoRecordComparator();
            
            SegmentReader segmentReader = (SegmentReader) in;
            int maxDoc = segmentReader.maxDoc();
            String segmentName = segmentReader.getSegmentName();
            
            Directory dir = getDirectory(in, geoSearchConfig);
            String geoSegmentName = geoSearchConfig.getGeoFileName(segmentName);
            GeoSegmentReader<CartesianGeoRecord> geoSegmentReader = new GeoSegmentReader<CartesianGeoRecord>(
                    dir, geoSegmentName, maxDoc, IOContext.READ,
                    geoRecordSerializer, geoRecordComparator);
            return geoSegmentReader;
        } 
        
        return null;
    }
    
    public static Directory getDirectory(AtomicReader in, GeoSearchConfig geoSearchConfig) throws IOException {
        SegmentReader segmentReader = (SegmentReader) in;
        Directory dir = segmentReader.directory();
        String segmentName = segmentReader.getSegmentName();
        String geoFileName = IndexFileNames.segmentFileName(segmentName, "", geoSearchConfig.getGeoFileExtension());
        if (segmentReader.getSegmentInfo().info.getUseCompoundFile()) {
            String compoundFileName = IndexFileNames.segmentFileName(segmentName, "", IndexFileNames.COMPOUND_FILE_EXTENSION);
            Directory compoundDir = new CompoundFileDirectory(dir, compoundFileName , IOContext.READ, false);
            
            //In order to read geo segments written against lucene3.x, we only use the CompoundFileReader if the geo file exist 
            //in the cfs file.  Otherwise, we run the risk of ignoring a .geo file created outside of a .cfs.
            if (compoundDir.fileExists(geoFileName)) {
                dir = compoundDir;
            }
        }
        
        return dir;
    }
    
    public GeoSegmentReader<CartesianGeoRecord> getGeoSegmentReader() {
        return geoSegmentReader;
    }
    
}
