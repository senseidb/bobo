package com.browseengine.bobo.geosearch.merge.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentReader;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BitVector;
import org.apache.lucene.util.IOUtils;
import org.springframework.stereotype.Component;

import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;
import com.browseengine.bobo.geosearch.impl.BTree;
import com.browseengine.bobo.geosearch.impl.GeoRecordComparator;
import com.browseengine.bobo.geosearch.impl.GeoRecordSerializer;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentWriter;
import com.browseengine.bobo.geosearch.merge.IGeoMergeInfo;
import com.browseengine.bobo.geosearch.merge.IGeoMerger;


/**
 * Basic implementation of the merger interface that merges
 * by using a Buffered look ahead iterator.  This will fail
 * if changing docIds cause any GeoRecords to be moved out of order
 * by more than BUFFER_CAPACITY.
 * 
 * @author Geoff Cooney
 */
@Component
public class BufferedGeoMerger implements IGeoMerger {
    
    private static final Logger LOGGER = Logger.getLogger(BufferedGeoMerger.class);

    public static final int BUFFER_CAPACITY = 10000;
    
    private final IGeoRecordSerializer<GeoRecord> geoRecordSerializer = 
        new GeoRecordSerializer(); 
    
    private final Comparator<GeoRecord> geoComparator = new GeoRecordComparator();
    
    @Override
    //TODO:  Handle more frequent checkAborts
    public void merge(IGeoMergeInfo geoMergeInfo, GeoSearchConfig config) throws IOException {
        IGeoConverter geoConverter = config.getGeoConverter();
        int bufferSizePerGeoReader = config.getBufferSizePerGeoSegmentReader();
        
        Directory directory = geoMergeInfo.getDirectory();
        List<SegmentReader> readers = geoMergeInfo.getReaders();
        List<SegmentInfo> segments =  geoMergeInfo.getSegmentsToMerge();
        
        List<BTree<GeoRecord>> mergeInputBTrees =  new ArrayList<BTree<GeoRecord>>(segments.size());
        List<BitVector> deletedDocsList =  new ArrayList<BitVector>(segments.size());
        boolean success = false;
        try {
            assert (readers.size() == segments.size());
            
            IFieldNameFilterConverter fieldNameFilterConverter = config.getGeoConverter().makeFieldNameFilterConverter();

            boolean hasFieldNameFilterConverter = false;
            for (SegmentReader reader : readers) {
                String geoFileName = config.getGeoFileName(reader.getSegmentName());
                
                BTree<GeoRecord> segmentBTree = 
                    getInputBTree(directory, geoFileName, bufferSizePerGeoReader); 
                mergeInputBTrees.add(segmentBTree);
                
                BitVector deletedDocs = buildDeletedDocsForSegment(reader);
                deletedDocsList.add(deletedDocs);
                
                //just take the first fieldNameFilterConverter for now.  Don't worry about merging them.
                if (!hasFieldNameFilterConverter) {
                    hasFieldNameFilterConverter = loadFieldNameFilterConverter(directory, geoFileName, fieldNameFilterConverter);
                }
            }
            
            if (!hasFieldNameFilterConverter) {
                // we are merging a bunch of segments, none of which have a corresponding .geo file
                // so there is nothing to do, it is okay if the outcome of this merge continues to 
                // not have a .geo file.
                LOGGER.warn("nothing to do during geo merge, no .geo files found for segments");
                success = true;
                return;
            }
            
            int newSegmentSize = calculateMergedSegmentSize(deletedDocsList, mergeInputBTrees, geoConverter);
            
            buildMergedSegment(mergeInputBTrees, deletedDocsList, newSegmentSize, geoMergeInfo, config, fieldNameFilterConverter);
            success = true;
            
            //TODO:  What should we do on an exception just propoate up or cast to a new exception type
        } finally {
            // see https://issues.apache.org/jira/browse/LUCENE-3405
            if (success) {
                IOUtils.close(mergeInputBTrees);
            } else {
                IOUtils.closeWhileHandlingException(mergeInputBTrees);
            }
        }
    }
    
    /**
     * 
     * @param directory
     * @param geoFileName
     * @param fieldNameFilterConverter
     * @return true iff successful
     * @throws IOException
     */
    protected boolean loadFieldNameFilterConverter(Directory directory, String geoFileName,
            IFieldNameFilterConverter fieldNameFilterConverter) throws IOException {
        try {
            DataInput input = directory.openInput(geoFileName);
            input.readVInt();  //read version
            input.readInt();   //throw out tree position
            input.readVInt();  //throw out tree size
        
            fieldNameFilterConverter.loadFromInput(input);
        
            return true;
        } catch (FileNotFoundException e) {
            LOGGER.warn("suppressing missing geo file pair, treating as no field names: "+e);
            return false;
        }
    }

    private void buildMergedSegment(List<BTree<GeoRecord>> mergeInputBTrees, 
            List<BitVector> deletedDocsList, int newSegmentSize, 
            IGeoMergeInfo geoMergeInfo, GeoSearchConfig config, 
            IFieldNameFilterConverter fieldNameFilterConverter) throws IOException {
        Directory directory = geoMergeInfo.getDirectory();
        IGeoConverter geoConverter = config.getGeoConverter();
        
        String segmentName = geoMergeInfo.getNewSegment().name;
        String outputFileName = config.getGeoFileName(segmentName);
        
        GeoSegmentInfo geoSegmentInfo = buildGeoSegmentInfo(segmentName, fieldNameFilterConverter);
        
        Iterator<GeoRecord> inputIterator = 
            new ChainedConvertedGeoRecordIterator(geoConverter, mergeInputBTrees, deletedDocsList, BUFFER_CAPACITY);
        
        BTree<GeoRecord> mergeOutputBTree = null;
        boolean success = false;
        try {
            mergeOutputBTree = getOutputBTree(newSegmentSize, inputIterator, directory, outputFileName, geoSegmentInfo);
            
            success = true;
        } finally {
            // see https://issues.apache.org/jira/browse/LUCENE-3405
            if (success) {
                IOUtils.close(mergeOutputBTree);
            } else {
                IOUtils.closeWhileHandlingException(mergeOutputBTree);
            }
        }
    }
    
    private GeoSegmentInfo buildGeoSegmentInfo(String segmentName, IFieldNameFilterConverter fieldNameFilterConverter) {
        GeoSegmentInfo geoSegmentInfo = new GeoSegmentInfo();
        geoSegmentInfo.setSegmentName(segmentName);
        geoSegmentInfo.setFieldNameFilterConverter(fieldNameFilterConverter);
        
        return geoSegmentInfo;
    }

    protected BTree<GeoRecord> getOutputBTree(int newSegmentSize, Iterator<GeoRecord> inputIterator, 
            Directory directory, String outputFileName, GeoSegmentInfo geoSegmentInfo) throws IOException {
        return new GeoSegmentWriter<GeoRecord>(newSegmentSize, inputIterator, 
                directory, outputFileName, geoSegmentInfo, geoRecordSerializer);
    }
    
    protected BTree<GeoRecord> getInputBTree(Directory directory, String geoFileName, 
            int bufferSizePerGeoReader) throws IOException {
        return new GeoSegmentReader<GeoRecord>(directory, geoFileName, -1, bufferSizePerGeoReader,
                geoRecordSerializer, geoComparator); 
    }
    
    private int calculateMergedSegmentSize(List<BitVector> deletedDocsList,
            List<BTree<GeoRecord>> mergeInputBTrees, IGeoConverter geoConverter) throws IOException {
        int newSegmentSize = 0;
        
        for (int i = 0; i < mergeInputBTrees.size(); i++) {
            BTree<GeoRecord> mergeInputBTree =  mergeInputBTrees.get(i);
            BitVector deletedDocs = deletedDocsList.get(i);
            
            newSegmentSize += calculateSegmentToMergeSize(mergeInputBTree, deletedDocs, geoConverter);
        }
        
        return newSegmentSize;
    }

    private int calculateSegmentToMergeSize(BTree<GeoRecord> mergeInputBTree, 
            BitVector deletedDocs, IGeoConverter geoConverter) throws IOException {
        Iterator<GeoRecord> treeIter = new ConvertedGeoRecordIterator(geoConverter, mergeInputBTree, 
                0, deletedDocs);
        
        int numRecordsToMerge = 0;
        while (treeIter.hasNext()) {
            numRecordsToMerge++;
            treeIter.next();
        }
        
        return numRecordsToMerge;
    }
 
    private BitVector buildDeletedDocsForSegment(SegmentReader reader) {
        BitVector deletedDocs = new BitVector(reader.maxDoc());
        for (int i = 0; i < deletedDocs.size(); i++) {
            if (reader.isDeleted(i)) {
                deletedDocs.set(i);
            }
        }
        
        return deletedDocs;
    }
    
}
