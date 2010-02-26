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

package com.browseengine.local.service.geoindex.test;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.browseengine.local.service.Address;
import com.browseengine.local.service.LocatedAddress;
import com.browseengine.local.service.geocode.GeoCode;
import com.browseengine.local.service.geocode.GeoCodeImpl;
import com.browseengine.local.service.geocode.GeoCodingException;
import com.browseengine.local.service.geoindex.YahooYpParser;
import com.browseengine.local.service.geoindex.YahooYpParser.YahooYpRecord;
import com.browseengine.local.service.index.IndexConstants;
import com.browseengine.local.service.tiger.TigerParseException;

/**
 * @author spackle
 *
 */
public class GeoCodingTest extends TestCase {
	private static final Logger LOGGER = Logger.getLogger(GeoCodingTest.class);

	public static File getGeoCodeDir() {
		return new File(getGeoDir(), IndexConstants.GEOCODE_DIR);
	}

	public static final File getGeoDir() {
		String geocodeDir = System.getProperty("TEST_GEOCODE_DIR");
		if (geocodeDir == null) {
			geocodeDir = "/usr/local/browseengine/geo";
		}
		return new File(geocodeDir);
	}
	
	public static final File getYpAddressFile() {
		String ypAddressFile = System.getProperty("TEST_YP_ADDRESS_PATH");
		if (ypAddressFile == null) {
			File f = new File(getGeoDir(), "yp.yahoo.com");
			return new File(f, "yp_0.txt");
		}
		return new File(ypAddressFile);
	}
	
	public static final String[] ADDRESSES = {
		//"1411 18th St, San Francisco, CA",
		"1124 Clay St., San Francisco, CA 94108",
		"5999 Lindhurst Ave., Marysville, CA 95901",
		"5999 Lindhurst Ave., Linda, CA 95901",
		"5997 Lindhurst Ave., Linda, CA 95901",
		"1608 N Beale Rd., Linda, CA 95901",
		"1608 Beale Rd., Linda, CA 95901",
		"1665 Beale Rd., Linda, CA 95901",
	};
	
	public void testLookUpAddress() throws Throwable {
		
		try {
			for (String address : ADDRESSES) {
				doLookup(address);
			}
		} catch (Throwable t) {
			LOGGER.error("fail: "+t, t);
			throw t;
		} finally {
		}
		
	}
	
	public void testLookupYpAddresses() throws Throwable {
		YahooYpParser parser = null;
		try {
			File f= getYpAddressFile();
			parser = new YahooYpParser(f);
			int geocodeErrCount = 0;
			int ypParseErrCount = 0;
			int geocodeSuccessCount = 0;
			while (parser.hasNext()) {
				YahooYpRecord record = parser.next();
				if (record != null) {
					String addr = record.addrLine1+", "+record.addrLine2;
					try {
						doLookup(addr);
						geocodeSuccessCount++;
					} catch (Throwable t) {
						LOGGER.warn("failed to lookup address: "+addr);
						geocodeErrCount++;
					}
				} else {
					LOGGER.warn("failed on YahooYP parser");
					ypParseErrCount++;
				}
			}
			LOGGER.info("Success on "+(geocodeSuccessCount)+"/"+(geocodeSuccessCount+geocodeErrCount)+" yp records, or "+((float)geocodeSuccessCount*100f/(geocodeSuccessCount+geocodeErrCount))+" % success");
			LOGGER.info("also, failed in yahoo yp parsing "+ypParseErrCount+" times");
			float ratio = (float)geocodeSuccessCount/(geocodeSuccessCount+geocodeErrCount);
			assertTrue("too many failures: "+geocodeErrCount+", total success ratio: "+ratio, geocodeSuccessCount > 4 && ratio > 0.75f);
		} catch (Throwable t) {
			LOGGER.error("fail: "+t, t);
			throw t;			
		} finally {
			try {
				if (parser != null) {
					parser.close();
				}
			} finally {
				parser = null;
			}
		}
	}

	private GeoCode _geocoder;
	
	protected void setUp() throws Exception {
		try {
		_geocoder = new GeoCodeImpl(getGeoCodeDir());
		} finally {
		super.setUp();
		}
	}
	
	protected void tearDown() throws Exception {
		try {
			if (_geocoder != null) {
				_geocoder.close();
			}
		} finally {
			_geocoder = null;
			super.tearDown();
		}
	}
	
	private void doLookup(String address) throws TigerParseException, IOException, GeoCodingException {
		boolean success = false;
		
		Address addr = _geocoder.parseAddress(address);
		if (addr != null) {
			LocatedAddress located = _geocoder.lookupAddress(addr);
			LOGGER.info("lookup "+address+" gave "+located);
			success = true;
		}
		assertTrue("didn't find address: "+address, success);
	}
}
