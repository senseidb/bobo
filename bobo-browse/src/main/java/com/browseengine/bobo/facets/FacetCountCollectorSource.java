package com.browseengine.bobo.facets;

import com.browseengine.bobo.api.BoboIndexReader;

public abstract class FacetCountCollectorSource {
	public abstract FacetCountCollector getFacetCountCollector(BoboIndexReader reader,int docBase);
}
