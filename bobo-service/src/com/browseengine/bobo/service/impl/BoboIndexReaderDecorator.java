package com.browseengine.bobo.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import proj.zoie.api.ZoieIndexReader;
import proj.zoie.api.indexing.IndexReaderDecorator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandlerFactory;

public class BoboIndexReaderDecorator implements IndexReaderDecorator<BoboIndexReader> {
	private final List<FacetHandlerFactory<?>> _facetHandlerFactories;
	private static final Logger log = Logger.getLogger(BoboIndexReaderDecorator.class);
	
	private final ClassLoader _classLoader;
	public BoboIndexReaderDecorator(List<FacetHandlerFactory<?>> facetHandlerFactories)
	{
	  _facetHandlerFactories = facetHandlerFactories;
		_classLoader = Thread.currentThread().getContextClassLoader();
	}
	
	public BoboIndexReaderDecorator()
	{
		this(null);
	}
	
	public BoboIndexReader decorate(ZoieIndexReader<BoboIndexReader> zoieReader) throws IOException {
	    if (zoieReader != null)
	    {
    		Thread.currentThread().setContextClassLoader(_classLoader);
    		if (_facetHandlerFactories!=null)
    		{
    		  ArrayList<FacetHandler<?>> handerList = new ArrayList<FacetHandler<?>>(_facetHandlerFactories.size());
    	      for (FacetHandlerFactory<?> factory : _facetHandlerFactories)
    	      {
    	        handerList.add((FacetHandler<?>)factory.newInstance());
    	      }
    	      return BoboIndexReader.getInstanceAsSubReader(zoieReader,handerList);
    		}
    		else
    		{
    		  return BoboIndexReader.getInstanceAsSubReader(zoieReader);
    		}
	    }
	    else
	    {
	      return null;
	    }
	}

	public BoboIndexReader redecorate(BoboIndexReader reader, ZoieIndexReader<BoboIndexReader> newReader)
			throws IOException {
		reader.rewrap(newReader);
		return reader;
	}
}
