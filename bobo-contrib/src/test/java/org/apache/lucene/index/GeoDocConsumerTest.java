package org.apache.lucene.index;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.FieldInfos.FieldNumbers;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.codec.GeoCodec;
import com.browseengine.bobo.geosearch.index.IGeoIndexer;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinate;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinateField;

/**
 * @author Geoff Cooney
 */
//@RunWith(SpringJUnit4ClassRunner.class) 
//@ContextConfiguration( { "/TEST-servlet.xml" }) 
//@IfProfileValue(name = "test-suite", values = { "unit", "all" })
public class GeoDocConsumerTest {
    private final Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);  
    }};
    
    private DocConsumer mockDocConsumer;
    
    private IGeoIndexer mockGeoIndexer;
    
    private GeoDocConsumer geoDocConsumer;

    DocumentsWriterPerThread documentsWriterPerThread;
    DocumentsWriter documentsWriter;
    
    final int docID = 10; 
    Document document;
    
    Version matchVersion = Version.LUCENE_CURRENT;
    Analyzer analyzer = new StandardAnalyzer(matchVersion);
    Directory directory;
    IndexWriter writer;
    FieldInfos.Builder fieldInfos;
    BufferedDeletesStream bufferedDeletesStream;

    //@Resource(type = GeoSearchConfig.class)
    GeoSearchConfig config = new GeoSearchConfig();
    
    @Before
    public void setUp() throws IOException {
        mockDocConsumer = context.mock(DocConsumer.class);
        
        mockGeoIndexer = context.mock(IGeoIndexer.class);

        documentsWriter = buildDocumentsWriter();
        DocumentsWriterPerThread documentsWriterPerThread = new DocumentsWriterPerThread(directory, 
                documentsWriter, fieldInfos, DocumentsWriterPerThread.defaultIndexingChain);
        
        geoDocConsumer = new GeoDocConsumer(config, mockDocConsumer, documentsWriterPerThread);
        geoDocConsumer.setGeoIndexer(mockGeoIndexer);
        
        document = buildDocument();
        
        documentsWriterPerThread.docState.doc = document;
        documentsWriterPerThread.docState.docID = docID;
    }
    
    @After
    public void tearDown() {
        context.assertIsSatisfied();
    }
    
    private DocumentsWriter buildDocumentsWriter() throws IOException {
        analyzer = new StandardAnalyzer(matchVersion);
        IndexWriterConfig config = new IndexWriterConfig(matchVersion, analyzer);
        Directory directory = new RAMDirectory();
//        writer = context.mock(IndexWriter.class);
        writer = new IndexWriter(directory, config);
        fieldInfos = new FieldInfos.Builder();
        BufferedDeletesStream bufferedDeletesStream = context.mock(BufferedDeletesStream.class);
        
        DocumentsWriter documentsWriter = new DocumentsWriter(new GeoCodec(), config, directory, 
                writer, new FieldNumbers(), bufferedDeletesStream);
        return documentsWriter;
    }
    
    private Document buildDocument() {
        Document document = new Document();
        document.add(new TextField("text", "my text", Store.NO));
        document.add(new TextField("text", "more text", Store.NO));
        document.add(new TextField("title", "A good title", Store.NO));
        
        return document;
    }
    
    @Test
    public void testProcessDocument_NoGeoFields() throws IOException {
        context.checking(new Expectations() {
            {
                one(mockDocConsumer).processDocument(fieldInfos);
                
                never(mockGeoIndexer);
            }
        });
        
        geoDocConsumer.processDocument(fieldInfos);
    }
      
    
    @Test
    public void testProcessDocument_TwoGeoFields() throws IOException {
        final String geoFieldName1 = "location1";
        final GeoCoordinate geoCoordinate1 = new GeoCoordinate(45.0f, 45.0f);
        final GeoCoordinateField geoField1 = new GeoCoordinateField(geoFieldName1, geoCoordinate1); 
        document.add(geoField1);
        
        final String geoFieldName2 = "location2";
        final GeoCoordinate geoCoordinate2 = new GeoCoordinate(45.0f, 45.0f);
        final GeoCoordinateField geoField2 = new GeoCoordinateField(geoFieldName2, geoCoordinate2);
        document.add(geoField2);
        
        context.checking(new Expectations() {
            {
                one(mockDocConsumer).processDocument(fieldInfos);
                
                one(mockGeoIndexer).index(docID, geoField1);
                one(mockGeoIndexer).index(docID, geoField2);
            }
        });
        
        geoDocConsumer.processDocument(fieldInfos);
    }
    
    @Test
    public void testProcessDocument_TwoGeoFields_SameName() throws IOException {
        final String geoFieldName1 = "location1";
        final GeoCoordinate geoCoordinate1 = new GeoCoordinate(45.0f, 45.0f);
        final GeoCoordinateField geoField1 = new GeoCoordinateField(geoFieldName1, geoCoordinate1); 
        document.add(geoField1);
        
        final GeoCoordinate geoCoordinate2 = new GeoCoordinate(45.0f, 45.0f);
        final GeoCoordinateField geoField2 = new GeoCoordinateField(geoFieldName1, geoCoordinate2);
        document.add(geoField2);
        
        context.checking(new Expectations() {
            {
                one(mockDocConsumer).processDocument(fieldInfos);
                
                one(mockGeoIndexer).index(docID, geoField1);
                one(mockGeoIndexer).index(docID, geoField2);
            }
        });
        
        geoDocConsumer.processDocument(fieldInfos);
    }
    
    @Test
    public void testAbort() throws IOException {
        context.checking(new Expectations() {
            {
                one(mockDocConsumer).abort();
                
                one(mockGeoIndexer).abort();
            }
        });
        
        geoDocConsumer.abort();
    }
    
    @Test
    public void testFlush() throws IOException {
        final String segmentName = "segmentA";
        BufferedDeletes bufferedDeletes = new BufferedDeletes();
        SegmentInfo segmentInfo = new SegmentInfo(directory, "v1", segmentName, 10, 
                true, new GeoCodec(new GeoSearchConfig()), null, null);
        final SegmentWriteState segmentWriteState = new SegmentWriteState(null, directory, segmentInfo , fieldInfos.finish(), 
                0, bufferedDeletes, IOContext.DEFAULT);
        context.checking(new Expectations() {
            {
                one(mockDocConsumer).flush(segmentWriteState);
                
                one(mockGeoIndexer).flush(segmentWriteState);
            }
        });
        
        geoDocConsumer.flush(segmentWriteState);
    }
    
}
