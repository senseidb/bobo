package org.apache.lucene.index;

import org.apache.lucene.store.Directory;

public class LuceneUtils {
    
    public static SegmentInfo buildSegmentInfo(String name, int docCount, int delCount, Directory dir) {
        boolean isCompoundFile = true;
        boolean hasSingleNormFile = true;
        boolean hasProx = true;
        boolean hasVectors = true;
        SegmentInfo segment = new SegmentInfo(name, docCount, dir, isCompoundFile, hasSingleNormFile, hasProx, hasVectors);
        segment.setDelCount(delCount);
        
        return segment;
    }
}
