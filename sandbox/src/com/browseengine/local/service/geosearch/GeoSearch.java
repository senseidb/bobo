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

package com.browseengine.local.service.geosearch;

import java.io.IOException;

import com.browseengine.local.service.LocalRequest;
import com.browseengine.local.service.LocalResource;
import com.browseengine.local.service.LocalResult;
import com.browseengine.local.service.geocode.GeoCodingException;
import com.browseengine.local.service.tiger.TigerParseException;

/**
 * @author spackle
 *
 */
public interface GeoSearch {
	LocalResult search(LocalRequest request) 
	throws IOException, GeoSearchingException, GeoCodingException, TigerParseException;
	LocalResource[] fetch(LocalResult result, int start, int range) 
	throws IOException, GeoSearchingException, GeoCodingException, TigerParseException;
	void close() throws GeoSearchingException;
}
