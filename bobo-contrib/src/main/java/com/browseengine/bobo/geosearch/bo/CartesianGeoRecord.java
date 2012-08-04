package com.browseengine.bobo.geosearch.bo;

import com.browseengine.bobo.geosearch.GeoRecordUtil;

public class CartesianGeoRecord implements IGeoRecord {
    /**
     * This constant will be removed when we figure out how to make the filters real.
     * Until then, you should reference when calling this constructor, it will make 
     * it easier for us to fix all callers in the future.
     */
    public static final byte DEFAULT_FILTER_BYTE = (byte)0;
    
    public final long highOrder;
    public  final long lowOrder;
    public final byte filterByte;
    
    public static final CartesianGeoRecord MIN_VALID_GEORECORD = 
        new CartesianGeoRecord(0, 0, DEFAULT_FILTER_BYTE);
    
    public static final CartesianGeoRecord MAX_VALID_GEORECORD = 
        new CartesianGeoRecord(Long.MAX_VALUE, Long.MAX_VALUE, DEFAULT_FILTER_BYTE);
    
    public CartesianGeoRecord(long highOrder, long lowOrder, byte filterByte) {
        if (highOrder < 0L || lowOrder < 0) {
            throw new RuntimeException("CartesianGeoRecord(" + highOrder + ", " + lowOrder 
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
        result = prime * result + (int) (lowOrder ^ lowOrder >>> 32);
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
        CartesianGeoRecord other = (CartesianGeoRecord) obj;
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
        return "CartesianGeoRecord [padded highOrder=" + lpad(highOrder) + ", padded lowOrder=" + 
            lpad(lowOrder) + ", filterByte=" + filterByte + "]";
    }
    
    public static String lpad(long val) {
        return GeoRecordUtil.lpad(GeoRecordUtil.MAX_DIGITS_LONG, val);
    }
}
