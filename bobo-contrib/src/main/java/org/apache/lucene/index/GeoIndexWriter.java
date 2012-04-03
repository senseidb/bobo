package org.apache.lucene.index;

import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;
import com.browseengine.bobo.geosearch.index.impl.DeletePairedExtensionDirectory;
import com.browseengine.bobo.geosearch.merge.IGeoMergeInfo;
import com.browseengine.bobo.geosearch.merge.IGeoMerger;


/**
 * Extends Lucene's IndexWriter to provide functionality for indexing, merging, and deleting
 * Coordinate based fields.
 * 
 * @author Geoff Cooney
 * @see IndexWriter
 */
public class GeoIndexWriter extends IndexWriter {

    GeoSearchConfig geoConfig;
    
    public GeoIndexWriter(Directory d, IndexWriterConfig indexWriterConfig, GeoSearchConfig geoConfig) throws CorruptIndexException, LockObtainFailedException,
            IOException {
        super(buildGeoDirectory(d, geoConfig), setIndexingChain(indexWriterConfig, geoConfig));
        
        this.geoConfig = geoConfig;
    }

    public static Directory buildGeoDirectory(Directory dir, GeoSearchConfig geoConfig) {
        DeletePairedExtensionDirectory pairedDirectory = new DeletePairedExtensionDirectory(dir);
        for (String pairedExtension: geoConfig.getPairedExtensionsForDelete()) {
            pairedDirectory.addExtensionPairing(pairedExtension, geoConfig.getGeoFileExtension());
        }
        
        return pairedDirectory;
    }
    
    public static IndexWriterConfig setIndexingChain(IndexWriterConfig indexWriterConfig, GeoSearchConfig geoConfig) {
        return indexWriterConfig.setIndexingChain(new GeoIndexingChain(geoConfig, indexWriterConfig.getIndexingChain()));
    }
    
    @Override
    public void beforeMergeAfterSetup(MergePolicy.OneMerge merge) throws IOException {
        merge.checkAborted(getDirectory());
        
        IGeoMergeInfo geoMergeInfo = new GeoMergeInfo(merge, getDirectory());

        IGeoMerger geoMerger = geoConfig.getGeoMerger();
        
        geoMerger.merge(geoMergeInfo, geoConfig);
    }
    
}
