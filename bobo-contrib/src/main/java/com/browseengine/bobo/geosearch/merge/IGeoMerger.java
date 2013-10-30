package com.browseengine.bobo.geosearch.merge;

import java.io.IOException;

import org.apache.lucene.index.MergeState;
import org.apache.lucene.index.SegmentWriteState;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;

public interface IGeoMerger {
    void merge(SegmentWriteState segmentWriteState, MergeState mergeState, GeoSearchConfig config) throws IOException;
}
