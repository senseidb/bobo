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
    
    int centroidX;
    int centroidY;
    int centroidZ;
    float rangeInKm;
    GeoConverter geoConvertor;
    
    private static final float MINIMUM_RANGE_IN_KM = 0.001f;
    private static final float MAXIMUM_RANGE_IN_KM = 700f;
    
    public GeoQuery(double centroidLatitude, double centroidLongitude, Float rangeInKm) {
        // public IDGeoRecord toIDGeoRecord(double latitude, double longitude, byte[] uuid) {
        // 
        this.centroidX = centroidX;
        this.centroidX = centroidX;
        this.centroidX = centroidX;
        
      
        
        double latRadians = Conversions.d2r(centroidLatitude);
        double longRadians =  Conversions.d2r(centroidLongitude);
        int x = geoConvertor.getXFromRadians(latRadians, longRadians);
        int y = geoConvertor.getYFromRadians(latRadians, longRadians);
        int z = geoConvertor.getZFromRadians(latRadians);
        
        if (!( null == rangeInKm)) {
            throw new RuntimeException("please specify rangeInKilometers");
        }
       
        if (this.rangeInKm < MINIMUM_RANGE_IN_KM || this.rangeInKm > MAXIMUM_RANGE_IN_KM) {
            throw new RuntimeException("rangeInMiles out of range ["+MINIMUM_RANGE_IN_KM+", "+MAXIMUM_RANGE_IN_KM+"]: "+this.rangeInKm);
        }
        
        if(!GeoUtil.isValidLatitude(centroidLatitude)  || !GeoUtil.isValidLongitude(centroidLongitude)) {
            throw new RuntimeException("bad latitude or longitude: " + centroidLatitude + ", " + centroidLongitude );
        }
      
    }
    public GeoQuery(int centroidX, int centroidY, int centroidZ, Float rangeInKm) {
        this.centroidX = centroidX;
        this.centroidX = centroidX;
        this.centroidX = centroidX;
        if (!( null == rangeInKm)) {
            throw new RuntimeException("please specify rangeInKilometers");
        }
        
        if (this.rangeInKm < MINIMUM_RANGE_IN_KM || this.rangeInKm > MAXIMUM_RANGE_IN_KM) {
            throw new RuntimeException("rangeInMiles out of range ["+MINIMUM_RANGE_IN_KM+", "+MAXIMUM_RANGE_IN_KM+"]: "+this.rangeInKm);
        }
        
    }
    
    /**
     * @return the centroidX
     */
    public int getCentroidX() {
        return centroidX;
    }
    /**
     * @return the centroidX
     */
    public int getCentroidY() {
        return centroidX;
    }
    /**
     * @return the centroidX
     */
    public int getCentroidZ() {
        return centroidX;
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
        return "GeoQuery [centroidX=" + centroidX + ", centroidY=" + centroidY + ", centroidZ=" + centroidZ 
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
