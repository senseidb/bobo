package com.browseengine.bobo.facets;

import javax.management.DynamicMBean;

public abstract class AbstractRuntimeFacetHandlerFactory<P extends FacetHandlerInitializerParam, F extends RuntimeFacetHandler<?>> implements
		RuntimeFacetHandlerFactory<P, F> {

	@Override
	public DynamicMBean getMBean(){
		return null;
	}
}
