package com.browseengine.bobo.geosearch;


public class CartesianCoordinateDocId {
    public int x;
    public int y;
    public int z;
    
    public int docid;
    
    public CartesianCoordinateDocId(int x, int y, int z, int docid) {
        this.x = x;
        this.y = y;
        this.z = z;
        
        this.docid = docid;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        result = prime * result + z;
        result = prime * result + docid;
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
        CartesianCoordinateDocId other = (CartesianCoordinateDocId) obj;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        if (z != other.z)
            return false;
        if (docid != other.docid)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[(x=" + x + ", y=" + y + ", z=" + z + "), docid=" + docid + "]";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public CartesianCoordinateDocId clone() {
        CartesianCoordinateDocId clone = new CartesianCoordinateDocId(
                x,
                y,
                z,
                docid);
        return clone;
    }
}

