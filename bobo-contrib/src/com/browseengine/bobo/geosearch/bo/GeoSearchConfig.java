package com.browseengine.bobo.geosearch.bo;

import javax.annotation.Resource;

import org.apache.lucene.index.IndexFileNames;
import org.springframework.stereotype.Component;

import com.browseengine.bobo.geosearch.IFieldNameFilterConverter;
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
    
    @Resource(type = IGeoConverter.class)
    public void setGeoConverter(IGeoConverter geoConverter) {
        this.geoConverter = geoConverter;
    }
    
    public void setFieldNameFilterConverter(IFieldNameFilterConverter fieldNameFilterConverter) {
        geoConverter.setFieldNameFilterConverter(fieldNameFilterConverter);
    }
    
    @Resource(type = IGeoUtil.class)
    public void setGeoUtil(IGeoUtil geoUtil) {
        this.geoUtil = geoUtil;
    }
    
    public IGeoUtil getGeoUtil() {
        return this.geoUtil;
    }

    
    public IGeoMerger getGeoMerger() {
        return geoMerger;
    }

    @Resource(type = IGeoMerger.class)
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

}
