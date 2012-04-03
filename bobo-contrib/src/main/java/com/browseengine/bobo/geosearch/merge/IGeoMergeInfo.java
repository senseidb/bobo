package com.browseengine.bobo.geosearch.merge;

import java.util.List;

import org.apache.lucene.index.MergePolicy.MergeAbortedException;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.store.Directory;

public interface IGeoMergeInfo {

    void checkAborted(Directory dir) throws MergeAbortedException;

    List<SegmentReader> getReaders();

    List<SegmentInfo> getSegmentsToMerge();

    Directory getDirectory();

    SegmentInfo getNewSegment();

}