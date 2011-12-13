package com.browseengine.bobo.geosearch.index.bo;

import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.FieldInfo.IndexOptions;

/**
 * 
 * @author Geoff Cooney
 * @author Shane Detsch
 *
 */
public class GeoCoordinateField implements Fieldable {
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private final String fieldName;
    private GeoCoordinate geoCoordinate;
    private float boost = 1.0f;
    
    public GeoCoordinateField(String fieldName, GeoCoordinate geoCoordinate) {
        this.fieldName = fieldName;
        this.geoCoordinate = geoCoordinate;
    }
    
    public GeoCoordinate getGeoCoordinate() {
        return geoCoordinate;
    }
    
    public void setGeoCoordinate(GeoCoordinate geoCoordinate) {
        this.geoCoordinate = geoCoordinate;
    }
    
    @Override
    public String stringValue() {
        return geoCoordinate.getLatitude() + ", " + geoCoordinate.getLongitude();
    }

    /** 
     *  Returns always <code>null</code> for GeoCoordinate fields 
     *  Use getGeoCoordinate to retrieve results instead. 
     */
    @Override
    public byte[] getBinaryValue(byte[] result){
      return null;
    }
    
    /** 
     *  Returns always <code>null</code> for GeoCoordinate fields 
     *  Use getGeoCoordinate to retrieve results instead. 
     */
    @Override
    public Reader readerValue() {
        return null;
    }

    @Override
    public TokenStream tokenStreamValue() {
        return null;
    }

    @Override
    public void setBoost(float boost) {
        this.boost = boost;
    }

    @Override
    public float getBoost() {
        return boost;
    }

    @Override
    public String name() {
        return fieldName;
    }

    @Override
    public boolean isStored() {
        return false;
    }

    @Override
    public boolean isIndexed() {
        return true;
    }

    @Override
    public boolean isTokenized() {
        return false;
    }

    @Override
    public boolean isTermVectorStored() {
        return false;
    }

    @Override
    public boolean isStoreOffsetWithTermVector() {
        return false;
    }

    @Override
    public boolean isStorePositionWithTermVector() {
        return false;
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public boolean getOmitNorms() {
        return true;
    }

    @Override
    public void setOmitNorms(boolean omitNorms) {
        if (omitNorms != getOmitNorms()) {
            throw new IllegalArgumentException("GeoCoordinate fields only support " + getOmitNorms()
                    + " for omitNorms");
        }
    }

    @Override
    public boolean isLazy() {
        return false;
    }

    @Override
    public int getBinaryOffset() {
        return 0;
    }

    @Override
    public int getBinaryLength() {
        return 0;
    }

    @Override
    /** 
     *  Returns always <code>null</code> for GeoCoordinate fields 
     *  Use getGeoCoordinate to retrieve results instead. 
     */
    public byte[] getBinaryValue() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndexOptions getIndexOptions() {
        return IndexOptions.DOCS_ONLY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIndexOptions(IndexOptions indexOptions) {
        if (indexOptions != getIndexOptions()) {
            throw new IllegalArgumentException("GeoCoordinate fields only support " + getIndexOptions()
                    + " for indexOptions");

        }
    }

}
