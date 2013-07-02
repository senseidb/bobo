package com.browseengine.bobo.facets;

import com.browseengine.bobo.api.BoboSegmentReader;

public abstract class FacetCountCollectorSource {
	public abstract FacetCountCollector getFacetCountCollector(BoboSegmentReader reader,int docBase);
}
