package com.browseengine.bobo.service;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;

public class BoboService{
	private static Logger logger = Logger.getLogger(BoboService.class);
	
	private final File _idxDir;
	private BoboIndexReader _boboReader;
	
	public BoboService(String path)
	{
		this(new File(path));
	}
	
	public BoboService(File idxDir)
	{
		_idxDir=idxDir;
		_boboReader = null;
	}
	
	public BrowseResult browse(BrowseRequest req)
	{
		BoboBrowser browser=null;
		try
		{
			browser = new BoboBrowser(_boboReader);
			return browser.browse(req);
		}
        catch(Exception e)
		{
			logger.error(e.getMessage(),e);
			return new BrowseResult();
		}
		finally
		{
			if (browser!=null)
			{
				try {
					browser.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}
	
	public void start() throws IOException
	{
		IndexReader reader=IndexReader.open(FSDirectory.open(_idxDir),true);
		try
		{
			_boboReader=BoboIndexReader.getInstance(reader);
		}
		catch(IOException ioe)
		{
			if (reader!=null)
			{
				reader.close();
			}
		}
	}
	
	public void shutdown()
	{
		if (_boboReader!=null)
		{
			try {
				_boboReader.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
	}
}
