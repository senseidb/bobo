/**
 * 
 */
package com.browseengine.bobo.geosearch.query;

import java.io.IOException;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;

import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.impl.GeoUtil;
import com.browseengine.bobo.geosearch.score.impl.Conversions;

/**
 * @author Shane Detsch
 * @author Ken McCracken
 *
 */
public class GeoQuery extends Query {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    float rangeInKm;
    GeoConverter geoConvertor;
    
    private static final float MINIMUM_RANGE_IN_KM = 0.001f;
    private static final float MAXIMUM_RANGE_IN_KM = 700f;

    double longRadians;
    double latRadians;
    
    public GeoQuery(double centroidLatitude, double centroidLongitude, Float rangeInKm) {
        latRadians = Conversions.d2r(centroidLatitude);
        longRadians = Conversions.d2r(centroidLongitude);
        this.rangeInKm = rangeInKm;
        
        if (null == rangeInKm) {
            throw new RuntimeException("please specify rangeInKilometers");
        }
       
        if (this.rangeInKm < MINIMUM_RANGE_IN_KM || this.rangeInKm > MAXIMUM_RANGE_IN_KM) {
            throw new RuntimeException("rangeInMiles out of range ["+MINIMUM_RANGE_IN_KM+", "+MAXIMUM_RANGE_IN_KM+"]: "+this.rangeInKm);
        }
        
        if(!GeoUtil.isValidLatitude(centroidLatitude)  || !GeoUtil.isValidLongitude(centroidLongitude)) {
            throw new RuntimeException("bad latitude or longitude: " + centroidLatitude + ", " + centroidLongitude );
        }
      
    }
    
    /**
     * @return the centroidX
     */
    public double getCentroidLongitude() {
        return longRadians;
    }
    /**
     * @return the centroidX
     */
    public double getCentroidLatitude() {
        return latRadians;
    }
    
    public float getRangeInKm() {
        return this.rangeInKm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Weight createWeight(Searcher searcher) throws IOException {
        return new GeoWeight(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "GeoQuery [centroidLatitude=" + latRadians + ", centroidLongitude=" + longRadians 
                + ", rangeInKm =" + rangeInKm + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(String arg0) {
        return toString();
    }

    

}
