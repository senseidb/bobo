/**
 * 
 */
package com.browseengine.bobo.geosearch.query;

import java.io.IOException;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Weight;

import com.browseengine.bobo.geosearch.impl.GeoUtil;

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
    
    double centroidLongitude;
    double centroidLatitude;
    float rangeInMiles;
    
    private static final float KM_TO_MILES = (float)(3.1/5);
    private static final float MINIMUM_RANGE_IN_MILES = 0.001f;
    private static final float MAXIMUM_RANGE_IN_MILES = 500f;
    
    public GeoQuery(double centroidLongitude, double centroidLatitude, Float rangeInMiles, Float rangeInKilometers) {
        this.centroidLongitude = centroidLongitude;
        this.centroidLatitude = centroidLatitude;
        if (!(null == rangeInMiles ^ null == rangeInKilometers)) {
            throw new RuntimeException("please specify either rangeInMiles or rangeInKilometers");
        }
        if (null != rangeInKilometers) {
            this.rangeInMiles = KM_TO_MILES * rangeInKilometers;
        } else {
            this.rangeInMiles = rangeInMiles;
        }
        if (this.rangeInMiles < MINIMUM_RANGE_IN_MILES || this.rangeInMiles > MAXIMUM_RANGE_IN_MILES) {
            throw new RuntimeException("rangeInMiles out of range ["+MINIMUM_RANGE_IN_MILES+", "+MAXIMUM_RANGE_IN_MILES+"]: "+this.rangeInMiles);
        }
        if (!GeoUtil.isValidLongitude(centroidLongitude) || !GeoUtil.isValidLatitude(centroidLatitude)) {
            throw new RuntimeException("bad centroidLongitude "+centroidLongitude+" or centroidLatitude "+centroidLatitude);
        }
    }
    
    
    
    /**
     * @return the centroidLongitude
     */
    public double getCentroidLongitude() {
        return centroidLongitude;
    }



    /**
     * @return the centroidLatitude
     */
    public double getCentroidLatitude() {
        return centroidLatitude;
    }



    /**
     * @return the rangeInMiles
     */
    public float getRangeInMiles() {
        return rangeInMiles;
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
        return "GeoQuery [centroidLatitude=" + centroidLatitude + ", centroidLongitude=" + centroidLongitude
                + ", rangeInMiles=" + rangeInMiles + "]";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(String arg0) {
        return toString();
    }

    

}
