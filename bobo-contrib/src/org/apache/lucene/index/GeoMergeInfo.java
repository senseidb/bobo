package org.apache.lucene.index;

import java.util.List;

import org.apache.lucene.index.MergePolicy.MergeAbortedException;
import org.apache.lucene.store.Directory;

import com.browseengine.bobo.geosearch.merge.IGeoMergeInfo;

/**
 * Class that contains information about the ongoing Geo Merge
 * 
 * @author Geoff Cooney
 *
 */
public class GeoMergeInfo implements IGeoMergeInfo {
    MergePolicy.OneMerge merge;
    Directory directory;
    
    
    public GeoMergeInfo(MergePolicy.OneMerge merge, Directory directory) {
        this.merge = merge;
        this.directory = directory;
    }
    
    /* (non-Javadoc)
     * @see org.apache.lucene.index.IGeoMergeInfo#checkAborted(org.apache.lucene.store.Directory)
     */
    @Override
    public void checkAborted(Directory dir) throws MergeAbortedException {
        merge.checkAborted(dir);
    }
    
    /* (non-Javadoc)
     * @see org.apache.lucene.index.IGeoMergeInfo#getReaders()
     */
    @Override
    public List<SegmentReader> getReaders() {
        return merge.readerClones;
    }

    /* (non-Javadoc)
     * @see org.apache.lucene.index.IGeoMergeInfo#getSegmentsToMerge()
     */
    @Override
    public List<SegmentInfo> getSegmentsToMerge() {
        return merge.segments;
    }

    /* (non-Javadoc)
     * @see org.apache.lucene.index.IGeoMergeInfo#getDirectory()
     */
    @Override
    public Directory getDirectory() {
        return merge.info.dir;
    }

    /* (non-Javadoc)
     * @see org.apache.lucene.index.IGeoMergeInfo#getNewSegment()
     */
    @Override
    public SegmentInfo getNewSegment() {
        return merge.info;
    }
    
}
