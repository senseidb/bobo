package com.browseengine.bobo.geosearch.index.impl;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexOutput;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.browseengine.bobo.geosearch.GeoVersion;
import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinate;
import com.browseengine.bobo.geosearch.index.bo.GeoCoordinateField;

/**
 * @author Geoff Cooney
 */
@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration( { "/TEST-servlet.xml" }) 
@IfProfileValue(name = "test-suite", values = { "unit", "all" }) 
public class GeoIndexerTest {
    Mockery context = new Mockery() {{ 
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
    Sequence outputSequence = context.sequence("output");
    
    Directory directory;
    IndexOutput mockOutput;
    
    GeoIndexer geoIndexer;
    
    //@Resource(type = GeoSearchConfig.class)
    GeoSearchConfig config = new GeoSearchConfig();
    
    String locationField = "location1";
    byte locationFilterByte = (byte)1;
    String unmappedLocation = "location2";
    
    String segmentName = "0a";
    
    IFieldNameFilterConverter fieldNameFilterConverter;
    
    @Before
    public void setUp() {
        directory = context.mock(Directory.class);
        
        mockOutput = context.mock(IndexOutput.class);
        
        fieldNameFilterConverter = context.mock(IFieldNameFilterConverter.class);
        config.setFieldNameFilterConverter(fieldNameFilterConverter);
        
        geoIndexer = new GeoIndexer(config);
    }
    
    @After
    public void tearDown() {
        context.assertIsSatisfied();
    }
    
    @Test
    public void testFlush_NoIndex() throws IOException {
        addIgnoreFieldNameConverterExpectation();
        context.checking(new Expectations() {
            {
                ignoring(directory).createOutput(segmentName + "." + config.getGeoFileExtension());
                will(returnValue(mockOutput));
                
                ignoring(mockOutput).getFilePointer();
                ignoring(mockOutput).seek(with(any(Integer.class)));
                
                ignoring(fieldNameFilterConverter).writeToOutput(mockOutput);
                
                one(mockOutput).writeVInt(GeoVersion.CURRENT_VERSION);
                one(mockOutput).writeVInt(0);
                one(mockOutput).writeInt(0);
                one(mockOutput).writeInt(with(any(Integer.class)));
                
                one(mockOutput).close();
            }
        });
        
        geoIndexer.flush(directory, segmentName);
    }
    
    @Test
    public void testIndexAndFlush_twoFields() throws IOException {
        final int docsToAdd = 20;
        final int locationFieldDocs = 10;
        
        addIgnoreFieldNameConverterExpectation();
        
        for (int i=0; i<docsToAdd; i++) {
            float lattitide = (float)Math.random();
            float longitude = (float)Math.random();
            String fieldName = i < locationFieldDocs ? locationField : unmappedLocation;
            
            GeoCoordinate geoCoordinate = new GeoCoordinate(lattitide, longitude);
            GeoCoordinateField field = new GeoCoordinateField(fieldName, geoCoordinate);
            geoIndexer.index(i, field);
        }
        
        doFlushAndTest(docsToAdd, locationFieldDocs);
    }

    @Test
    public void testIndexAndFlush_oneDocId() throws IOException {
        int docId = 0;
        
        final int docsToAdd = 20;
        final int locationFieldDocs = 10;
        
        addIgnoreFieldNameConverterExpectation();
        
        for (int i=0; i<docsToAdd; i++) {
            float lattitide = (float)Math.random();
            float longitude = (float)Math.random();
            String fieldName = i < locationFieldDocs ? locationField : unmappedLocation;
            
            GeoCoordinate geoCoordinate = new GeoCoordinate(lattitide, longitude);
            GeoCoordinateField field = new GeoCoordinateField(fieldName, geoCoordinate);
            geoIndexer.index(docId, field);
        }
        
        doFlushAndTest(docsToAdd, locationFieldDocs);
    }
    
    @Test
    public void testIndexAndFlush_multipleThreads() throws InterruptedException, IOException {
        final int docsToAddPerThread = 10;
        final int locationFieldDocsPerThread = 5;
        final int numThreads = 10;
        
        addIgnoreFieldNameConverterExpectation();
        
        final CountDownLatch latch = new CountDownLatch(numThreads);
        for (int i=0; i < numThreads; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i=0; i<docsToAddPerThread; i++) {
                            float lattitide = (float)Math.random();
                            float longitude = (float)Math.random();
                            String fieldName = i < locationFieldDocsPerThread ? locationField : unmappedLocation;
                            
                            GeoCoordinate geoCoordinate = new GeoCoordinate(lattitide, longitude);
                            GeoCoordinateField field = new GeoCoordinateField(fieldName, geoCoordinate);
                            geoIndexer.index(i, field);
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            };
            
            Thread thread = new Thread(runnable);
            thread.start();
        }
        
        latch.await(500, TimeUnit.MILLISECONDS);
        
        doFlushAndTest(docsToAddPerThread * numThreads, locationFieldDocsPerThread * numThreads);
    }
    
    /**
     * WARNING TO FUTURE DEVELOPERS:  This test will fail if docsToAdd is > 127.  The issue is that
     * writeVInt in IndexOutput is final and cannot be mocked correctly.  It looks to me like JMock
     * actually just adds an expectation on the first call that writeVint makes.  This works ok
     * for variable int encodings if they take up only one byte but is problematic once the encodings 
     * take up more than that.
     * 
     * @param docsToAdd
     * @param locationFieldDocs
     * @throws IOException
     */
    private void doFlushAndTest(final int docsToAdd, final int locationFieldDocs) throws IOException {
        final byte[] byteBuf = new byte[10];
        
        context.assertIsSatisfied();  //we should have no calls to mock Objects before we flush
        context.checking(new Expectations() {
            {
                ignoring(mockOutput).getFilePointer();
                
                //get output
                one(directory).createOutput(segmentName + "." + config.getGeoFileExtension());
                will(returnValue(mockOutput));

                //write file header
                one(mockOutput).writeVInt(GeoVersion.CURRENT_VERSION);
                inSequence(outputSequence);
                one(mockOutput).writeInt(0);
                inSequence(outputSequence);
                one(mockOutput).writeVInt(docsToAdd);
                inSequence(outputSequence);
                one(fieldNameFilterConverter).writeToOutput(mockOutput);
                inSequence(outputSequence);

                one(mockOutput).seek(with(any(Integer.class)));
                inSequence(outputSequence);
                one(mockOutput).writeInt(with(any(Integer.class)));
                inSequence(outputSequence);
                
                // fill zeroes
                one(mockOutput).length();
                will(returnValue(7L));
                inSequence(outputSequence);
                one(mockOutput).seek(with(any(Long.class)));
                inSequence(outputSequence);
                one(mockOutput).writeBytes(with(any(byteBuf.getClass())), with(any(Integer.TYPE)), with(any(Integer.TYPE)));
                inSequence(outputSequence);
                one(mockOutput).seek(with(any(Long.class)));
                inSequence(outputSequence);
                one(mockOutput).length();
                will(returnValue((long)(7+13*docsToAdd)));
                inSequence(outputSequence);

                
                //write actual tree
                exactly(docsToAdd).of(mockOutput).seek(with(any(Long.class)));
                exactly(docsToAdd).of(mockOutput).writeLong(with(any(Long.class)));
                exactly(docsToAdd).of(mockOutput).writeInt(with(any(Integer.class)));
                exactly(docsToAdd).of(mockOutput).writeByte(GeoRecord.DEFAULT_FILTER_BYTE);
                
                //close
                one(mockOutput).close();
            }
        });
        
        geoIndexer.flush(directory, segmentName);
        context.assertIsSatisfied();
    }
    
    @Test
    public void testAbort() throws IOException {
        int docId = 0;
        
        final int docsToAdd = 20;
        final int locationFieldDocs = 10;
        
        addIgnoreFieldNameConverterExpectation();
        
        for (int i=0; i<docsToAdd; i++) {
            float lattitide = (float)Math.random();
            float longitude = (float)Math.random();
            String fieldName = i < locationFieldDocs ? locationField : unmappedLocation;
            
            GeoCoordinate geoCoordinate = new GeoCoordinate(lattitide, longitude);
            GeoCoordinateField field = new GeoCoordinateField(fieldName, geoCoordinate);
            geoIndexer.index(docId, field);
        }
        
        geoIndexer.abort();
    }
    
    public void addIgnoreFieldNameConverterExpectation() {
        context.checking(new Expectations() {
            {
                ignoring(fieldNameFilterConverter).getFilterValue(with(any(String[].class)));
            }
        });
    }
}
