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

import java.io.File;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.browseengine.bobo.fields.FieldPlugin;
import com.browseengine.bobo.index.BoboIndexReader;
import com.browseengine.bobo.service.BrowseHit;
import com.browseengine.bobo.service.BrowseRequest;
import com.browseengine.bobo.service.BrowseResult;
import com.browseengine.bobo.service.BrowseSelection;
import com.browseengine.bobo.service.BrowseService;
import com.browseengine.bobo.service.BrowseServiceFactory;
import com.browseengine.bobo.service.FieldConfiguration;
import com.browseengine.local.glue.BrowseGeoSearchInitializer;
import com.browseengine.local.glue.GeoScoreAdjusterFactory;
import com.browseengine.local.glue.GeoSearchFieldPlugin;

/**
 * @author spackle
 *
 */
public class BrowseGeoSearchTest extends TestCase {
	private static final Logger LOGGER = Logger.getLogger(BrowseGeoSearchTest.class);	

	private BrowseService _browseService;
	private String _geosearchField;
	
	private static final String INDEX_KEY = "TEST_BROWSE_INDEX";
	
	protected void setUp() throws Exception {
		super.setUp();
		// register the plugin first!
		BrowseGeoSearchInitializer.init();
		String path = System.getProperty(INDEX_KEY);
		if (path == null) {
			throw new Exception("no '"+INDEX_KEY+"' set; can't run tests");
		}
		IndexReader reader = null;
		BoboIndexReader breader =null;
		Directory dir = null;
		boolean success = false;
		try {
			dir = FSDirectory.getDirectory(path, false);
			File f = new File(path, "field.xml");
			FieldConfiguration config = FieldConfiguration.loadFieldConfiguration(f);
			GeoScoreAdjusterFactory factory = new GeoScoreAdjusterFactory();
			reader = IndexReader.open(dir);
			breader = new BoboIndexReader(reader,dir,config);
			breader.setScoreAdjusterFactory(factory);
			// find the first geosearch field plugin
			GeoSearchFieldPlugin gplugin = new GeoSearchFieldPlugin();
			String[] fieldNames = config.getFieldNames();
			for (String fieldName : fieldNames) {
				FieldPlugin plugin = config.getFieldPlugin(fieldName);
				if (plugin.getTypeString().equals(gplugin.getTypeString())) {
					_geosearchField = fieldName;
					success = true;
				}
			}
			if (success) {
				success = false;
				_browseService = BrowseServiceFactory.createBrowseService(breader);
				success = true;
			}
		} finally {
			if (!success) {
				try {
					if (breader != null) {
						breader.close();
					}
				} finally {
					try {
						if (reader != null) {
							reader.close();
						}
					} finally {
						try {
							if (dir != null) {
								dir.close();
							}
						} finally {
							//
						}
					}
				}
			}
		}
	}

	protected void tearDown() throws Exception {
		try {
			if (_browseService != null) {
				_browseService.close();
			}
		} finally {
			_browseService = null;
		}
		super.tearDown();
	}
	
	public void testBrowseGeoSearch() throws Throwable {
		try {
			BrowseRequest req = new BrowseRequest();
			BrowseSelection sel = new BrowseSelection(_geosearchField);
			int lon = 82;
			int lat = 27;
			int range = 100;
			sel.addValue("("+lon+","+lat+"):"+range);
			req.addSelection(sel);
			req.setCount(10);
			BrowseResult res = _browseService.browse(req);
			LOGGER.info("got back "+res.getNumHits()+" hits");
			
			StringBuilder buffer = new StringBuilder();
			BrowseHit[] hits=res.getHits();
			for (int i=0;i<hits.length;++i){
				if (i!=0){
					buffer.append('\n');
				}
				buffer.append(hits[i]);
			}
			LOGGER.info(buffer.toString());

		} catch (Throwable t) {
			LOGGER.error("fail: "+t, t);
			throw t;
		} finally {
			//
		}
	}
}
