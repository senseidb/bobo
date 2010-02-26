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

package com.browseengine.bobo.index.digest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public abstract class FileDigester extends DataDigester {
	private File _file;
	private  Charset _charset;
	private int maxDocs;
	
	public int getMaxDocs() {
		return maxDocs;
	}

	public void setMaxDocs(int maxDocs) {
		this.maxDocs = maxDocs;
	}

	public FileDigester(File file) {
		super();
		_file=file;		
	}
	
	public void setCharset(Charset charset){
		_charset=charset;
	}
	
	public File getDataFile(){
		return _file;
	}
	
	public Charset getCharset(){
		return _charset;
	}

	abstract public void digest(DataHandler handler) throws IOException;
}
