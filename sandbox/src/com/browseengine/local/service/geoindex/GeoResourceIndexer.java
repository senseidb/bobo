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
 * contact owner@browseengine.com.
 */

package com.browseengine.local.service.geoindex;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.browseengine.local.service.Address;
import com.browseengine.local.service.LocalResource;
import com.browseengine.local.service.LocatedAddress;
import com.browseengine.local.service.geocode.GeoCode;
import com.browseengine.local.service.geocode.GeoCodeImpl;
import com.browseengine.local.service.geocode.GeoCodingException;
import com.browseengine.local.service.geoindex.YahooYpParser.YahooYpRecord;
import com.browseengine.local.service.tiger.TigerDataException;
import com.browseengine.local.service.tiger.TigerParseException;

/**
 * @author spackle
 *
 */
public class GeoResourceIndexer {
	private static final Logger LOGGER = Logger.getLogger(GeoResourceIndexer.class);
	
	private GeoCode _geocoder;
	private GeoResourceWriter _writer;
	
	/**
	 * <code>geocodeDir</code> is for looking up the geocodes for addresses, 
	 * given that they are in String format.
	 * 
	 * @param geocodeDir
	 * @throws TigerDataException 
	 * @throws IOException 
	 * @throws GeoIndexingException 
	 */
	public GeoResourceIndexer(File geocodeDir, File geosearchDir) throws IOException, TigerDataException, GeoIndexingException {
		_geocoder = new GeoCodeImpl(geocodeDir);
		_writer = new GeoResourceWriter(geosearchDir);
	}
	
	public void parseYahooYpFile(File yahooYpFile) throws IOException, GeoIndexingException {
		YahooYpParser parser = null;
		try {
			int added = 0;
			parser = new YahooYpParser(yahooYpFile);
			while (parser.hasNext()) {
				YahooYpRecord record = parser.next();
				if (record != null) {
					String fulladdr = record.addrLine1+", "+record.addrLine2;
				try {
					LocatedAddress addr = geocode(fulladdr);
					LocalResource toindex = new LocalResource(record.name, "A random resource found on "+parser.getSource()+".", 
							fulladdr, record.phone, addr.getLongitudeDeg(), addr.getLatitudeDeg());
					_writer.addResource(toindex);
					added++;
				} catch (TigerParseException tpe) {
					LOGGER.warn("trouble geocoding or adding entry "+record.name+" at: "+fulladdr+": "+tpe, tpe);
				} catch (IOException ioe) {
					LOGGER.warn("trouble geocoding or adding entry "+record.name+" at: "+fulladdr+": "+ioe, ioe);
				} catch (GeoCodingException ioe) {
					LOGGER.warn("trouble geocoding or adding entry "+record.name+" at: "+fulladdr+": "+ioe, ioe);
				} catch (GeoIndexingException ioe) {
					LOGGER.warn("trouble geocoding or adding entry "+record.name+" at: "+fulladdr+": "+ioe, ioe);					
				}
				} else {
					LOGGER.warn("skipping over a null record from YahooYpParser");
				}
			}
			LOGGER.info("added "+added+" resources from "+parser.getSource()+" source file");
		} finally {
			if (parser != null) {
				parser.close();
			}
		}
	}
	
		private LocatedAddress geocode(String address) throws TigerParseException, IOException, GeoCodingException {
			Address addr = _geocoder.parseAddress(address);
			LocatedAddress located = _geocoder.lookupAddress(addr);
			return located;
	}
	
	public void close() throws GeoCodingException, IOException, GeoIndexingException {
		try {
			if (_geocoder != null) {
				_geocoder.close();
			}
		} finally {
			try {
				if (_writer != null) {
					_writer.optimize();
				}
			} finally {
				try {
					if (_writer != null) {
						_writer.close();
					}
				} finally {
					_geocoder = null;
					_writer = null;
				}
			}
		}
	}
}
