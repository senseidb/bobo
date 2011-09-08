/**
 * 
 */
package com.browseengine.bobo.geosearch.bo;

/**
 * We have got to find a shorter name for this class.
 * 
 * @author Ken McCracken
 *
 */
public class GeoRecordAndLongitudeLatitudeDocId {
    public GeoRecord geoRecord;
    public LatitudeLongitudeDocId longitudeLatitudeDocId;
    
    public GeoRecordAndLongitudeLatitudeDocId(GeoRecord geoRecord,
        LatitudeLongitudeDocId longitudeLatitudeDocId) {
        this.geoRecord = geoRecord;
        this.longitudeLatitudeDocId = longitudeLatitudeDocId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "GeoRecordAndLongitudeLatitudeDocId [geoRecord=" + geoRecord + ", longitudeLatitudeDocId="
                + longitudeLatitudeDocId + "]";
    }
    
}
