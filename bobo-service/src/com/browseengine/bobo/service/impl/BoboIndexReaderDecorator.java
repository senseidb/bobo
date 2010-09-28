package com.browseengine.bobo.service.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import proj.zoie.api.ZoieIndexReader;
import proj.zoie.api.indexing.IndexReaderDecorator;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.RuntimeFacetHandlerFactory;

public class BoboIndexReaderDecorator implements IndexReaderDecorator<BoboIndexReader> {
	private final List<FacetHandler<?>> _facetHandlers;
	private static final Logger log = Logger.getLogger(BoboIndexReaderDecorator.class);
	
    private final Collection<RuntimeFacetHandlerFactory<?,?>> _facetHandlerFactories;
	private final ClassLoader _classLoader;
	public BoboIndexReaderDecorator(List<FacetHandler<?>> facetHandlers, Collection<RuntimeFacetHandlerFactory<?,?>> facetHandlerFactories)
	{
	  _facetHandlers = facetHandlers;
	  _facetHandlerFactories = facetHandlerFactories;
		_classLoader = Thread.currentThread().getContextClassLoader();
	}
	
	public BoboIndexReaderDecorator(List<FacetHandler<?>> facetHandlers)
	{
	  this(facetHandlers,null);
	}
	
	public BoboIndexReaderDecorator()
	{
		this(null, null);
	}
	
	/* (non-Javadoc)
	 * @see proj.zoie.api.indexing.IndexReaderDecorator#decorate(proj.zoie.api.ZoieIndexReader)
	 */
	public BoboIndexReader decorate(ZoieIndexReader<BoboIndexReader> zoieReader) throws IOException {
	    if (zoieReader != null)
	    {
    		Thread.currentThread().setContextClassLoader(_classLoader);
    		if (_facetHandlers!=null)
    		{
    		  return BoboIndexReader.getInstanceAsSubReader(zoieReader, _facetHandlers, _facetHandlerFactories);
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

	/* (non-Javadoc)
	 * @see proj.zoie.api.indexing.IndexReaderDecorator#redecorate(org.apache.lucene.index.IndexReader, proj.zoie.api.ZoieIndexReader, boolean)
	 */
	public BoboIndexReader redecorate(BoboIndexReader reader, ZoieIndexReader<BoboIndexReader> newReader,boolean withDeletes)
			throws IOException {
		return reader.copy(newReader);
	}
}
