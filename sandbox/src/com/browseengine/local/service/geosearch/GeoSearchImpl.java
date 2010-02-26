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
 * send mail to owner@browseengine.com.
 */

package com.browseengine.local.service.geosearch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;

import com.browseengine.local.service.Address;
import com.browseengine.local.service.LocalRequest;
import com.browseengine.local.service.LocalResource;
import com.browseengine.local.service.LocalResult;
import com.browseengine.local.service.Locatable;
import com.browseengine.local.service.geocode.GeoCode;
import com.browseengine.local.service.geocode.GeoCodingException;
import com.browseengine.local.service.index.GeoSearchFields;
import com.browseengine.local.service.tiger.TigerParseException;

/**
 * @author spackle
 *
 */
public class GeoSearchImpl implements GeoSearch {
	/**
	 * underlying index comes sorted by longitude.
	 * docid is the index in this array.
	 * lons[docid] is the longitude in degrees.
	 */
	private int[] _lons;
	/**
	 * lats are not sorted in the underlying index.
	 * docid is the index in this array.
	 * inclusion/eclusion from the candidate result set can be 
	 * done with two quick checks if this value is 
	 * outside the candidate result bounds.
	 */
	private int[] _lats;
	private GeoCode _geocoder;
	private Searcher _searcher;
	private IndexReader _reader;
	private int _maxDoc;
	
	public GeoSearchImpl(GeoCode geocoder, File f) throws IOException, GeoSearchingException {
		try {
			_geocoder = geocoder;
			_reader = IndexReader.open(f);
			_searcher = new IndexSearcher(_reader);
			_lons = loadDegreeFieldIntoInt(_reader, GeoSearchFields.LON.getField());
			checkSorted(_lons);
			_lats = loadDegreeFieldIntoInt(_reader, GeoSearchFields.LAT.getField());
			_maxDoc = _reader.maxDoc();
		} catch (IOException ioe) {
			close();
			throw ioe;
		}
	}
	
	private void checkSorted(int[] ints) throws GeoSearchingException {
		int prev = Integer.MIN_VALUE;
		for (int i = 0; i < ints.length; i++) {
			int val = ints[i];
			if (val < prev) {
				throw new GeoSearchingException("array of ints not sorted in nondescending order at index: "+i+", val: "+val);
			}
			prev = val;
		}
	}
	
	/**
	 * lon and lat are stored in the index as 7-decimal place precision values of 
	 * degrees.
	 * 2^32 = 4,294,967,296 values.  signed gives us -2^31 to 2^31-1.
	 * if we multiply the normal value 179.123456 by 10^6, we get 179,123,456, 
	 * which fits in this space.
	 * if we multiply the normal value 179.1234567 by 10^7, we get 1,791,234,567, 
	 * which fits in this space.
	 * @throws IOException
	 * @throws GeoSearchingException 
	 */
	public static int[] loadDegreeFieldIntoInt(IndexReader reader, String fld) throws IOException, GeoSearchingException {
		int[] vals = new int[reader.maxDoc()];
		TermEnum termEnum = null;
		TermDocs termDocs = null;
		try {
			fld = fld.intern();
			Term term = new Term(fld, "");
			termEnum=  reader.terms(term);
			termDocs = reader.termDocs();
			do {
				term = termEnum.term();
				if (null == term || term.field() != fld) {
					break;
				}
				termDocs.seek(term);
				int numAdded = 0;
				while (termDocs.next()) {
					String str = term.text();
					double dub = Double.parseDouble(str);
					vals[termDocs.doc()] = GeoSearchFields.dubToInt(dub);
					numAdded++;
				}
				if (numAdded <= 0) {
					throw new GeoSearchingException("data integrity problem in field "+fld+", term "+term.text());
				}
			} while (termEnum.next());
			
			return vals;
		} catch (NumberFormatException nfe) {
			throw new GeoSearchingException("data integrity problem, non-numeric field value for field "+fld+": "+nfe, nfe);
		} finally {
			try {
				if (termDocs != null) {
					termDocs.close();
				}
			} finally {
				try {
					if (termEnum != null) {
						termEnum.close();
					}
				} finally {
					//
				}
			}
		}
	}

	public void close() throws GeoSearchingException {
		try {
			if (_searcher != null) {
				_searcher.close();
			}
		} catch (IOException ioe) {
			throw new GeoSearchingException(ioe.toString(), ioe);
		} finally {
			try {
				if (_reader != null) {
					_reader.close();
				}
			} catch (IOException ioe) {
				throw new GeoSearchingException(ioe.toString(), ioe);
			} finally {
				_searcher = null;
				_reader = null;
				_lons = null;
				_lats = null;
			}
		}
	}
	
	public LocalResource[] fetch(LocalResult result, int start, int range) throws IOException, GeoSearchingException, TigerParseException, GeoCodingException {
		if (result == null || result.getRequest() == null) {
			throw new GeoSearchingException("result was null");
		}
		if (range <= 0) {
			throw new GeoSearchingException("can't fetch zero or less results, range was "+range);
		}
		if (start < 0) {
			throw new GeoSearchingException("can't fetch results starting from a negative index, start was "+start);
		}
		if (start > result.getNumHits()) {
			return new LocalResource[0];
		}
		if (start+range > MAX_DOC) {
			throw new GeoSearchingException("not supporting paging past the first "+MAX_DOC+" results yet");
		}
		LocalRequest request = result.getRequest();
		GeoPriorityQueue queue= innerSearch(request, result.getCentroid());
		// else, we fetch from the queue
		ScoreDoc[] hits = new ScoreDoc[queue.size()];
		for (int i = hits.length-1; i >= 0; i--) {
			hits[i] = (ScoreDoc)queue.pop();
		}
		if (start+range > queue.getHitCount()) {
			range = queue.getHitCount()-start;
		}
		LocalResource[] hitResources = new LocalResource[range];
		for (int i = 0; i < range; i++) {
			hitResources[i] = getHitResource(hits[start+i]);
		}
		return hitResources;
	}

	private LocalResource getHitResource(ScoreDoc hit) throws IOException {
		Document doc = _reader.document(hit.doc);
		String name = doc.get(GeoSearchFields.NAME.getField());
		String description = doc.get(GeoSearchFields.DESCRIPTION.getField());
		String address = doc.get(GeoSearchFields.ADDRESS.getField());
		String phone = doc.get(GeoSearchFields.PHONE.getField());
		long phoneL = -1L;
		if (GeoSearchFields.validIndexedPhoneNumber(phone)) {
			phoneL = Long.parseLong(phone);
		}
		double lon = GeoSearchFields.intToDub(_lons[hit.doc]);
		double lat = GeoSearchFields.intToDub(_lats[hit.doc]);
		LocalResource hitResource = new LocalResource(name, description, address, phoneL, lon, lat, hit.score);
		return hitResource;
	}
	
	private static final int MAX_DOC = 50;

	public LocalResult search(LocalRequest request) throws IOException, GeoSearchingException, TigerParseException, GeoCodingException {
		if (request == null || (request.getPoint() == null && request.getAddress() == null && request.getAddressStr() == null)) {
			throw new GeoSearchingException("request was null");
		}
		LocalResult result = null;
		//result = _searchCache.getObject(request);
		if (result != null) {
			return result;
		}

		GeoPriorityQueue queue = innerSearch(request, null);
		return new LocalResult(new LocalRequest(request), queue.getHitCount(), queue.getCentroid());
	}

	private GeoPriorityQueue innerSearch(LocalRequest request, Locatable knownCentroid) throws IOException, GeoSearchingException, TigerParseException, GeoCodingException {
		// TODO: desparately need caching to avoid re-running innerSearch for the standard 
		// search(...) then fetch(...) API sequence
		Locatable point = knownCentroid;
		if (null == point) {
			point = request.getPoint();
			if (null == point) {
				// we need to figure out lon/lat
				Address address = request.getAddress();
				if (address == null) {
					String query = request.getAddressStr();
					if (query == null) {
						throw new GeoSearchingException("you must specify at least one of point, address, or query on GeoSearch.search calls");
					}
					// callback
					address = _geocoder.parseAddress(query);				
					if (null == address) {
						throw new GeoSearchingException("unable to parse '"+query+"' into a valid address");
					}
				}
				// callback
				point = _geocoder.lookupAddress(address);
				if (null == point) {
					throw new GeoSearchingException("unable to locate lon/lat for address '"+point+"'");
				}
			}
		}
		
		float radius = request.getRangeInMiles();
		if (radius <= 0f) {
			radius = 5f;
		}
		int[] bounds = HaversineWrapper.computeLonLatMinMaxAsInt(point, radius);
		int lowdocid = Arrays.binarySearch(_lons, bounds[HaversineWrapper.LON_MIN]);
		if (lowdocid < 0) {
			lowdocid = -1-lowdocid;
		}
		// doc at lowdocid is the first candidate
		int highdocid = Arrays.binarySearch(_lons, bounds[HaversineWrapper.LON_MAX]);
		if (highdocid < 0) {
			highdocid = -1-highdocid;
			// highdocid is actually too high a value
			highdocid--;
		} else {
			// highdocid is a valid match, as well as any following that are equal
			while (highdocid+1 < _lons.length && _lons[highdocid] == _lons[highdocid+1]) {
				highdocid++;
			}
		}
		// doc at highdocid is the last candidate
		if (highdocid < lowdocid) {
			return new GeoPriorityQueue(point, 20);
		}

		GeoPriorityQueue queue = new GeoPriorityQueue(point, MAX_DOC);

		// check every value in the region
		double lon1rad = point.getLongitudeRad();
		double lat1rad = point.getLatitudeRad();
		for (int i = lowdocid; i < highdocid; i++) {
			// check if a candidate
			if (_lats[i] >= bounds[HaversineWrapper.LAT_MIN] && _lats[i] <= bounds[HaversineWrapper.LAT_MAX]) {
				float distance = HaversineWrapper.computeHaversineDistanceMiles(lon1rad, lat1rad, _lons[i], _lats[i]);
				if (distance < radius) {
					// add to queue
					queue.insert(new ScoreDoc(i, distance));
				}
			}
		}
		return queue;
	}
	
	//private SimpleHardCache<LocalRequest,Hits> _searchCache = new SimpleHardCache<LocalRequest,Hits>(50);

	/*
	private static class Hits {
		public ScoreDoc[] scoreDocs;
		public int totalHits;
	}
	*/
}
