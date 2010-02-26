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

package com.browseengine.local.service.impl.test;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

import com.browseengine.local.service.LocalRequest;
import com.browseengine.local.service.LocalResource;
import com.browseengine.local.service.LocalResult;
import com.browseengine.local.service.LocalService;
import com.browseengine.local.service.LocalService.LocalException;
import com.browseengine.local.service.geoindex.test.GeoCodingTest;
import com.browseengine.local.service.impl.SingletonLocalServiceFactory;

/**
 * @author spackle
 *
 */
public class LocalServiceTest extends TestCase {
	private static final Logger LOGGER = Logger.getLogger(LocalServiceTest.class);	
	
	public void testGeoCodeAndSearch() throws Throwable {
		try {
			LocalService local = SingletonLocalServiceFactory.getLocalServiceImpl();
			
			for (String addressStr : GeoCodingTest.ADDRESSES) {
				geosearch(local, addressStr);
			}
			
			// don't close local; it's a singleton shared elsewhere in this VM
		} catch (Throwable t) {
			LOGGER.error("fail: "+t, t);
			throw t;
		} finally {
			//
		}
	}
	
	public void testGeoCodeSearchAndPage() throws Throwable {
		try {
			LocalService local = SingletonLocalServiceFactory.getLocalServiceImpl();			
			for (String addressStr : GeoCodingTest.ADDRESSES) {
				geosearchAndPage(local, addressStr);
			}
		} catch (Throwable t) {
			LOGGER.error("fail: "+t, t);
			throw t;
		}
	}
	
	private void geosearchAndPage(LocalService local, String address) throws LocalException {
		LocalRequest request = new LocalRequest();
		request.setAddressStr(address);
		request.setRangeInMiles(10f);
		LocalResult result = local.search(request);
		if (result.getNumHits() < 1) {
			request.setRangeInMiles(500f);
			result = local.search(request);
		}
		assertTrue("not enough results: "+(result != null ? result.getNumHits() : null), result != null && result.getNumHits() > 0);
		LocalResource[] zeroThru9 = local.fetch(result, 0, 10);
		assertTrue("zeroThru9 wasn't the right length: "+(zeroThru9 != null ? zeroThru9.length : null), zeroThru9 != null && zeroThru9.length > 0 && (result.getNumHits() > 10 ? 10 == zeroThru9.length : result.getNumHits() == zeroThru9.length));
		float distance = zeroThru9[0].getDistanceInMiles();
		for (int i = 1; i < zeroThru9.length; i++) {
			assertTrue("distance at "+i+" wasn't sorted, prev: "+distance+", current: "+zeroThru9[i].getDistanceInMiles(), distance <= zeroThru9[i].getDistanceInMiles());
			distance = zeroThru9[i].getDistanceInMiles();
		}
		if (result.getNumHits() > 1) {
			LocalResource[] oneThru9 = local.fetch(result, 1, 9);
			assertTrue("oneThru9 wasnt' the right length: "+(oneThru9 != null ? oneThru9.length : null), oneThru9 != null && oneThru9.length == zeroThru9.length -1);
			for (int i = 0; i < oneThru9.length; i++) {
				LocalResource zero = zeroThru9[i+1];
				LocalResource one = oneThru9[i];
				assertTrue("values weren't the same at i = "+i, zero.getDistanceInMiles() == one.getDistanceInMiles() && zero.getName().equals(one.getName()));
			}
		}
		
		if (result.getNumHits() > 10) {
			LocalResource[] secondPage = local.fetch(result, 10, 10);
			assertTrue("second page wasn't long enough: "+(secondPage != null ? secondPage.length : null), secondPage != null && secondPage.length > 0);
			for (int i = 0; i < secondPage.length; i++) {
				assertTrue("second page distance at "+i+" wasn't sorted, prev: "+distance+", current: "+secondPage[i].getDistanceInMiles(), distance <= secondPage[i].getDistanceInMiles());
				distance = secondPage[i].getDistanceInMiles();
			}
		} else {
			LocalResource[] secondPage = local.fetch(result, 10, 1);
			assertTrue("second page wasn't empty, when had "+result.getNumHits()+" hits", secondPage != null && secondPage.length == 0);			
		}
	}
	
	private void geosearch(LocalService local, String address) throws LocalException {
		LocalRequest request = new LocalRequest();
		request.setAddressStr(address);
		request.setRangeInMiles(10f);
		LocalResult result = local.search(request);
		if (result.getNumHits() < 1) {
			request.setRangeInMiles(500f);
			result = local.search(request);
		}
		LocalResource[] resources = local.fetch(result, 0, 10);
		assertTrue("for address: "+address+"; didn't get enough resources: "+(resources != null ? resources.length : resources), resources != null && resources.length > 0);
		StringBuilder buf = new StringBuilder().append("got back ").append(result.getNumHits()).append(" hits, displaying ").append(resources.length).append(":\n");
		for (int i = 0; i < resources.length; i++) {
			LocalResource resource = resources[i];
			buf.append("  ").append((i+1)).append(". ").append(resource).append('\n');
		}
		LOGGER.info(buf.toString());
	}
}
