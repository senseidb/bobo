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

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.lucene.store.RAMDirectory;

import com.browseengine.local.service.LocalResource;
import com.browseengine.local.service.geoindex.GeoIndexingException;
import com.browseengine.local.service.geoindex.GeoResourceWriter;

/**
 * @author spackle
 *
 */
public class GeoResourceWriterTest extends TestCase {
	private static final Logger LOGGER = Logger.getLogger(GeoResourceWriterTest.class);

	public void testWrite() throws Throwable {
		GeoResourceWriter writer = null;
		try {
			RAMDirectory ramDir = new RAMDirectory();
			writer = new GeoResourceWriter(ramDir, true);
			
			LocalResource resource = new LocalResource("Spackle Me", "A place to go to get spackle.", "123 Fake St., Springfiled, ??", 4155551212L, -101., 45.1, 1);
			writer.addResource(resource);
			
			resource = new LocalResource("Bobo Foobar", "A place where you can hop to it.", "321 Singleton Ave., Chicago, IL 01123", 4155555785L, -95, 40, 2);
			writer.addResource(resource);
			
			try {
				resource = new LocalResource("Bad Record", "This record should never make the index", "The Moon, Alice!", 4155559292L, -200, 22, 100);
				writer.addResource(resource);
				fail("should have thrown an exception for an out-of-range resource, but didn't");
			} catch (GeoIndexingException gie) {
				// okay
			}
			
			writer.optimize();
			writer.close();
			writer = null;
			
			LOGGER.info(getName()+": success!");
		} catch (Throwable t) {
			LOGGER.error("fail: "+t, t);
			throw t;
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}
}
