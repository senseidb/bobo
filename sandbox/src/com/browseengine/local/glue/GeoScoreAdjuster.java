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

import com.browseengine.bobo.score.ScoreAdjuster;
import com.browseengine.local.glue.GeoSearchFieldPlugin.GeoPluginFieldData;
import com.browseengine.local.service.Locatable;
import com.browseengine.local.service.geosearch.HaversineWrapper;

/**
 * Ignores the input score coming in on the chain.
 * Returns 1/(1+distance).
 * 
 * At the end, we can extract out the true distance 
 * by calling 
 * 
 * @author spackle
 *
 */
public class GeoScoreAdjuster implements ScoreAdjuster {
	private GeoPluginFieldData _lonLats;
	private double lonRadians;
	private double latRadians;
	private float rangeInMiles;
	
	public GeoScoreAdjuster(GeoPluginFieldData data, Locatable locatable, float rangeInMiles) {
		_lonLats = data;
		lonRadians = locatable.getLongitudeRad();
		latRadians = locatable.getLatitudeRad();
		this.rangeInMiles = rangeInMiles;
	}

	/**
	 * Gets the distance, in miles, that the given score represents.
	 * Recall that the scores are overwritten so that the higher value scores still 
	 * get to the head of the sort list.
	 * 
	 * @param score
	 * @return
	 */
	public static float getDistanceFromScore(float score) {
		if (score != 0f) {
			return 1f/score-1f;
		}
		return 1000000f; // 1 million miles away
	}
	
	/* (non-Javadoc)
	 * @see com.browseengine.bobo.score.ScoreAdjuster#adjustScore(int, float)
	 */
	public float adjustScore(int docid, float origScore) {
		float d_i = HaversineWrapper.computeHaversineDistanceMiles(lonRadians, latRadians, _lonLats.lons[docid], _lonLats.lats[docid]);
		if (d_i <= rangeInMiles) {
			return 1f/(1f+d_i);
		} else {
			return 0f;
		}
	}

}
