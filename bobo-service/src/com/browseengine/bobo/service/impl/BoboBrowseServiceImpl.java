package com.browseengine.bobo.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import proj.zoie.api.IndexReaderFactory;
import proj.zoie.api.ZoieIndexReader;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.Browsable;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.MultiBoboBrowser;
import com.browseengine.bobo.service.BrowseService;
import com.browseengine.bobo.service.SerializedFacetAccessible;

public class BoboBrowseServiceImpl implements BrowseService {
    private final IndexReaderFactory<ZoieIndexReader<BoboIndexReader>> _indexReaderFactory;
    
	public BoboBrowseServiceImpl(IndexReaderFactory<ZoieIndexReader<BoboIndexReader>> indexReaderFactory)
	{
		_indexReaderFactory=indexReaderFactory;
	}
	
	private Browsable buildBrowsable() throws IOException
	{
		List<ZoieIndexReader<BoboIndexReader>> readerList = _indexReaderFactory.getIndexReaders();
		
		ArrayList<Browsable> subBrowsableList = new ArrayList<Browsable>();
		
		for (ZoieIndexReader<BoboIndexReader> reader : readerList)
		{
			List<BoboIndexReader> boboReaders = reader.getDecoratedReaders();
			for (BoboIndexReader boboReader : boboReaders){
			  subBrowsableList.add(new BoboBrowser(boboReader));
			}
		}
		
		MultiBoboBrowser multiBrowser = new MultiBoboBrowser(subBrowsableList.toArray(new Browsable[subBrowsableList.size()]));
		
		return multiBrowser;
	}
	
	public BrowseResult browse(BrowseRequest req) throws BrowseException {
		try
		{
			Browsable browsable = buildBrowsable();
			BrowseResult res = browsable.browse(req);
			
			BrowseResult serializableResult = new BrowseResult();
			serializableResult.setHits(res.getHits());
			serializableResult.setNumHits(res.getNumHits());
			serializableResult.setTotalDocs(res.getTotalDocs());
			serializableResult.setTime(res.getTime());
			
			Map<String,FacetAccessible> facetMap = res.getFacetMap();
			Set<Entry<String,FacetAccessible>> entries = facetMap.entrySet();
			for (Entry<String,FacetAccessible> entry : entries)
			{
				List<BrowseFacet> facets = entry.getValue().getFacets();
				SerializedFacetAccessible facetAccessible = new SerializedFacetAccessible(facets);
				serializableResult.addFacets(entry.getKey(), facetAccessible);
			}
			return serializableResult;
		}
		catch(IOException ioe)
		{
			throw new BrowseException(ioe.getMessage(),ioe);
		}
	}

	public void close() throws BrowseException {
		
	}
}
