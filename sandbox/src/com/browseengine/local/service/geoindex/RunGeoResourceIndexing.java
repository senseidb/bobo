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

import org.apache.log4j.Logger;

/**
 * @author spackle
 *
 */
public class RunGeoResourceIndexing {
	private static final Logger LOGGER = Logger.getLogger(RunGeoResourceIndexing.class);

	private static void usage() {
		System.err.println("incorrect input arguments.\nUsage: java com.browseengine.local.service.geoindex.RunGeoResourceIndexing <geocodepath> <geosearchpath> <yahooyppath>");
		System.exit(-1);
	}
	
	public static void main(String[] argv) {
		if (argv.length != 3) {
			usage();
		}
		GeoResourceIndexer indexer = null;
		try {
			indexer = new GeoResourceIndexer(new File(argv[0]), new File(argv[1]));
			indexer.parseYahooYpFile(new File(argv[2]));
			indexer.close();
			indexer = null;
		} catch (Throwable t) {
			LOGGER.error("failed during indexing: "+t);
			t.printStackTrace();
		} finally {
			try {
				if (indexer != null) {
					indexer.close();
				}
			} catch (Throwable t) {
				LOGGER.error("failed during close: "+t);
				t.printStackTrace();
			}
			finally {
				indexer = null;
			}
		}
	}
}
