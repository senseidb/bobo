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

package com.browseengine.local.service.geosearch;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.PriorityQueue;

import com.browseengine.local.service.Locatable;

/**
 * @author spackle
 *
 */
public class GeoPriorityQueue extends PriorityQueue {
	private int _hitCount = 0;
	private Locatable _centroid;
	
	public GeoPriorityQueue(Locatable centroid, int maxSize) {
		_centroid = centroid;
		initialize(maxSize);
	}

	public Locatable getCentroid() {
		return _centroid;
	}
	
	public int getHitCount() {
		return _hitCount;
	}
	
	public boolean insert(Object o) {
		_hitCount++;
		return super.insert(o);
	}
	
	@Override
	protected boolean lessThan(Object a, Object b) {
	    ScoreDoc hitA = (ScoreDoc)a;
	    ScoreDoc hitB = (ScoreDoc)b;
	    if (hitA.score == hitB.score)
	      return hitA.doc > hitB.doc; 
	    else
	    	// opposite from lucene
	      return hitA.score > hitB.score;
	}

}
