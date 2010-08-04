/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  John Wang
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * send mail to owner@browseengine.com.
 */

package com.browseengine.bobo.impl;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.service.BrowseService;
import com.browseengine.bobo.service.BrowseServiceFactory;

public class BrowseServiceImpl implements BrowseService {
	private static final Logger logger = Logger.getLogger(BrowseServiceImpl.class);
	private final File _idxDir;
	private BoboIndexReader _reader;
		
	
	public BrowseServiceImpl(File idxDir) {
		super();
		_idxDir=idxDir;
		try
		{
			_reader=newIndexReader();
		}
		catch(IOException e)
		{
			logger.error(e.getMessage(),e);
		}
	}
	
	public BrowseServiceImpl(){
		this(new File(System.getProperty("index.directory")));
	}
	
	private  BoboIndexReader newIndexReader() throws IOException {
        Directory idxDir=FSDirectory.open(_idxDir);
        return newIndexReader(idxDir);
	}
	
	public static BoboIndexReader newIndexReader(Directory idxDir) throws IOException{
        if (!IndexReader.indexExists(idxDir)) {
                return null;
        }
        
        long start=System.currentTimeMillis();
        
        IndexReader ir = IndexReader.open(idxDir,true);
        BoboIndexReader reader;
        
        try {
                reader = BoboIndexReader.getInstance(ir);
        } catch (IOException ioe) {
                try { ir.close(); } catch (IOException ioe2) {}
                throw ioe;
        }

        long end=System.currentTimeMillis();
        
        if (logger.isDebugEnabled()){
                logger.debug("New index loading took: "+(end-start));
        }
        
        return reader;
	}



	public synchronized void close() throws BrowseException{
		try {
		    if (_reader!=null)
		    {
			  _reader.close();
			}
		} catch (IOException e) {
			throw new BrowseException(e.getMessage(),e);
		}
	}
	
	public BrowseResult browse(BrowseRequest req) throws BrowseException{
		return BrowseServiceFactory.createBrowseService(_reader).browse(req);
	}
}
