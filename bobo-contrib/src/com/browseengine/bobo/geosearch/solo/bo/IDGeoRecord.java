package com.browseengine.bobo.geosearch.solo.bo;

import java.util.Arrays;

import com.browseengine.bobo.geosearch.bo.GeoRecord;

public class IDGeoRecord {
    public final long highOrder;
    public final int lowOrder;
    
    public final byte[] id;
    
    public static final GeoRecord MIN_VALID_GEORECORD = 
        new GeoRecord(0, 0, GeoRecord.DEFAULT_FILTER_BYTE);
    
    public static final GeoRecord MAX_VALID_GEORECORD = 
        new GeoRecord(Long.MAX_VALUE, Integer.MAX_VALUE, GeoRecord.DEFAULT_FILTER_BYTE);
    
    public IDGeoRecord(long highOrder, int lowOrder, byte[] id) {
        this.highOrder = highOrder;
        this.lowOrder = lowOrder;
        this.id = id;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (highOrder ^ (highOrder >>> 32));
        result = prime * result + Arrays.hashCode(id);
        result = prime * result + lowOrder;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        
        IDGeoRecord other = (IDGeoRecord) obj;
        if (highOrder != other.highOrder)
            return false;
        if (!Arrays.equals(id, other.id))
            return false;
        if (lowOrder != other.lowOrder)
            return false;
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "GeoRecord [highOrder=" + highOrder + ", lowOrder=" + 
            lowOrder + ", id=" + Arrays.toString(id) + "]";
    }
}
