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

package com.browseengine.local.glue;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.browseengine.bobo.cache.FieldDataCache;
import com.browseengine.bobo.fields.FieldPlugin;
import com.browseengine.bobo.index.BoboIndexReader;
import com.browseengine.bobo.score.ChainedScoreAdjuster;
import com.browseengine.bobo.score.ScoreAdjuster;
import com.browseengine.bobo.score.ScoreAdjusterFactory;
import com.browseengine.bobo.service.BrowseRequest;
import com.browseengine.bobo.service.BrowseSelection;
import com.browseengine.bobo.service.FieldConfiguration;
import com.browseengine.bobo.service.BrowseService.BrowseException;
import com.browseengine.local.glue.GeoSearchFieldPlugin.GeoPluginFieldData;
import com.browseengine.local.service.Locatable;
import com.browseengine.local.service.LonLat;

/**
 * @author spackle
 *
 */
public class GeoScoreAdjusterFactory implements ScoreAdjusterFactory {
	// TODO: check that there is not a problem with refreshing the data!!!!
	private Map<String,GeoPluginFieldData> geoFields;
	
	public GeoScoreAdjusterFactory() {
		
	}
	
	public void setBoboIndexReader(BoboIndexReader reader) {
		geoFields = new Hashtable<String,GeoPluginFieldData>();
		FieldConfiguration fConf=reader.getFieldConfiguration();
		String[] fieldNames=fConf.getFieldNames();
		
		String geoFldType = new GeoSearchFieldPlugin().getTypeString();
		if (geoFldType != null) {
			for (String fieldName : fieldNames){
				FieldPlugin plugin=fConf.getFieldPlugin(fieldName);			
				String fldType = plugin.getTypeString();
				if (fldType != null) {
					FieldDataCache fieldDataCache = reader.getIndexData().getFieldDataCache(fieldName);
					GeoPluginFieldData data = (GeoPluginFieldData)fieldDataCache.getUserObject();
					geoFields.put(fieldName, data);
				}
			}
		}
	}
	
	public ScoreAdjuster newScoreAdjuster(BrowseRequest br) throws BrowseException {
		BrowseSelection[] sels = br.getSelections();
		List<ScoreAdjuster> list = new ArrayList<ScoreAdjuster>();
		for (int i = 0; i < sels.length; i++) {
			BrowseSelection sel = sels[i];
			String fieldName = sel.getFieldName();
			if (fieldName != null) {
				GeoPluginFieldData data = geoFields.get(fieldName);
				if (data != null) {
					// add a geo score adjuster
					GeoSearchSelection[] gsels = GeoSearchSelection.parse(sel.getValues());
					if (gsels != null) {
						for (GeoSearchSelection gsel : gsels) {
							Locatable locatable = LonLat.getLonLatDeg(gsel.getLon(), gsel.getLat());
							ScoreAdjuster sa = new GeoScoreAdjuster(data, locatable, gsel.getRangeInMiles());
							list.add(sa);
						}
					}
				}
			}
		}
		if (list.size() > 1) {
			ChainedScoreAdjuster chain = new ChainedScoreAdjuster();
			Iterator<ScoreAdjuster> iter = list.iterator();
			while (iter.hasNext()) {
				chain.addScoreAdjuster(iter.next());
			}
			return chain;
		} else if (list.size() > 0) {
			return list.get(0);
		}
		return null;
	}

}
