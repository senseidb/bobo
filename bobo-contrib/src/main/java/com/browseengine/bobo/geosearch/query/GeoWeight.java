/**
 * 
 */
package com.browseengine.bobo.geosearch.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.Bits;

import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.index.impl.GeoAtomicReader;
import com.browseengine.bobo.geosearch.index.impl.GeoIndexReader;
import com.browseengine.bobo.geosearch.index.impl.GeoSegmentReader;

/**
 * @author Shane Detsch
 * @author Ken McCracken
 *
 */
public class GeoWeight extends Weight {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private final GeoQuery geoQuery;
    private float queryWeight;
    private float queryNorm;
    private float value;

    public GeoWeight(GeoQuery geoQuery) {
        this.geoQuery = geoQuery;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Query getQuery() {
        return geoQuery;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Explanation explain(AtomicReaderContext context, int doc) throws IOException {
        return new Explanation(doc, geoQuery.toString()+", queryNorm: "+queryNorm);
    }

    @Override
    public float getValueForNormalization() throws IOException {
        // idf is effectively 1
        queryWeight = geoQuery.getBoost();
        return queryWeight * queryWeight;
    }

    @Override
    public void normalize(float norm, float topLevelBoost) {
        this.queryNorm = queryNorm * topLevelBoost;
        queryWeight *= queryNorm;                   // normalize query weight
        // idf is effectively 1
        value = queryWeight;                  // idf for document
    }

    @Override
    public Scorer scorer(AtomicReaderContext context, boolean scoreDocsInOrder, 
            boolean topScorer, Bits acceptDocs)
            throws IOException {
        //TODO: Should we be behaving differently if topScorer is true
        List<AtomicReaderContext> leaves = context.leaves();
        
        List<GeoSegmentReader<CartesianGeoRecord>> segmentsInOrder = 
                new ArrayList<GeoSegmentReader<CartesianGeoRecord>>(leaves.size());
        
        for (AtomicReaderContext leafContext : leaves) {
            AtomicReader reader = leafContext.reader();
            if (!(reader instanceof GeoAtomicReader)) {
                throw new RuntimeException("attempt to create a "
                        +GeoScorer.class+" with a reader that was not a "
                        +GeoIndexReader.class);
            }
            
            GeoAtomicReader geoIndexReader = (GeoAtomicReader) reader;
            segmentsInOrder.add(geoIndexReader.getGeoSegmentReader());
        }
        
        
        return new GeoScorer(this, segmentsInOrder, acceptDocs, 
                geoQuery.getCentroidLatitude(), geoQuery.getCentroidLongitude(), geoQuery.rangeInKm);
    }
    
}
