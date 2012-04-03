package com.browseengine.bobo.geosearch.bo;

/**
 * @author gcooney
 *
 */
public class CartesianCoordinateUUID {
    public int x;
    public int y;
    public int z;
    
    public byte[] uuid;
    
    public CartesianCoordinateUUID(int x, int y, int z, byte[] uuid) {
        this.x = x;
        this.y = y;
        this.z = z;
        
        this.uuid = uuid;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        result = prime * result + z;
        result = prime * result + uuid.hashCode();
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
        CartesianCoordinateUUID other = (CartesianCoordinateUUID) obj;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        if (z != other.z)
            return false;
        if (uuid != other.uuid)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[(x=" + x + ", y=" + y + ", z=" + z + "), uuid=" + uuid + "]";
    }
}
