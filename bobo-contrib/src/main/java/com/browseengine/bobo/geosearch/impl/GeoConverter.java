/**
 * 
 */
package com.browseengine.bobo.geosearch.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.browseengine.bobo.geosearch.CartesianCoordinateDocId;
import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.bo.CartesianCoordinateUUID;
import com.browseengine.bobo.geosearch.bo.CartesianGeoRecord;
import com.browseengine.bobo.geosearch.bo.GeoRecord;
import com.browseengine.bobo.geosearch.bo.LatitudeLongitudeDocId;
import com.browseengine.bobo.geosearch.score.impl.Conversions;
import com.browseengine.bobo.geosearch.solo.bo.IDGeoRecord;

/**
 * This is the GeoSearch POJO-in, POJO-out converter that handles bit
 * interlacing and inverse bit interlacing.
 * 
 * @author Shane Detsch
 * @author Ken McCracken
 * @author Geoff Cooney
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
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public CartesianGeoRecord toCartesianGeoRecord(IFieldNameFilterConverter fieldNameFilterConverter, 
            String fieldName, LatitudeLongitudeDocId longitudeLatitudeDocId) {
        byte filterByte = fieldNameFilterConverter == null ? 
                GeoRecord.DEFAULT_FILTER_BYTE : 
                fieldNameFilterConverter.getFilterValue(new String[] {fieldName});
        
        return toCartesianGeoRecord(longitudeLatitudeDocId, filterByte);
    }
    
    //This constant is calculated as the 1 - e^2 where 
    //e^2 = (a^2 - b^2) / a^2 with a = major-axis of the earth and
    //b = minor axis of the earth.  Calculated with values from WGS84
    //Geodetic datum.
    static final double ONE_MINUS_ECCENTRICITY_OF_EARTH = 0.99330562;
    
    @Override
    public int getXFromRadians(double latRadians, double longRadians)
    {
      return (int) (Conversions.EARTH_RADIUS_INTEGER_UNITS * Math.cos(latRadians) * Math.cos(longRadians));
    }

    public int getYFromRadians(double latRadians, double longRadians)
    {
      return (int) (Conversions.EARTH_RADIUS_INTEGER_UNITS * Math.cos(latRadians) * Math.sin(longRadians));
    }

    public int getZFromRadians(double latRadians)
    {
      return (int) (Conversions.EARTH_RADIUS_INTEGER_UNITS * ONE_MINUS_ECCENTRICITY_OF_EARTH * Math.sin(latRadians));
    }

    @Override
    public IDGeoRecord toIDGeoRecord(CartesianCoordinateUUID coordinate) {
        return toIDGeoRecord(coordinate.x, coordinate.y, coordinate.z, coordinate.uuid);
    }
    
    @Override
    public CartesianCoordinateUUID toCartesianCoordinate(double latitude, double longitude, byte[] uuid) {
        double latRadians = Conversions.d2r(latitude);
        double longRadians =  Conversions.d2r(longitude);
        int x = getXFromRadians(latRadians, longRadians);
        int y = getYFromRadians(latRadians, longRadians);
        int z = getZFromRadians(latRadians);
        
        return new CartesianCoordinateUUID(x, y, z, uuid);
    }
    
    @Override
    public IDGeoRecord toIDGeoRecord(double latitude, double longitude, byte[] uuid) {
        double latRadians = Conversions.d2r(latitude);
        double longRadians =  Conversions.d2r(longitude);
        int x = getXFromRadians(latRadians, longRadians);
        int y = getYFromRadians(latRadians, longRadians);
        int z = getZFromRadians(latRadians);
        
        return toIDGeoRecord(x, y, z, uuid);
    }
    
    protected IDGeoRecord toIDGeoRecord(int x, int y, int z, byte[] uuid) { 
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
        xPos -= (highOrderPosition / 3) + 1;
        highOrderBits = interlaceToLong(y, INTLENGTH - 2, highOrderBits, --highOrderPosition, 3);
        yPos -= (highOrderPosition / 3) + 1;
        highOrderBits = interlaceToLong(z, INTLENGTH - 2, highOrderBits, --highOrderPosition, 3);
        zPos -= (highOrderPosition / 3) + 1;
        
        int lowOrderBits = 0;
        int lowOrderPosition = INTLENGTH - 2;
        lowOrderBits = interlaceToInteger(x, xPos, lowOrderBits, lowOrderPosition, 3);
        lowOrderBits = interlaceToInteger(y, yPos, lowOrderBits, --lowOrderPosition, 3);
        lowOrderBits = interlaceToInteger(z, zPos, lowOrderBits, --lowOrderPosition, 3);

        return new IDGeoRecord(highOrderBits, lowOrderBits, uuid);
    }
    
    private long interlaceToLong(int inputValue, int inputBitPosition, long longValue, int longBitPosition, 
            int interlaceInterval) {
        while (longBitPosition > -1 && inputBitPosition > -1) {
            if ((inputValue & (ONE_AS_INT << inputBitPosition)) != 0) {
                longValue += ONE_AS_LONG << longBitPosition;
            }
            longBitPosition -= interlaceInterval;
            inputBitPosition--;
        }
        
        return longValue;
    }
    
    private int interlaceToInteger(int inputValue, int inputBitPosition, 
            int integerValue, int integerBitPosition, int interlaceInterval) {
        while (integerBitPosition > -1 && inputBitPosition > -1) {
            if ((inputValue & (ONE_AS_INT << inputBitPosition)) != 0) {
                integerValue += ONE_AS_INT << integerBitPosition;
            }
            integerBitPosition -= interlaceInterval;
            inputBitPosition--;
        }
        
        return integerValue;
    }   

    @Override
    public CartesianCoordinateUUID toCartesianCoordinate(IDGeoRecord geoRecord) {
        int x = 0;
        int y = 0;
        int z = 0;
        int dimensions = 3;
        
        int highOrderPosition = LONGLENGTH - 2 - dimensions;
        
        //now interlace the rest
        int xPos = INTLENGTH - 2;
        int yPos = INTLENGTH - 2;
        int zPos = INTLENGTH - 2;
        
        //uninterlace high order bits
        x = unInterlaceFromLong(x, xPos, geoRecord.highOrder, highOrderPosition, dimensions);
        xPos -= (highOrderPosition / dimensions) + 1;
        y = unInterlaceFromLong(y, yPos, geoRecord.highOrder, --highOrderPosition, dimensions);
        yPos -= (highOrderPosition / dimensions) + 1;
        z = unInterlaceFromLong(z, zPos, geoRecord.highOrder, --highOrderPosition, dimensions);
        zPos -= (highOrderPosition / dimensions) + 1;
        
        //uninterlace low order bits
        int lowOrderPosition = INTLENGTH - 2;
        x = unInterlaceFromInt(x, xPos, geoRecord.lowOrder, lowOrderPosition, dimensions);
        y = unInterlaceFromInt(y, yPos, geoRecord.lowOrder, --lowOrderPosition, dimensions);
        z = unInterlaceFromInt(z, zPos, geoRecord.lowOrder, --lowOrderPosition, dimensions);
        
        highOrderPosition = LONGLENGTH - 2;
        //uninterlace sign bit
        if ((geoRecord.highOrder & (ONE_AS_LONG << highOrderPosition)) == 0) {
            x = x - Integer.MIN_VALUE;
        } 
        highOrderPosition--;
        
        if ((geoRecord.highOrder & (ONE_AS_LONG << highOrderPosition)) == 0) {
            y = y - Integer.MIN_VALUE;
        } 
        highOrderPosition--;
        
        if ((geoRecord.highOrder & (ONE_AS_LONG << highOrderPosition)) == 0) {
            z = z - Integer.MIN_VALUE;
        } 
        highOrderPosition--;

        return new CartesianCoordinateUUID(x, y, z, geoRecord.id);
    }

    private int unInterlaceFromLong(int outputValue, int outputBitPos, long longValue, int longBitPosition, 
            int interlaceInterval) {
        while (longBitPosition > -1 && outputBitPos > -1) {
            if ((longValue & (ONE_AS_LONG << longBitPosition)) != 0) {
                outputValue += ONE_AS_INT << outputBitPos; 
            }
            longBitPosition -= interlaceInterval;
            outputBitPos--;
        }
        
        return outputValue;
    }
    
    private int unInterlaceFromInt(int outputValue, int outputBitPos, int intValue, int intBitPosition, 
            int interlaceInterval) {
        while (intBitPosition > -1 && outputBitPos > -1) {
            if ((intValue & (ONE_AS_INT << intBitPosition)) != 0) {
                outputValue += ONE_AS_INT << outputBitPos; 
            } 
            intBitPosition -= interlaceInterval;
            outputBitPos--;
        }
        
        return outputValue;
    }
    
    @Override
    public CartesianGeoRecord toCartesianGeoRecord(CartesianCoordinateDocId coord) {
        return toCartesianGeoRecord(coord.x, coord.y, coord.z, coord.docid, (byte)0);
    }

    @Override
    public CartesianGeoRecord toCartesianGeoRecord(LatitudeLongitudeDocId latLongDocID, byte filterByte) {
        double latRadians = Conversions.d2r(latLongDocID.latitude);
        double longRadians =  Conversions.d2r(latLongDocID.longitude);
        int x = getXFromRadians(latRadians, longRadians);
        int y = getYFromRadians(latRadians, longRadians);
        int z = getZFromRadians(latRadians);
        
        return toCartesianGeoRecord(x, y, z, latLongDocID.docid, filterByte);
    }
    
    /**
     * Bitinterlaces docid, x, y, z into long high and low order bits with the lose of a sign 
     * bit on docid which does not exist anyways and the least significant bit on z which is 
     * sub meter precision. 
     * @param x
     * @param y
     * @param z
     * @param docid
     * @param filterByte
     * @return
     */
    CartesianGeoRecord toCartesianGeoRecord(int x, int y, int z, int docid, byte filterByte) {
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
        int docIdPos = INTLENGTH - 2;
        
        highOrderBits = interlaceToLong(docid, INTLENGTH - 2, highOrderBits, highOrderPosition, 4);
        docIdPos -= (highOrderPosition / 4) + 1;
        highOrderBits = interlaceToLong(x, INTLENGTH - 2, highOrderBits, --highOrderPosition, 4);
        xPos -= (highOrderPosition / 4) + 1;
        highOrderBits = interlaceToLong(y, INTLENGTH - 2, highOrderBits, --highOrderPosition, 4);
        yPos -= (highOrderPosition / 4) + 1;
        highOrderBits = interlaceToLong(z, INTLENGTH - 2, highOrderBits, --highOrderPosition, 4);
        zPos -= (highOrderPosition / 4) + 1;
        
        
        long lowOrderBits = 0;
        int lowOrderPosition = LONGLENGTH - 2;
        lowOrderBits = interlaceToLong(docid, docIdPos, lowOrderBits, lowOrderPosition, 4);
        lowOrderBits = interlaceToLong(x, xPos, lowOrderBits, --lowOrderPosition, 4);
        lowOrderBits = interlaceToLong(y, yPos, lowOrderBits, --lowOrderPosition, 4);
        lowOrderBits = interlaceToLong(z, zPos, lowOrderBits, --lowOrderPosition, 4);
        
        return new CartesianGeoRecord(highOrderBits, lowOrderBits, filterByte);
    }
    
    @Override
    public CartesianCoordinateDocId toCartesianCoordinateDocId(CartesianGeoRecord geoRecord) {
        int x = 0;
        int y = 0;
        int z = 0;
        int docid = 0;
        int dimensions = 4;
        
        int highOrderPosition = LONGLENGTH - 2 - dimensions + 1;
        
        //now interlace the rest
        int xPos = INTLENGTH - 2;
        int yPos = INTLENGTH - 2;
        int zPos = INTLENGTH - 2;
        int docidPos = INTLENGTH - 2;
        
        //uninterlace high order bits
        docid = unInterlaceFromLong(docid, docidPos, geoRecord.highOrder, highOrderPosition, dimensions);
        docidPos -= (highOrderPosition / dimensions) + 1;
        x = unInterlaceFromLong(x, xPos, geoRecord.highOrder, --highOrderPosition, dimensions);
        xPos -= (highOrderPosition / dimensions) + 1;
        y = unInterlaceFromLong(y, yPos, geoRecord.highOrder, --highOrderPosition, dimensions);
        yPos -= (highOrderPosition / dimensions) + 1;
        z = unInterlaceFromLong(z, zPos, geoRecord.highOrder, --highOrderPosition, dimensions);
        zPos -= (highOrderPosition / dimensions) + 1;
        
        //uninterlace low order bits
        int lowOrderPosition = LONGLENGTH - 2;
        docid = unInterlaceFromLong(docid, docidPos, geoRecord.lowOrder, lowOrderPosition, dimensions);
        x = unInterlaceFromLong(x, xPos, geoRecord.lowOrder, --lowOrderPosition, dimensions);
        y = unInterlaceFromLong(y, yPos, geoRecord.lowOrder, --lowOrderPosition, dimensions);
        z = unInterlaceFromLong(z, zPos, geoRecord.lowOrder, --lowOrderPosition, dimensions);
        
        
        highOrderPosition = LONGLENGTH - 2;
        //uninterlace sign bit
        if ((geoRecord.highOrder & (ONE_AS_LONG << highOrderPosition)) == 0) {
            x = x - Integer.MIN_VALUE;
        } 
        highOrderPosition--;
        
        if ((geoRecord.highOrder & (ONE_AS_LONG << highOrderPosition)) == 0) {
            y = y - Integer.MIN_VALUE;
        } 
        highOrderPosition--;
        
        if ((geoRecord.highOrder & (ONE_AS_LONG << highOrderPosition)) == 0) {
            z = z - Integer.MIN_VALUE;
        } 
        highOrderPosition--;

        return new CartesianCoordinateDocId(x, y, z, docid);
    }

    @Override
    public CartesianGeoRecord toCartesianGeoRecord(CartesianCoordinateDocId coord, byte filterByte) {
        return toCartesianGeoRecord(coord.x, coord.y, coord.z, coord.docid, filterByte);
    }
}
