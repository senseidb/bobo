package org.apache.lucene.index;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordComparator;
import com.browseengine.bobo.geosearch.impl.CartesianGeoRecordSerializer;
import com.browseengine.bobo.geosearch.impl.MappedFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinate;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinateField;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;

/**
 * Superclass for GeoSearch functional test containing common methods for
 * building and searching indices
 *
 * @author Geoff Cooney
 *
 */
public class GeoSearchFunctionalTezt {
    Directory directory;
    IndexWriterConfig config;
    
    IndexWriter writer;
    
    GeoSearchConfig geoConfig;
    MappedFieldNameFilterConverter fieldNameConverter;
    CartesianGeoRecordComparator geoComparator;
    IGeoRecordSerializer<CartesianGeoRecord> geoRecordSerializer;
    
    static final String TEXT_FIELD = "text";
    static final String TITLE_FIELD = "title";
    static final String LOCATION_FIELD = "geocoordinate";
    static final String IMAGE_LOCATION_FIELD = "imageLocation";
    
    static final byte LOCATION_BIT_MASK = (byte)1;
    static final byte IMAGE_LOCATION_BIT_MASK = (byte)2;
    
    protected static final float LATTITUDE_MAX_VALUE = 90f;
    protected static final float LATTITUDE_MIN_VALUE = -90f;
    protected static final float LONGITUDE_MAX_VALUE = 180f;
    protected static final float LONGITUDE_MIN_VALUE = -180f;
    
  //Sample text and titles come from the top 11 opening lines of novels according to
    //a random web page from google(Skipping Lolita because I was a little uncomfortable pastin
    //that quote in here) 
    String[] text = new String[] {
        "Call me Ishmael",
        "It is a truth universally acknowledged, that a single man in possession of a good fortune, must be in want of a wife",
        "A screaming comes across the sky",
        "Many years later, as he faced the firing squad, Colonel Aureliano Buendia was to remember that distant afternoon when his father took him to discover ice",
        "Happy families are all alike; every unhappy family is unhappy in its own way",
        "riverrun, past Eve and Adam's, from swerve of shore to bend of bay, brings us by a commodius vicus of recirculation back to Howth Castle and Environs",
        "It was a bright cold day in April, and the clocks were striking thirteen",
        "It was the best of times, it was the worst of times, it was the age of wisdom, it was the age of foolishness, it was the epoch of belief, it was the epoch of incredulity, it was the season of Light, it was the season of Darkness, it was the spring of hope, it was the winter of despair",
        "I am an invisible man",
        "The Miss Lonelyhearts of the New York Post-Dispatch (Are you in trouble? Do-you-need-advice? Write-to-Miss-Lonelyhearts-and-she-will-help-you) sat at his desk and stared at a piece of white cardboard"
    };
    
    String[] titles = new String[] {
        "Moby Dick",
        "Pride and Prejudice",
        "Gravity's Rainbow",
        "One Hundred Years of Solitude",
        "Anna Karenina",
        "Finnegans Wake",
        "1984",
        "A Tale Of Two Cities",
        "The Invisible Man",
        "Miss Lonelyhearts"
    };
    
    void addDocuments() throws CorruptIndexException, IOException {
        for (int i=0; i < text.length; i++) {
            Document document = buildDocument(text[i], titles[i], i, text.length);
            writer.addDocument(document);
        }
    }
    
    void buildGeoIndexWriter(boolean useCompoundFileFormat) throws CorruptIndexException, LockObtainFailedException, IOException {
        buildGeoIndexWriter(useCompoundFileFormat, true);
    }
    
    void buildGeoIndexWriter(boolean useCompoundFileFormat, boolean initDirectory) throws CorruptIndexException, LockObtainFailedException, IOException {
        geoComparator = new CartesianGeoRecordComparator();
        geoRecordSerializer = new CartesianGeoRecordSerializer();
        
        if(initDirectory) {
            initDirectory();
        }
        
        config = new IndexWriterConfig(Version.LUCENE_43, 
                new StandardAnalyzer(Version.LUCENE_43));
        
        config.setMergePolicy(new MergeOnOptimizeOnly(useCompoundFileFormat));
        
        geoConfig = getGeoSearchConfig();
        geoConfig.addFieldBitMask(LOCATION_FIELD, LOCATION_BIT_MASK);
        geoConfig.addFieldBitMask(IMAGE_LOCATION_FIELD, IMAGE_LOCATION_BIT_MASK);
        
        writer = new GeoIndexWriter(directory, config, geoConfig);
    }
    
    void initDirectory() {
        directory = new RAMDirectory();
    }

    public static GeoSearchConfig getGeoSearchConfig() {
        GeoSearchConfig geoConfig = new GeoSearchConfig();
        
        geoConfig.addFieldBitMask(LOCATION_FIELD, (byte)1);
        
        return geoConfig;
    }

    
    /**
     * Builds a document with given text and title and two locations whose lat/long are given
     * by index * (MAXVALUE - MINVALUE) / 10 + MINVALUE 
     * and (index * 2 + 1) * (MAXVALUE - MINVALUE) / 20 
     * @param text
     * @param title
     * @param index
     * @return
     */
    Document buildDocument(String text, String title, int index, int maxIndex) {
        Document document = new Document();
        
        IndexableField textField = new TextField(TEXT_FIELD, text, Store.NO);
        IndexableField titleField = new TextField(TITLE_FIELD, title, Store.YES);
        
        GeoCoordinate geoCoordinate = calculateGeoCoordinate(2 * index, 2 * maxIndex + 1); 
        GeoCoordinateField locationField = new GeoCoordinateField(LOCATION_FIELD, geoCoordinate);
        
        GeoCoordinate imageGeoCoordinate = calculateGeoCoordinate(2 * index + 1, 2 * maxIndex + 1); 
        GeoCoordinateField imageLocationField = new GeoCoordinateField(IMAGE_LOCATION_FIELD, imageGeoCoordinate);
        
        document.add(textField);
        document.add(titleField);
        document.add(locationField);
        document.add(imageLocationField);
        
        return document;
    }
    
    GeoCoordinate calculateGeoCoordinate(int index, int maxIndex) {
        float lattitude = (index * (LATTITUDE_MAX_VALUE - LATTITUDE_MIN_VALUE) / maxIndex) + LATTITUDE_MIN_VALUE;
        float longitude = (index * (LONGITUDE_MAX_VALUE - LONGITUDE_MIN_VALUE) / maxIndex) + LONGITUDE_MIN_VALUE;
        
        return new GeoCoordinate(lattitude, longitude);
    }
    
    void verifyExpectedResults(List<String> expectedTitles, TopDocs topDocs, IndexSearcher searcher) throws CorruptIndexException, IOException {
        assertEquals("Unexpected number of hits", expectedTitles.size(), topDocs.totalHits);
        
        for (int i = 0; i < expectedTitles.size(); i++) {
            String expectedTitle = expectedTitles.get(i);
            Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
            String title = doc.get("title");
            assertEquals(expectedTitle, title);
        }
    }
    
    int countExtensions(Directory directory, String ext) throws IOException {
        String[] fileNames = directory.listAll();
        
        int  fileCount = 0;
        for (String fileName: fileNames) { 
            if (fileName.endsWith("." + ext)) {
                fileCount++;
            }
        }
        
        return fileCount;
    }
    
    protected void verifyFilter(String geoFileName, int maxDocs) throws IOException {
        MappedFieldNameFilterConverter fieldNameConverter = buildFieldNameConverter(geoFileName, maxDocs);
        
        //Verify mapping is correct
        List<String> filterFields = fieldNameConverter.getFields(LOCATION_BIT_MASK);
        assertEquals("Expected a single field that maps to the bit mask", 1, filterFields.size());
        assertEquals(LOCATION_FIELD, filterFields.get(0));
        
        filterFields = fieldNameConverter.getFields(IMAGE_LOCATION_BIT_MASK);
        assertEquals("Expected a single field that maps to the bit mask", 1, filterFields.size());
        assertEquals(IMAGE_LOCATION_FIELD, filterFields.get(0));
        
        //verify that half the documents have one filter and the other half have the other
        int countImageLocationFiltered = 0;
        int countLocationFiltered = 0;
        
        GeoSegmentReader<CartesianGeoRecord> reader = new GeoSegmentReader<CartesianGeoRecord>(directory, 
                geoFileName, maxDocs, IOContext.READ, geoRecordSerializer, geoComparator);
        Iterator<CartesianGeoRecord> geoIter = reader.getIterator(CartesianGeoRecord.MIN_VALID_GEORECORD, CartesianGeoRecord.MAX_VALID_GEORECORD);
        while (geoIter.hasNext()) {
            CartesianGeoRecord geoRecord = geoIter.next();
            if (fieldNameConverter.fieldIsInFilter(LOCATION_FIELD, geoRecord.filterByte)) {
                countLocationFiltered++;
            }
            
            if (fieldNameConverter.fieldIsInFilter(IMAGE_LOCATION_FIELD, geoRecord.filterByte)) {
                countImageLocationFiltered++;
            }
        }
        
        assertEquals("Expected one point per doc to be filtered by default location", maxDocs, countLocationFiltered);
        assertEquals("Expected one point per doc to be filtered by image location", maxDocs, countImageLocationFiltered);
    }
    
    protected MappedFieldNameFilterConverter buildFieldNameConverter(String geoFileName, int maxDoc) throws IOException {
        GeoSegmentReader<CartesianGeoRecord> segmentReader = new GeoSegmentReader<CartesianGeoRecord>(directory, geoFileName, maxDoc, 
                IOContext.READ, geoRecordSerializer, geoComparator);
        
        DataInput input = directory.openInput(geoFileName, IOContext.READ);
        input.readVInt(); //throw out version
        input.readInt();   //throw out tree position
        input.readVInt();  //throw out the data size
        input.readVInt();  //throw out tree name    
        
        MappedFieldNameFilterConverter fieldNameConverter = new MappedFieldNameFilterConverter();
        fieldNameConverter.loadFromInput(input);
        
        return fieldNameConverter;
    }
    
    /**
     * A simple Merge Policy that never forces a merge and always uses CFS.
     * @author gcooney
     *
     */
    private class MergeOnOptimizeOnly extends MergePolicy {
        private boolean useCompoundFileFormat;

        public MergeOnOptimizeOnly(boolean useCompoundFileFormat) {
            this.useCompoundFileFormat = useCompoundFileFormat;
        }
        
        @Override
        public MergeSpecification findForcedDeletesMerges(SegmentInfos segmentInfos) throws CorruptIndexException,
                IOException {
            return null;
        }

        @Override
        public void close() {
        }

        @Override
        public MergeSpecification findMerges(MergeTrigger mergeTrigger, SegmentInfos segmentInfos) throws IOException {
            return null;
        }

        @Override
        public MergeSpecification findForcedMerges(SegmentInfos segmentInfos, int maxSegmentCount,
                Map<SegmentInfoPerCommit, Boolean> segmentsToMerge) throws IOException {
            List<SegmentInfoPerCommit> activeSegmentsToOptimize = new Vector<SegmentInfoPerCommit>();
            for (SegmentInfoPerCommit segmentInfo: segmentInfos) {
                if (segmentsToMerge.get(segmentInfo)) {
                    activeSegmentsToOptimize.add(segmentInfo);
                }
            }
            
            MergeSpecification mergeSpec = new MergeSpecification();
            
            if (activeSegmentsToOptimize.size() > 0) {
                OneMerge merge = new OneMerge(activeSegmentsToOptimize);
                mergeSpec.add(merge);
            }
        
            return mergeSpec;
        }

        @Override
        public boolean useCompoundFile(SegmentInfos segments, SegmentInfoPerCommit newSegment) throws IOException {
            return useCompoundFileFormat;
        }
    }
}
