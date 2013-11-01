package org.apache.lucene.index;

import java.util.List;

import org.apache.lucene.util.InfoStream;

public class MergeStateExposer extends MergeState {

    public MergeStateExposer(List<AtomicReader> readers, SegmentInfo segmentInfo, InfoStream infoStream,
            CheckAbort checkAbort) {
        super(readers, segmentInfo, infoStream, checkAbort);
    }
    
}
