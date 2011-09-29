package com.browseengine.bobo.geosearch.index;

import java.io.IOException;

import org.apache.lucene.store.Directory;

import com.browseengine.bobo.geosearch.index.bo.GeoCoordinateField;

public interface IGeoIndexer {
    void index(int docID, GeoCoordinateField field);

    void abort();

    void flush(Directory directory, String segmentName) throws IOException;
}
