/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianCoordinate;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;

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
    
    Map<String, Byte> bitmasks = new HashMap<String, Byte>();
    
    @Override
    public void addFieldBitMask(String fieldName, byte bitMask) {
        bitmasks.put(fieldName, bitMask);
    }
    
    @Override
    public IFieldNameFilterConverter makeFieldNameFilterConverter() {
        MappedFieldNameFilterConverter mappedFieldNameFilterConverter = new MappedFieldNameFilterConverter();
        for (Entry<String, Byte> bitmask : bitmasks.entrySet()) {
            String fieldName = bitmask.getKey();
            Byte bitMask = bitmask.getValue();
            mappedFieldNameFilterConverter.addFieldBitMask(fieldName, bitMask);
        }
        return mappedFieldNameFilterConverter;
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
    public GeoRecord toGeoRecord(IFieldNameFilterConverter fieldNameFilterConverter, 
            String fieldName, LatitudeLongitudeDocId longitudeLatitudeDocId) {
        byte filterByte = fieldNameFilterConverter == null ? 
                GeoRecord.DEFAULT_FILTER_BYTE : 
                fieldNameFilterConverter.getFilterValue(new String[] {fieldName});
        
        return toGeoRecord(filterByte, longitudeLatitudeDocId);
    }

    @Override
    public GeoRecord toGeoRecord(byte filterByte, LatitudeLongitudeDocId latitudeLongitudeDocId) {
        double lng, lat;
        double lngDivisor, latDivisor;
        long lShift;
        int iShift;
        int docPlace;

        lng = latitudeLongitudeDocId.longitude + PIRADIANS; // make (-180, 180]
                                                            // go to (0, 360]
        lat = latitudeLongitudeDocId.latitude + (PIRADIANS / 2.0); // make [-90,
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
            if ((latitudeLongitudeDocId.docid & (ONE_AS_INT << docPlace)) == (ONE_AS_INT << docPlace)) {
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
    
    //This constant is calculated as the 1 - e^2 where 
    //e^2 = (a^2 - b^2) / a^2 with a = major-axis of the earth and
    //b = minor axis of the earth.  Calculated with values from WGS84
    //Geodetic datum.
    static final double ONE_MINUS_ECCENTRICITY_OF_EARTH = 0.99330562;
    static final double EARTH_RADIUS_METERS = 6378137.0;
    static final int EARTH_RADIUS_INTEGER_UNITS = 2140000000;
    
    /**
     * Returns the cartesian coordinates for a given latitude and longitude.  This method
     * scales the results so that the 
     */
    @Override
    public CartesianCoordinate toCartesianCoordinate(double latitude, double longitude) {
        double latRadians = degreesToRadians(latitude);
        double longRadians =  degreesToRadians(longitude);
        int x = getXFromRadians(latRadians, longRadians);
        int y = getYFromRadians(latRadians, longRadians);
        int z = getZFromRadians(latRadians);
        
        return new CartesianCoordinate(x, y, z);
    }

    private double degreesToRadians(double degrees)
    {
      return (degrees * (Math.PI / 180));
    }
    
    private int getXFromRadians(double latRadians, double longRadians)
    {
      return (int) (EARTH_RADIUS_INTEGER_UNITS * Math.cos(latRadians) * Math.cos(longRadians));
    }

    private int getYFromRadians(double latRadians, double longRadians)
    {
      return (int) (EARTH_RADIUS_INTEGER_UNITS * Math.cos(latRadians) * Math.sin(longRadians));
    }

    private static int getZFromRadians(double latRadians)
    {
      return (int) (EARTH_RADIUS_INTEGER_UNITS * ONE_MINUS_ECCENTRICITY_OF_EARTH * Math.sin(latRadians));
    }

    @Override
    public IDGeoRecord toIDGeoRecord(CartesianCoordinate coordinate, byte[] id) {
        int x = coordinate.x;
        int y = coordinate.y;
        int z = coordinate.z;
        
        long highOrderBits = 0;
        int highOrderPosition = LONGLENGTH - 2;
        
        //first divide on the sign bit
        if (x < 0) {
            x = x + Integer.MIN_VALUE;
        } else {
            highOrderBits += (ONE_AS_LONG << highOrderPosition);
        }
        highOrderPosition--;
        
        if (y < 0) {
            y = y + Integer.MIN_VALUE;
        } else {
            highOrderBits += (ONE_AS_LONG << highOrderPosition);
        }
        highOrderPosition--;
        
        if (z < 0) {
            z = z + Integer.MIN_VALUE;
        } else {
            highOrderBits += (ONE_AS_LONG << highOrderPosition);
        }
        highOrderPosition--;

        //now interlace the rest
        int xPos = INTLENGTH - 2;
        int yPos = INTLENGTH - 2;
        int zPos = INTLENGTH - 2;
        
        highOrderBits = interlaceToLong(x, INTLENGTH - 2, highOrderBits, highOrderPosition, 3);
        xPos -= (highOrderPosition + 3) / 3;
        highOrderBits = interlaceToLong(y, INTLENGTH - 2, highOrderBits, highOrderPosition--, 3);
        yPos -= (highOrderPosition + 3) / 3;
        highOrderBits = interlaceToLong(z, INTLENGTH - 2, highOrderBits, highOrderPosition--, 3);
        zPos -= (highOrderPosition + 3) / 3;
        
        int lowOrderBits = 0;
        int lowOrderPosition = INTLENGTH - 2;
        lowOrderBits = interlaceToInteger(x, xPos, lowOrderBits, lowOrderPosition, 3);
        lowOrderBits = interlaceToInteger(y, yPos, lowOrderBits, lowOrderPosition--, 3);
        lowOrderBits = interlaceToInteger(z, zPos, lowOrderBits, lowOrderPosition--, 3);

        return new IDGeoRecord(highOrderBits, lowOrderBits, id);
    }
    
    private long interlaceToLong(int inputValue, int inputBitPosition, long longValue, int longBitPosition, 
            int countOfValuesToInterlace) {
        while (longBitPosition > -1 && inputBitPosition > -1) {
            longValue += inputValue & (ONE_AS_INT << inputBitPosition); 
            longBitPosition -= countOfValuesToInterlace;
            inputBitPosition--;
        }
        
        return longValue;
    }
    
    private int interlaceToInteger(int inputValue, int inputBitPosition, 
            int integerValue, int integerBitPosition, int countOfValuesToInterlace) {
        while (integerBitPosition > -1 && inputBitPosition > -1) {
            integerValue += inputValue & (ONE_AS_INT << inputBitPosition); 
            integerBitPosition -= countOfValuesToInterlace;
            inputBitPosition--;
        }
        
        return integerValue;
    }
}
