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

import java.io.IOException;

import com.browseengine.local.service.Address;
import com.browseengine.local.service.LocalRequest;
import com.browseengine.local.service.LocalResource;
import com.browseengine.local.service.LocalResult;
import com.browseengine.local.service.LocalService;
import com.browseengine.local.service.Locatable;
import com.browseengine.local.service.geocode.GeoCode;
import com.browseengine.local.service.geocode.GeoCodeImpl;
import com.browseengine.local.service.geocode.GeoCodingException;
import com.browseengine.local.service.geosearch.GeoSearch;
import com.browseengine.local.service.geosearch.GeoSearchImpl;
import com.browseengine.local.service.geosearch.GeoSearchingException;
import com.browseengine.local.service.tiger.TigerDataException;
import com.browseengine.local.service.tiger.TigerParseException;

/**
 * @author spackle
 *
 */
public class LocalServiceImpl implements LocalService {
	private GeoCode _geocoder;
	private GeoSearch _geosearcher;
	private LocalServiceConfig _config;
	
	public LocalServiceImpl() throws LocalException {
		this(null);
	}
	
	public LocalServiceImpl(LocalServiceConfig config) throws LocalException {
		try {
			if (config == null) {
				config = new LocalServiceConfig(); // defaults
			}
			// uses a GeoCode and a GeoSearch
			_config = config;
			_geocoder = new GeoCodeImpl(_config.getGeoCodeDir());
			_geosearcher = new GeoSearchImpl(_geocoder, _config.getGeoSearchDir());		
		} catch (IOException ioe) {
			throw new LocalException(ioe.toString(), ioe);
		} catch (TigerDataException tde) {
			throw new LocalException(tde.toString(), tde);
		} catch (GeoSearchingException gse) {
			throw new LocalException(gse.toString(), gse);
		}
	}
	
	public void close() throws LocalException {
		try {
			if (_geocoder != null) {
				_geocoder.close();
			}
		} catch (GeoCodingException gce) {
			throw new LocalException(gce.toString(), gce);
		} finally {
			try {
				if (_geosearcher != null) {
					_geosearcher.close();
				}
			} catch (GeoSearchingException gse) {
				throw new LocalException(gse.toString(), gse);
			} finally {
				_geocoder = null;
				_geosearcher = null;
				_config = null;
			}
		}
	}
	
	public Locatable lookupAddress(String addressStr) throws LocalException {
		try {
			Address address =  _geocoder.parseAddress(addressStr);
			return _geocoder.lookupAddress(address);
		} catch (IOException e) {
			throw new LocalException(e.toString(), e);
		} catch (GeoCodingException e) {
			throw new LocalException(e.toString(), e);
		} catch (TigerParseException e) {
			throw new LocalException(e.toString(), e);
		}
	}
	
	public Locatable lookupAddress(Address address) throws LocalException {
		try {
			return _geocoder.lookupAddress(address);
		} catch (GeoCodingException gce) {
			throw new LocalException(gce.toString(), gce);
		} catch (IOException ioe) {
			throw new LocalException(ioe.toString(), ioe);
		}
	}

	public LocalResult search(LocalRequest request) throws LocalException {
		try {
			return _geosearcher.search(request);
		} catch (IOException ioe) {
			throw new LocalException(ioe.toString(), ioe);
		} catch (GeoSearchingException e) {
			throw new LocalException(e.toString(), e);
		} catch (GeoCodingException e) {
			throw new LocalException(e.toString(), e);
		} catch (TigerParseException e) {
			throw new LocalException(e.toString(), e);
		}
	}

	public LocalResource[] fetch(LocalResult result, int start, int range) throws LocalException {
		try {
			return _geosearcher.fetch(result, start, range);
		} catch (IOException ioe) {
			throw new LocalException(ioe.toString(), ioe);
		} catch (GeoSearchingException e) {
			throw new LocalException(e.toString(), e);
		} catch (GeoCodingException e) {
			throw new LocalException(e.toString(), e);
		} catch (TigerParseException e) {
			throw new LocalException(e.toString(), e);
		}
	}

}
