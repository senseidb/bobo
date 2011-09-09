/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;

/**
 * This is the GeoSearch POJO-in, POJO-out converter that handles bit
 * interlacing and inverse bit interlacing.
 * 
 * @author Shane Detsch
 * @author Ken McCracken
 * 
 */
@Component
public class GeoConverter implements IGeoConverter {
    private static final double PIRADIANS = 180.0;
    private static final int LONGLENGTH = 64;
    private static final int INTLENGTH = 32;
    private static final long ONE_AS_LONG = 1L;
    private static final int ONE_AS_INT = 1;

    private IFieldNameFilterConverter fieldNameFilterConverter = new MappedFieldNameFilterConverter();
    
    @Override
    @Resource(type = IFieldNameFilterConverter.class)
    public void setFieldNameFilterConverter(IFieldNameFilterConverter fieldNameFilterConverter) {
        this.fieldNameFilterConverter = fieldNameFilterConverter;
    }
    
    @Override
    public IFieldNameFilterConverter getFieldNameFilterConverter() {
        return fieldNameFilterConverter;
    }
    
    @Override
    public LatitudeLongitudeDocId toLongitudeLatitudeDocId(GeoRecord geoRecord) {

        long lShift;
        double lngDivisor, latDivisor;
        int iShift;
        int docPlace;
        long hob;
        int lob;

        lShift = LONGLENGTH - 1;
        iShift = INTLENGTH - 1;
        docPlace = INTLENGTH - 1;
        lngDivisor = PIRADIANS;
        latDivisor = (PIRADIANS / 2.0);

        lob = geoRecord.lowOrder << 1;
        hob = geoRecord.highOrder;

        int docid = 0;
        double lng = 0.0;
        double lat = 0.0;

        while (iShift > -1) {
            if (lShift > -1) {
                if ((hob & (ONE_AS_LONG << lShift)) == (ONE_AS_LONG << lShift)) {
                    docid += (ONE_AS_INT << docPlace);
                }
                lShift--;
                docPlace--;
            } else {
                if ((lob & (ONE_AS_INT << iShift)) == (ONE_AS_INT << iShift)) {
                    docid += (ONE_AS_INT << docPlace);
                }
                iShift--;
                docPlace--;
            }
            if (lShift > -1) {
                if ((hob & (ONE_AS_LONG << lShift)) == (ONE_AS_LONG << lShift)) {
                    lng += lngDivisor;
                }
                lShift--;
                lngDivisor /= 2;
            } else {
                if ((lob & (ONE_AS_INT << iShift)) == (ONE_AS_INT << iShift)) {
                    lng += lngDivisor;
                }
                iShift--;
                lngDivisor /= 2;
            }
            if (lShift > -1) {
                if ((hob & (ONE_AS_LONG << lShift)) == (ONE_AS_LONG << lShift)) {
                    lat += latDivisor;
                }
                lShift--;
                latDivisor /= 2;
            } else {
                if ((lob & (ONE_AS_INT << iShift)) == (ONE_AS_INT << iShift)) {
                    lat += latDivisor;
                }
                iShift--;
                latDivisor /= 2;
            }

        }
        lng -= PIRADIANS; // make (0, 360] go to (-180, 180]
        lat -= (PIRADIANS / 2.0); // make [0, 180] go to [-90,
                                  // 90]

        return new LatitudeLongitudeDocId(lat, lng, docid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GeoRecord toGeoRecord(String fieldName, LatitudeLongitudeDocId longitudeLatitudeDocId) {
        byte filterByte = fieldNameFilterConverter == null ? 
                GeoRecord.DEFAULT_FILTER_BYTE : 
                fieldNameFilterConverter.getFilterValue(new String[] {fieldName});
        
        return toGeoRecord(filterByte, longitudeLatitudeDocId);
    }

    @Override
    public GeoRecord toGeoRecord(byte filterByte, LatitudeLongitudeDocId longitudeLatitudeDocId) {
        double lng, lat;
        double lngDivisor, latDivisor;
        long lShift;
        int iShift;
        int docPlace;

        lng = longitudeLatitudeDocId.longitude + PIRADIANS; // make (-180, 180]
                                                            // go to (0, 360]
        lat = longitudeLatitudeDocId.latitude + (PIRADIANS / 2.0); // make [-90,
                                                                   // 90] go to
        // [0, 180]
        lngDivisor = PIRADIANS;
        latDivisor = (PIRADIANS / 2.0);
        lShift = LONGLENGTH - 1;
        iShift = INTLENGTH - 1;

        long hob = 0;
        int lob = 0;
        docPlace = INTLENGTH - 1;

        while (iShift > -1) {
            if ((longitudeLatitudeDocId.docid & (ONE_AS_INT << docPlace)) == (ONE_AS_INT << docPlace)) {
                if (lShift > -1) {
                    hob += (ONE_AS_LONG << lShift);
                    lShift--;
                } else {
                    lob += (ONE_AS_INT << iShift);
                    iShift--;
                }
                docPlace--;
            } else {
                docPlace--;
                if (lShift > -1) {
                    lShift--;
                } else {
                    iShift--;
                }
            }
            if (lng / lngDivisor >= 1 ? true : false) {
                lng -= lngDivisor;
                if (lShift > -1) {
                    hob += (ONE_AS_LONG << lShift);
                    lShift--;
                } else {
                    lob += (ONE_AS_INT << iShift);
                    iShift--;
                }
            } else {
                if (lShift > -1) {
                    lShift--;
                } else {
                    iShift--;
                }
            }
            lngDivisor /= 2;
            if (lat / latDivisor >= 1 ? true : false) {
                lat -= latDivisor;
                if (lShift > -1) {
                    hob += (ONE_AS_LONG << lShift);
                    lShift--;
                } else {
                    lob += (ONE_AS_INT << iShift);
                    iShift--;
                }
            } else {
                if (lShift > -1) {
                    lShift--;
                } else {
                    iShift--;
                }
            }
            latDivisor /= 2;
        }
        // >>> does not shift in the sign bit.
        lob >>>= 1;

        return new GeoRecord(hob, lob, filterByte);
    }

}
