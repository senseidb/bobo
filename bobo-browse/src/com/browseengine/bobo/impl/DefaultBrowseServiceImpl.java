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

import java.io.IOException;

import org.apache.log4j.Logger;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.service.BrowseService;

public class DefaultBrowseServiceImpl implements BrowseService {
	private static Logger logger=Logger.getLogger(DefaultBrowseServiceImpl.class);
	private BoboIndexReader _reader;
	private boolean _closeReader;
	
	public DefaultBrowseServiceImpl(BoboIndexReader reader) {
		super();
		_reader=reader;
		_closeReader=false;
	}
	
	public void setCloseReaderOnCleanup(boolean closeReader){
		_closeReader=closeReader;
	}

	@SuppressWarnings("serial")
	public BrowseResult browse(BrowseRequest req) throws BrowseException {
		BrowseResult result=BrowseService.EMPTY_RESULT;
		if (req.getOffset() < 0) throw new BrowseException("Invalid offset: "+req.getOffset());
		if (_reader!=null){
			BoboBrowser browser;
			try
			{
			  browser = new BoboBrowser(_reader);
			}
			catch(IOException e)
			{
			  throw new BrowseException("failed to create BoboBrowser", e);
			}
			result=browser.browse(req);			
		}				
		return result;
	}

	public void close() throws BrowseException {
		if (_closeReader){
			synchronized(this){
				if (_reader!=null){
					try{
						_reader.close();
						_reader=null;
					}
					catch(IOException ioe){
						throw new BrowseException(ioe.getMessage(),ioe);
					}
				}
			}
		}
	}
}
