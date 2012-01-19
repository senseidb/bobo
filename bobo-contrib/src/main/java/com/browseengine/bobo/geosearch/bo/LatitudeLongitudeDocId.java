/**
 * 
 */
package com.browseengine.bobo.geosearch.bo;

/**
 * Inlinable POJO representing a raw latitude, longitude, docid record.
 * 
 * @author Shane Detsch
 * @author Ken McCracken
 *
 */
public class LatitudeLongitudeDocId implements Cloneable {
    public double latitude;
    public double longitude;
    public int docid;
    
    public LatitudeLongitudeDocId(double latitude,
     double longitude,
     int docid)
    {
        this.  latitude = latitude;
        this. longitude = longitude;
        this. docid = docid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LatitudeLongitudeDocId other = (LatitudeLongitudeDocId) obj;
        if (docid != other.docid) {
            return false;
        }
        if (!equalsWithTolerance(longitude, other.longitude)) {
            return false;
        }
        if (!equalsWithTolerance(latitude, other.latitude)) {
            return false;
        }
        return true;
    }
    
    private boolean equalsWithTolerance(double a, double b) {
        double diff = a-b;
        if (0 == diff) {
            return true;
        }
        if (Math.abs(diff / a) < 0.00001) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "LatitudeLongitudeDocId [longitude=" + longitude + ", latitude=" + latitude + ", docid=" + docid + "]";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public LatitudeLongitudeDocId clone() {
        LatitudeLongitudeDocId clone = new LatitudeLongitudeDocId(
                latitude,
                longitude,
                docid);
        return clone;
    }
    
}
