package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.FacetHandler;

public class FacetHandlerLoader {
	
	private FacetHandlerLoader()
	{
		
	}
	public static void load(Collection<FacetHandler> tobeLoaded)
	{
		load(tobeLoaded,null);
	}
	
	public static void load(Collection<FacetHandler> tobeLoaded,Map<String,FacetHandler> preloaded)
	{
		
	}
	
	private static void load(BoboIndexReader reader,Collection<FacetHandler> tobeLoaded,Map<String,FacetHandler> preloaded,Set<String> visited) throws IOException
	{
		Map<String,FacetHandler> loaded = new HashMap<String,FacetHandler>();
		if (preloaded!=null)
		{
			loaded.putAll(preloaded);
		}
		
		Iterator<FacetHandler> iter = tobeLoaded.iterator();
		
		while(iter.hasNext())
		{
			FacetHandler handler = iter.next();
			if (!loaded.containsKey(handler.getName()))
			{
			  Set<String> depends = handler.getDependsOn();
			  if (depends.size() > 0)
			  {
			  }
			  handler.load(reader);
			}
		}
	}
}
