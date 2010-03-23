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

package com.browseengine.bobo.service;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.impl.BrowseServiceImpl;
import com.browseengine.bobo.impl.DefaultBrowseServiceImpl;

public class BrowseServiceFactory {
	
	private static Logger logger=Logger.getLogger(BrowseServiceFactory.class);
	
	public static BrowseService createBrowseService(File idxDir)
													throws BrowseException {
		if (idxDir==null) throw new IllegalArgumentException("Null index dir specified");
			return new BrowseServiceImpl(idxDir);
	}
	
	public static BrowseService createBrowseService(BoboIndexReader bReader){
		return new DefaultBrowseServiceImpl(bReader);
	}
	
	public static BoboIndexReader getBoboIndexReader(Directory idxDir) throws BrowseException{
	  try{
        if (!BoboIndexReader.indexExists(idxDir)){
            throw new BrowseException("Index does not exist at: "+idxDir);
        }
        }
        catch(IOException ioe){
            throw new BrowseException(ioe.getMessage(),ioe);
        }
            
        IndexReader reader=null;
        try{
            reader=IndexReader.open(idxDir,true);
        }
        catch(IOException ioe){
            throw new BrowseException(ioe.getMessage(),ioe);
        }
        
        BoboIndexReader bReader=null;
        try{
            bReader=BoboIndexReader.getInstance(reader);
        }
        catch(IOException ioe){
            if (reader!=null){
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(),e);
                }
            }
            throw new BrowseException(ioe.getMessage(),ioe);
        }
        return bReader;
	}
	
	public static BrowseService createBrowseService(Directory idxDir) throws BrowseException{
	    BoboIndexReader bReader=getBoboIndexReader(idxDir);
		
		DefaultBrowseServiceImpl bs=(DefaultBrowseServiceImpl)createBrowseService(bReader);
		bs.setCloseReaderOnCleanup(true);
		return bs;
	}
}
