package org.apache.lucene.index;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.apache.lucene.index.DocumentsWriter.IndexingChain;
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

    //@Resource(type = GeoSearchConfig.class)
    GeoSearchConfig config = new GeoSearchConfig();
    
    @Before
    public void setUp() {
        mockIndexingChain = context.mock(IndexingChain.class);
        mockDocConsumer = context.mock(DocConsumer.class);
        
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
                one(mockIndexingChain).getChain(with(aNull(DocumentsWriter.class)));
                will(returnValue(mockDocConsumer));
            }
        });
        
        DocConsumer docConsumer = geoIndexingChain.getChain(null);
        assertTrue("Expected a GeoDocConsumer", docConsumer instanceof GeoDocConsumer);
        GeoDocConsumer geoDocConsumer = (GeoDocConsumer)docConsumer;
        assertSame("GeoDocConsumer's default consumer was not set correctly", 
                mockDocConsumer, geoDocConsumer.getDefaultDocConsumer());
    }
                                
}
