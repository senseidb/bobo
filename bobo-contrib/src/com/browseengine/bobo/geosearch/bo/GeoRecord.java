/**
 * 
 */
package com.browseengine.bobo.geosearch.bo;

/**
 * Inlinable POJO representing a bit interlace record.
 * 
 * @author Shane Detsch
 * @author Ken McCracken
 *
 */
public class GeoRecord implements IGeoRecord {
    /**
     * This constant will be removed when we figure out how to make the filters real.
     * Until then, you should reference when calling this constructor, it will make 
     * it easier for us to fix all callers in the future.
     */
    public static final byte DEFAULT_FILTER_BYTE = (byte)0;
    
    public final long highOrder;
    public final int lowOrder;
    public final byte filterByte;
    
    public static final GeoRecord MIN_VALID_GEORECORD = 
        new GeoRecord(0, 0, GeoRecord.DEFAULT_FILTER_BYTE);
    
    public static final GeoRecord MAX_VALID_GEORECORD = 
        new GeoRecord(Long.MAX_VALUE, Integer.MAX_VALUE, GeoRecord.DEFAULT_FILTER_BYTE);
    
    public GeoRecord(long highOrder, int lowOrder, byte filterByte) {
        if (highOrder < 0L || lowOrder < 0) {
            throw new RuntimeException("GeoRecord(" + highOrder + ", " + lowOrder 
                    + ", " + filterByte + "): only supports positive highOrder and lowOrder");
        }
        this.highOrder = highOrder;
        this.lowOrder = lowOrder;
        this.filterByte = filterByte;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + filterByte;
        result = prime * result + (int) (highOrder ^ (highOrder >>> 32));
        result = prime * result + lowOrder;
        return result;
    }

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
        GeoRecord other = (GeoRecord) obj;
        if (filterByte != other.filterByte) {
            return false;
        }
        if (highOrder != other.highOrder) {
            return false;
        }
        if (lowOrder != other.lowOrder) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "GeoRecord [padded highOrder=" + lpad(highOrder) + ", padded lowOrder=" + 
            lpad(lowOrder) + ", filterByte=" + filterByte + "]";
    }
    
    private static final int MAX_DIGITS_INT = ndigits(Integer.MAX_VALUE);
    private static final int MAX_DIGITS_LONG = ndigits(Long.MAX_VALUE);
    
    public static String lpad(int val) {
        return lpad(MAX_DIGITS_INT, val);
    }
    
    public static String lpad(long val) {
        return lpad(MAX_DIGITS_LONG, val);
    }
    
    private static String lpad(int maxDigits, long val) {
        int ndigits = ndigits(val);
        int pad = maxDigits - ndigits;
        StringBuilder buf = new StringBuilder();
        while (pad > 0) {
            buf.append('0');
            pad--;
        }
        buf.append(val);
        return buf.toString();
    }
    
    private static int ndigits(long val) {
        val = Long.highestOneBit(val);
        int i = 0;
        while (val > 0) {
            i++;
            val /= 10;
        }
        return i;
    }
    
    
}
