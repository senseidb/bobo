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

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortField;

import com.browseengine.local.glue.GeoSearchFieldPlugin.GeoPluginFieldData;
import com.browseengine.local.service.Conversions;
import com.browseengine.local.service.Locatable;
import com.browseengine.local.service.geosearch.HaversineWrapper;

/**
 * @author spackle
 *
 */
public class GeoSearchDocComparator implements ScoreDocComparator {
	private GeoPluginFieldData _lonLats;
	private double _centerLonRad;
	private double _centerLatRad;
	GeoSearchDocComparator(GeoPluginFieldData data, Locatable centroid){
		_lonLats=data;
		_centerLonRad = centroid.getLongitudeRad();
		_centerLatRad = centroid.getLatitudeRad();
	}
	GeoSearchDocComparator(GeoPluginFieldData data, double centerLonDegrees, double centerLatDegrees) {
		_lonLats = data;
		_centerLonRad = Conversions.d2r(centerLonDegrees);
		_centerLatRad = Conversions.d2r(centerLatDegrees);
	}
	public int compare(ScoreDoc i, ScoreDoc j) {
		float d_i = HaversineWrapper.computeHaversineDistanceMiles(_centerLonRad, _centerLatRad, _lonLats.lons[i.doc], _lonLats.lats[i.doc]);
		float d_j = HaversineWrapper.computeHaversineDistanceMiles(_centerLonRad, _centerLatRad, _lonLats.lons[j.doc], _lonLats.lats[j.doc]);
		if (d_i < d_j) {
			return -1;
		} else if (d_i == d_j) {
			return 0;
		} else {
			return 1;
		}
	}
	public int sortType() {
		return SortField.FLOAT;
	}
	public Comparable sortValue(ScoreDoc i) {
		float d_i = HaversineWrapper.computeHaversineDistanceMiles(_centerLonRad, _centerLatRad, _lonLats.lons[i.doc], _lonLats.lats[i.doc]);
		return new Float(d_i);
	}
}
