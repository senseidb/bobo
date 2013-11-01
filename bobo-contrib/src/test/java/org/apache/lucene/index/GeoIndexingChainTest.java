package org.apache.lucene.index;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DocumentsWriterPerThread.IndexingChain;
import org.apache.lucene.index.FieldInfos.FieldNumbers;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.codec.GeoCodec;


/**
 * @author Geoff Cooney
 */
@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration( { "/TEST-servlet.xml" }) 
@IfProfileValue(name = "test-suite", values = { "unit", "all" })
public class GeoIndexingChainTest {
    private final Mockery context = new Mockery() {{
        setImposteriser(ClassImposteriser.INSTANCE);  
    }};
    
    private IndexingChain mockIndexingChain;
    private DocConsumer mockDocConsumer;
    
    private GeoIndexingChain geoIndexingChain;
    private DocumentsWriterPerThread docWriterPerThread;
    //@Resource(type = GeoSearchConfig.class)
    GeoSearchConfig config = new GeoSearchConfig();
    
    @Before
    public void setUp() throws IOException {
        mockIndexingChain = context.mock(IndexingChain.class);
        mockDocConsumer = context.mock(DocConsumer.class);
        Directory dir = new RAMDirectory();
        DocumentsWriter writer = buildDocumentsWriter();
        docWriterPerThread = new DocumentsWriterPerThread(dir, 
                writer, null, DocumentsWriterPerThread.defaultIndexingChain);
        
        geoIndexingChain = new GeoIndexingChain(config, mockIndexingChain);
    }
    
    @After
    public void tearDown() {
        context.assertIsSatisfied();
    }
    
    @Test
    public void testGetChain() {
        context.checking(new Expectations() {
            {
                one(mockIndexingChain).getChain(docWriterPerThread);
                will(returnValue(mockDocConsumer));
            }
        });
        
        DocConsumer docConsumer = geoIndexingChain.getChain(docWriterPerThread);
        assertTrue("Expected a GeoDocConsumer", docConsumer instanceof GeoDocConsumer);
        GeoDocConsumer geoDocConsumer = (GeoDocConsumer)docConsumer;
        assertSame("GeoDocConsumer's default consumer was not set correctly", 
                mockDocConsumer, geoDocConsumer.getDefaultDocConsumer());
    }
         
    private DocumentsWriter buildDocumentsWriter() throws IOException {
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
        Directory directory = new RAMDirectory();
//        writer = context.mock(IndexWriter.class);
        IndexWriter writer = new IndexWriter(directory, config);
        BufferedDeletesStream bufferedDeletesStream = context.mock(BufferedDeletesStream.class);
        
        DocumentsWriter documentsWriter = new DocumentsWriter(new GeoCodec(), config, directory, 
                writer, new FieldNumbers(), bufferedDeletesStream);
        return documentsWriter;
    }
}
