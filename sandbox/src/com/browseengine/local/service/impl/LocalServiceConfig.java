/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  spackle
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

package com.browseengine.local.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.browseengine.local.service.LocalService.LocalException;
import com.browseengine.local.service.index.IndexConstants;

/**
 * @author spackle
 *
 */
public class LocalServiceConfig {
	public static final String CONFIG_GEO_PATH = "config.geo.path";
	private File _geoSearchDir;
	private File _geoCodeDir;
	
	public LocalServiceConfig() throws LocalException {
		File geoPath = new File("/usr/local/browseengine/geo");
		init(geoPath);
	}
	
	private void init(File geoPath) throws LocalException {
		_geoSearchDir = new File(geoPath, IndexConstants.GEOSEARCH_DIR);
		_geoCodeDir = new File(geoPath, IndexConstants.GEOCODE_DIR);
		if (!_geoSearchDir.exists() || !_geoSearchDir.isDirectory() ||
				!_geoCodeDir.exists() || !_geoCodeDir.isDirectory()) {
			throw new LocalException("one of "+IndexConstants.GEOSEARCH_DIR+" at "+_geoSearchDir.getAbsolutePath()+
					", or "+IndexConstants.GEOCODE_DIR+" at "+_geoCodeDir.getAbsolutePath()+
					" is not a valid directory");
		}
	}
	
	public LocalServiceConfig(File propsFile) throws LocalException {
		InputStream fin = null;
		try {
			if (propsFile == null || !propsFile.exists() || !propsFile.isFile()) {
				throw new LocalException("props file "+(propsFile != null ? propsFile.getAbsolutePath() : null)+" does not exist");
			}
			fin=new FileInputStream(propsFile);
			Properties prop=new Properties();
			prop.load(fin);
			String geoPathStr=prop.getProperty(CONFIG_GEO_PATH);
			if (geoPathStr == null) {
				throw new LocalException("properties' "+CONFIG_GEO_PATH+" was null");
			}
			File geoPath = new File(geoPathStr);
			if (!geoPath.exists() || !geoPath.isDirectory()) {
				throw new LocalException("geo dir "+geoPath.getAbsolutePath()+" is not a valid directory");
			}
			init(geoPath);
		} catch (IOException ioe) {
			throw new LocalException(ioe.toString(), ioe);
		} finally {
			try {
				if (fin != null) {
					fin.close();
				}
			} catch (IOException ioe) {
				throw new LocalException(ioe.toString(), ioe);
			}
		}
	}
	
	public File getGeoCodeDir() {
		return _geoCodeDir;
	}
	
	public File getGeoSearchDir() {
		return _geoSearchDir;
	}
}
