/**
 * 
 */
package com.browseengine.bobo.geosearch.bo;

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;

/**
 * We have got to find a shorter name for this class.
 * 
 * @author Ken McCracken
 * @author shandets
 *
 */
public class GeRecordAndCartesianDocId {
    public IGeoRecord geoRecord;
    public CartesianCoordinateDocId cartesianCoordinateDocId;
    
    public GeRecordAndCartesianDocId(IGeoRecord geoRecord,
            CartesianCoordinateDocId cartesianCoordinateDocId) {
        this.geoRecord = geoRecord;
        this.cartesianCoordinateDocId = cartesianCoordinateDocId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "GeRecordAndCartesianDocId [geoRecord=" + geoRecord + ", cartesianCoordinateDocId="
                + cartesianCoordinateDocId + "]";
    }
    
}
