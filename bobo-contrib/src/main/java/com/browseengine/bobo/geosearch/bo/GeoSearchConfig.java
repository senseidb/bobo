package com.browseengine.bobo.geosearch.bo;

import org.apache.lucene.index.IndexFileNames;
import org.springframework.stereotype.Component;

import com.browseengine.bobo.geosearch.IGeoConverter;
import com.browseengine.bobo.geosearch.IGeoUtil;
import com.browseengine.bobo.geosearch.impl.GeoConverter;
import com.browseengine.bobo.geosearch.impl.GeoUtil;
import com.browseengine.bobo.geosearch.merge.IGeoMerger;
import com.browseengine.bobo.geosearch.merge.impl.BufferedGeoMerger;

/**
 * Class to hold configuration parameters for GeoSearch 
 * 
 * @author Geoff Cooney
 *
 */
@Component
public class GeoSearchConfig {

    public static final String DEFAULT_GEO_FILE_EXTENSION = "geo";
    private IGeoConverter geoConverter = new GeoConverter();
    private IGeoUtil geoUtil = new GeoUtil();
    private IGeoMerger geoMerger = new BufferedGeoMerger();
    private String geoFileExtension = DEFAULT_GEO_FILE_EXTENSION;
    
    private String[] pairedExtensionsForDelete 
        = new String[] {IndexFileNames.COMPOUND_FILE_EXTENSION};
    
    public static final int DEFAULT_ID_BYTE_COUNT = 16;
    private int bytesForId = DEFAULT_ID_BYTE_COUNT;
    private int maxIndexSize = Integer.MAX_VALUE;
    
    /**
     * Sets the extension for geo indices.
     * WARNING:  This should never be changed when reading an existing index as doing so may result in
     * geo search being unable to find indices for existing segments
     * @param fileExtension
     */
    public void setGeoFileExtension(String fileExtension) {
        this.geoFileExtension = fileExtension;
    }
    
    public String getGeoFileExtension() {
        return geoFileExtension;
    }
    
    public IGeoConverter getGeoConverter() {
        return geoConverter;
    }
    
    public void setGeoConverter(IGeoConverter geoConverter) {
        this.geoConverter = geoConverter;
    }
    
    public void setGeoUtil(IGeoUtil geoUtil) {
        this.geoUtil = geoUtil;
    }
    
    public void addFieldBitMask(String fieldName, byte bitMask) {
        this.geoConverter.addFieldBitMask(fieldName, bitMask);
    }
    
    public IGeoUtil getGeoUtil() {
        return this.geoUtil;
    }

    
    public IGeoMerger getGeoMerger() {
        return geoMerger;
    }

    public void setGeoMerger(IGeoMerger geoMerger) {
        this.geoMerger = geoMerger;
    }

    public String getGeoFileName(String name) {
        return name + "." + getGeoFileExtension();
    }
    
    public int getBufferSizePerGeoSegmentReader() {
        return 16*1024;
    }

    /**
     * The extension that we should pair off of by delete.  When any extension in this list
     * is deleted by Lucene for any reason, geo search will also delete the corresponding
     * geo file.  By default, this is set to CFS and FNM.
     */
    public void setPairedExtensionPairsForDelete(String... pairedExtensionsForDelete) {
        this.pairedExtensionsForDelete = pairedExtensionsForDelete;
    }

    public String[] getPairedExtensionsForDelete() {
        return pairedExtensionsForDelete;
    }
    
    /**
     * The number of bytes reserved for the id field.  This is only used
     * by GeoOnlySearch
     * @param bytesForId
     */
    public void setBytesForId(int bytesForId) {
        this.bytesForId = bytesForId;
    }
    
    public int getBytesForId() {
        return bytesForId;
    }
    
    /**
     * The maximum size the index should be allowed to grow to.  Any attempts to
     * flush an index larger than this will throw errors.  Applies only to the GeoOnlyIndex. 
     * @param maxIndexSize
     */
    public void setMaxIndexSize(int maxIndexSize) {
        this.maxIndexSize = maxIndexSize;
    }
    
    public int getMaxIndexSize() {
        return maxIndexSize;
    }
}
