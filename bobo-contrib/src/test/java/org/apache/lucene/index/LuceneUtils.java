package org.apache.lucene.index;

import org.apache.lucene.store.Directory;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.codec.GeoCodec;

public class LuceneUtils {
    
    public static SegmentInfo buildSegmentInfo(String name, int docCount, GeoSearchConfig geoSearchConfig, Directory dir) {
        boolean isCompoundFile = true;
        boolean hasSingleNormFile = true;
        boolean hasProx = true;
        boolean hasVectors = true;
        SegmentInfo segment = new SegmentInfo(dir, "v1", name, docCount, isCompoundFile, new GeoCodec(geoSearchConfig), null, null);
        
        return segment;
    }
}
