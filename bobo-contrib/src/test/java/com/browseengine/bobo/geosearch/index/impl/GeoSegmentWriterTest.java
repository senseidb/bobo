package com.browseengine.bobo.geosearch.index.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Comparator;
import java.util.TreeSet;

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
import com.browseengine.bobo.geosearch.IGeoRecordSerializer;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.bo.GeoSegmentInfo;


/**
 * @author Geoff Cooney
 */
@RunWith(SpringJUnit4ClassRunner.class) 
@ContextConfiguration( { "/TEST-servlet.xml" }) 
@IfProfileValue(name = "test-suite", values = { "unit", "all" }) 
public class GeoSegmentWriterTest {
    Mockery context = new Mockery() {{ 
        setImposteriser(ClassImposteriser.INSTANCE);
    }};
    
    Sequence outputSequence = context.sequence("output");
    
    Directory directory;
    IndexOutput mockOutput;
    
    // can't use the spring bean, because herein we mock
    GeoSearchConfig config = new GeoSearchConfig();
    IGeoRecordSerializer<CartesianGeoRecord> geoRecordSerializer;
    
    TreeSet<CartesianGeoRecord> treeSet;
    GeoSegmentInfo info;

    IFieldNameFilterConverter fieldNameFilterConverter;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        directory = context.mock(Directory.class);
        mockOutput = context.mock(IndexOutput.class);
        fieldNameFilterConverter = context.mock(IFieldNameFilterConverter.class);
        
        geoRecordSerializer = context.mock(IGeoRecordSerializer.class);
        
        config.setGeoFileExtension("gto");
        
        treeSet = new TreeSet<CartesianGeoRecord>(new Comparator<CartesianGeoRecord>() {

            @Override
            public int compare(CartesianGeoRecord o1, CartesianGeoRecord o2) {
                return (int) (o1.lowOrder - o2.lowOrder);
            }
        });
        
        String segmentName = "01";
        info = new GeoSegmentInfo();
        info.setFieldNameFilterConverter(fieldNameFilterConverter);
        info.setGeoVersion(GeoVersion.CURRENT_VERSION);
        info.setSegmentName(segmentName);
    }
    
    @After
    public void tearDown() {
        context.assertIsSatisfied();
    }
    
    @Test
    public void createOutputBTree() throws IOException, InvalidTreeSizeException {
        final int docsToAdd = 20;
        buildTreeSet(docsToAdd);
        
        doCreateAndTest(docsToAdd);
    }
    
    @Test
    public void createOutputBTree_V2() throws IOException, InvalidTreeSizeException {
        final int docsToAdd = 20;
        buildTreeSet(docsToAdd);
        
        doCreateAndTest(docsToAdd, GeoVersion.VERSION_1);
    }
    
    @Test
    public void createOutputBTree_WithValueMapping() throws IOException, InvalidTreeSizeException {
        info.setFieldNameFilterConverter(fieldNameFilterConverter);
        
        final int docsToAdd = 20;
        
        buildTreeSet(docsToAdd);
        
        doCreateAndTest(docsToAdd);
    }
    
    @Test
    public void treeTooLarge() throws IOException {
        final int docsToAdd = 20;
        int version = GeoVersion.VERSION_0;
        buildTreeSet(docsToAdd);
        
        
        buildWriteExpectations(docsToAdd, docsToAdd + 1, version);
        
        info.setGeoVersion(version);
        String fileName = config.getGeoFileName(info.getSegmentName());
        
        try {
            GeoSegmentWriter<CartesianGeoRecord> bTree = new GeoSegmentWriter<CartesianGeoRecord>(docsToAdd + 1, treeSet.iterator(), 
                    directory, fileName, info, geoRecordSerializer);
            fail("Expected InvalidTreeSizeException but it did not occur");
        } catch (InvalidTreeSizeException e) {
            assertEquals("Expected tree size to be as specified", docsToAdd + 1, e.getTreeSize());
            assertEquals("Expected recordSize to equal records in iterator", docsToAdd, e.getRecordSize());
        }
    }
    
    @Test
    public void treeTooSmall() throws IOException {
        final int docsToAdd = 20;
        int version = GeoVersion.VERSION_0;
        buildTreeSet(docsToAdd);
        
        
        buildWriteExpectations(docsToAdd, docsToAdd - 1, version);
        
        info.setGeoVersion(version);
        String fileName = config.getGeoFileName(info.getSegmentName());
        
        try {
            GeoSegmentWriter<CartesianGeoRecord> bTree = new GeoSegmentWriter<CartesianGeoRecord>(docsToAdd - 1, treeSet.iterator(), 
                    directory, fileName, info, geoRecordSerializer);
            fail("Expected InvalidTreeSizeException but it did not occur");
        } catch (InvalidTreeSizeException e) {
            assertEquals("Expected tree size to be as specified", docsToAdd -1, e.getTreeSize());
            assertEquals("Expected recordSize to equal records in iterator", docsToAdd, e.getRecordSize());
        }
    }
    
    private void buildTreeSet(int docsToAdd) {
        for (int i=0; i<docsToAdd; i++) {
            long highOrder = 0;
            int lowOrder = i;
            byte filterByte = CartesianGeoRecord.DEFAULT_FILTER_BYTE; 
            
            CartesianGeoRecord record = new CartesianGeoRecord(highOrder, lowOrder, filterByte);
            treeSet.add(record);
        }
    }
    
    private void doCreateAndTest(final int docsToAdd) throws IOException, InvalidTreeSizeException {
        doCreateAndTest(docsToAdd, GeoVersion.VERSION_0);
    }
    
    private void doCreateAndTest(final int docsToAdd, final int version) throws IOException, InvalidTreeSizeException {
        buildWriteExpectations(docsToAdd, docsToAdd, version);
        
        context.checking(new Expectations() {
            {
                //close
                one(mockOutput).close();
            }
        });
        
        info.setGeoVersion(version);
        String fileName = config.getGeoFileName(info.getSegmentName());
        GeoSegmentWriter<CartesianGeoRecord> bTree = new GeoSegmentWriter<CartesianGeoRecord>(treeSet, directory, 
                fileName, info, geoRecordSerializer);
        bTree.close();
        context.assertIsSatisfied();
    }
    
    private void buildWriteExpectations(final int docsToAdd, final int expectedDocs, final int version) throws IOException {
        final byte[] byteBuf = new byte[10];
        final Class<?> clazz = byteBuf.getClass();
        
        context.assertIsSatisfied();  //we should have no calls to mock Objects before we flush
        context.checking(new Expectations() {
            {
                ignoring(mockOutput).getFilePointer();
                will(returnValue(1L));
                
                //get output
                one(directory).createOutput(info.getSegmentName() + "." + config.getGeoFileExtension());
                will(returnValue(mockOutput));
                inSequence(outputSequence);

                //write file header
                one(mockOutput).writeVInt(version);
                inSequence(outputSequence);
                one(mockOutput).writeInt(0);
                inSequence(outputSequence);
                one(mockOutput).writeVInt(expectedDocs);
                inSequence(outputSequence);
                if(version > GeoVersion.VERSION_0) {
                    one(mockOutput).writeVInt(GeoSegmentInfo.BYTES_PER_RECORD_V1);
                    inSequence(outputSequence);
                }
                one(fieldNameFilterConverter).writeToOutput(mockOutput);
                inSequence(outputSequence);
                
                one(mockOutput).seek(1);
                inSequence(outputSequence);
                one(mockOutput).writeInt(1);
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
                for (int i = 0; i < Math.min(docsToAdd, expectedDocs); i++) {
                    
                    one(mockOutput).seek(with(any(Long.class)));
                    inSequence(outputSequence);
                    one(geoRecordSerializer).writeGeoRecord(with(mockOutput), with(any(CartesianGeoRecord.class)), with(any(Integer.class)));
                    inSequence(outputSequence);
                }
            }
        });
    }
}
