package com.browseengine.bobo.geosearch.merge;

import java.io.IOException;


import com.browseengine.bobo.geosearch.bo.GeoSearchConfig;

public interface IGeoMerger {
    void merge(IGeoMergeInfo geoMergeInfo, GeoSearchConfig config) throws IOException;
}
