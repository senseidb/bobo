/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2006  Spackle
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
 * please go to https://sourceforge.net/projects/bobo-browse/.
 */

package com.browseengine.local.service.geocode;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;

import com.browseengine.local.service.Address;
import com.browseengine.local.service.LocatedAddress;
import com.browseengine.local.service.index.SegmentsFields;

/**
 * @author spackle
 *
 */
public class SegmentSearcher {
	private static final Logger LOGGER = Logger.getLogger(SegmentSearcher.class);

	private Searcher _searcher; // TLID,placeL,stateL,placeR,stateR,startLon,startLat,endLon,endLat
	private IndexReader _reader;
	private int _maxDoc;
	
	public SegmentSearcher(File f) throws IOException {
		try {
			_reader = IndexReader.open(f);
			_searcher = new IndexSearcher(_reader);
			_maxDoc = _reader.maxDoc();
		} catch (IOException ioe) {
			try {
				close();
			} catch (IOException ioe2) {
				LOGGER.warn("trouble closing SegmentsSearcher, after trouble opening it", ioe2);
			}
			throw ioe;
		}
	}
	
	public LocatedAddress constructLocalResource(RangeMatch rangeMatch, Address addressSoFar) throws IOException {
		TermDocs termDocs = null;
		try {
			Term term = new Term(SegmentsFields.TLID.getField(),""+rangeMatch.tlid);
			termDocs = _reader.termDocs(term);
			if (termDocs.next()) {
				int docid = termDocs.doc();
				Document doc = _reader.document(docid);
				String placeL = doc.get(SegmentsFields.PLACEL.getField());
				String stateL = doc.get(SegmentsFields.STATEL.getField());
				String placeR = doc.get(SegmentsFields.PLACER.getField());
				String stateR = doc.get(SegmentsFields.STATER.getField());
				
				String city;
				String state;
				if (rangeMatch.lrboth == PlaceMatch.LEFT) {
					if (placeL != null) {
						city = placeL;
					} else {
						city = placeR;
					}
					if (stateL != null) {
						state = stateL;
					} else {
						state = stateR;
					}
				} else {
					if (placeR != null) {
						city = placeR;
					} else {
						city = placeL;
					}
					if (stateR != null) {
						state = stateR;
					} else {
						state = stateL;
					}
				}
				Address a = addressSoFar;
				Address addr = new Address(a.getNumber(), a.getStreetPrefix(), 
						a.getStreetName(), a.getStreetType(), 
						a.getStreetSuffix(), a.getAptNo(), city, 
						state, a.getZip5(), 
						a.getCountry());
				// linear interpolation
				float percent = rangeMatch.toPercent;
				double startLon = Double.parseDouble(doc.get(SegmentsFields.START_LON.getField()));
				double startLat = Double.parseDouble(doc.get(SegmentsFields.START_LAT.getField()));
				double endLon = Double.parseDouble(doc.get(SegmentsFields.END_LON.getField()));
				double endLat = Double.parseDouble(doc.get(SegmentsFields.END_LAT.getField()));
				double lon = startLon + percent*(endLon-startLon);
				double lat = startLat + percent*(endLat-startLat);
				lon = adjustDegr(lon);
				lat = adjustDegr(lat);
				LocatedAddress localResource = new LocatedAddress(
						addr, lon, lat);
				return localResource;
			}
			return null;
		} finally {
			try {
				if (termDocs != null) {
					termDocs.close();
				}
			} catch (IOException ioe) {
				LOGGER.warn("trouble closing termdocs: "+ioe,ioe);
			}
		}

	}
	
	private static double adjustDegr(double in) {
		return in/1000000.;
	}
	
	public PlaceMatch lookupPlace(String city, String state) throws GeoCodingException, IOException {
		if (state.length() != 2) {
			throw new GeoCodingException("improper 2-letter state code: "+state);
		}
		city = GeoCodeImpl.replaceWhitespacesWithASpace(city);
		state = state.toLowerCase();
		city = city.toLowerCase();
		// now, if we search on city,state, we should get back a 
		// set of docids, and whether we matched on left, right, or both

		// could eventually pull stateL and stateR into TernarySets in memory
		// this would cost about 2x8 bits per segment = 2 bytes per segment
		TermDocs td = null;
		try {
			td = _reader.termDocs();
			// right
			BitSet right = cityAndStateMatch(td, city, state, false);
			// left 
			BitSet left = cityAndStateMatch(td, city, state, true);
			
			PlaceMatch place = new PlaceMatch();
			int nextRight = right.nextSetBit(0);
			int nextLeft = left.nextSetBit(0);
			while (nextLeft >= 0 || nextRight >= 0) {
				if (nextLeft >= 0 && nextRight >= 0) {
					// we want the smaller one
					if (nextLeft == nextRight) {
						// advance both
						place.tlidSideMap.put(getTLID(nextLeft),PlaceMatch.BOTH);
						nextLeft = left.nextSetBit(nextLeft+1);
						nextRight = right.nextSetBit(nextRight+1);
					} else if (nextLeft < nextRight) {
						// advance left
						place.tlidSideMap.put(getTLID(nextLeft),PlaceMatch.LEFT);
						nextLeft = left.nextSetBit(nextLeft+1);
					} else {
						// advance right
						place.tlidSideMap.put(getTLID(nextRight),PlaceMatch.RIGHT);
						nextRight = right.nextSetBit(nextRight+1);
					}
				} else if (nextLeft >= 0) {
					// we only have answers remaining on left list
					place.tlidSideMap.put(getTLID(nextLeft),PlaceMatch.LEFT);
					nextLeft = left.nextSetBit(nextLeft+1);
				} else {
					// we only have answers remaining on right list
					place.tlidSideMap.put(getTLID(nextRight),PlaceMatch.RIGHT);
					nextRight = right.nextSetBit(nextRight+1);
				}
			}
			return place;
		} finally {
			try {
				if (td != null) {
					td.close();
				}
			} finally {
				//
			}
		}

	}

	private int getTLID(int docid) throws IOException {
		Document d= _reader.document(docid);
		return Integer.parseInt(d.get(SegmentsFields.TLID.getField()));
	}
	
	private BitSet getCityMatch(TermDocs termDocs, String city, boolean isLeft) throws IOException {
		Term term;
		if (isLeft) {
			term = new Term(SegmentsFields.PLACEL.getField(), city);
		} else {
			term = new Term(SegmentsFields.PLACER.getField(), city);
		}
		BitSet bits = new BitSet(_maxDoc);
		termDocs.seek(term);
		while (termDocs.next()) {
			bits.set(termDocs.doc());
		}
		return bits;
	}

	/**
	 * modifies: thisAndState, by clearing any bits not matching the 
	 * state.
	 * 
	 * @param thisAndState
	 * @param termDocs
	 * @param stateCode
	 * @param isLeft
	 * @throws IOException
	 */
	private BitSet cityAndStateMatch(TermDocs termDocs, String city, String stateCode, boolean isLeft) throws IOException {
		BitSet match = getCityMatch(termDocs, city, isLeft);
		if (match.isEmpty()) {
			return match;
		}
		Term term;
		if (isLeft) {
			term = new Term(SegmentsFields.STATEL.getField(), stateCode);
		} else {
			term = new Term(SegmentsFields.STATER.getField(), stateCode);
		}
		termDocs.seek(term);
		int setBit = match.nextSetBit(0);
		while (setBit >= 0 && termDocs.skipTo(setBit)) {
			int doc = termDocs.doc();
			if (doc != setBit) {
				// clear from [setBit,doc-1]
				match.clear(setBit, doc-1);
			}
			// current bit at doc stays as is
			setBit = (doc + 1 < _maxDoc ? match.nextSetBit(doc+1) : -1);
		}
		return match;
	}
	
	private BitSet getStateMatch(TermDocs termDocs, String stateCode, boolean isLeft) throws IOException {
		Term term;
		if (isLeft) {
			term = new Term(SegmentsFields.STATEL.getField(), stateCode);
		} else {
			term = new Term(SegmentsFields.STATER.getField(), stateCode);
		}
		BitSet bits = new BitSet(_maxDoc);
		termDocs.seek(term);
		while (termDocs.next()) {
			bits.set(termDocs.doc());
		}
		return bits;
	}
	
	public synchronized void close() throws IOException {
		try {
			if (_searcher != null) {
				_searcher.close();
			}
		} finally {
			try {
				if (_reader != null) {
					_reader.close();
				}
			} finally {
				_searcher = null;
				_reader = null;
			}
		}
	}
}
