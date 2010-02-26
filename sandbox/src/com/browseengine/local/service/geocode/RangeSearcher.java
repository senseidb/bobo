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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;

import com.browseengine.local.service.index.RangesFields;

/**
 * @author spackle
 *
 */
public class RangeSearcher {
	private static final Logger LOGGER = Logger.getLogger(RangeSearcher.class);

	private Searcher _searcher; // TLID,isLeft,isNumeric,from,to,zip5
	private IndexReader _reader;
	private int _maxDoc;
	private BitSet _isLeft;
	private BitSet _isNumeric;
	
	public RangeSearcher(File f) throws IOException {
		try {
			_reader = IndexReader.open(f);
			_searcher = new IndexSearcher(_reader);
			_maxDoc = _reader.maxDoc();
			_isLeft= loadBitField(RangesFields.LEFT.getField());
			_isNumeric = loadBitField(RangesFields.IS_NUMERIC.getField());
		} catch (IOException ioe) {
			try {
				close();
			} catch (IOException ioe2) {
				LOGGER.warn("trouble closing NameSearcher, after trouble opening it", ioe2);
			}
			throw ioe;
		}
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

	private BitSet loadBitField(String fieldName) throws IOException {
		Term term = new Term(fieldName, "1");
		TermDocs termDocs = null;
		try {
			BitSet bits = new BitSet(_maxDoc);
			termDocs = _reader.termDocs(term);
			while (termDocs.next()) {
				bits.set(termDocs.doc());
			}
			return bits;
		} finally {
			if (termDocs != null) {
				termDocs.close();
			}
		}
	}
	
	private int getZip5(Document doc) {
		if (doc != null) {
			String tmp = doc.get(RangesFields.ZIP5.getField());
			if (tmp != null) {
				Matcher m = IS_NUMERIC.matcher(tmp);
				if (m.matches()) {
					return Integer.parseInt(tmp);
				}
			}
		}
		return -1;
	}
	
	private int getTLID(int docid) throws IOException {
		Document doc = _reader.document(docid);
		return Integer.parseInt(doc.get(RangesFields.TLID.getField()));
	}
	
	/**
	 * assumes: matches.tlidSideMap.size() > 0.
	 * 
	 * @param matches
	 * @param streetNumber
	 * @param zip5
	 * @return
	 * @throws IOException
	 */
	public RangeMatch narrowToRange(PlaceMatch matches, String streetNumber, int zip5) throws IOException, GeoCodingException {
		TermDocs termDocs = null;
		try {
			termDocs = _reader.termDocs();
			if (streetNumber == null || streetNumber.length() <= 0) {
				// no street number was specified as part of the input
				return takeAnyRange(termDocs, matches, zip5);
			} else {
				return findBestRange(termDocs, matches, streetNumber, zip5);
			}
		} finally {
			try {
				if (termDocs != null) {
					termDocs.close();
				}
			} catch (IOException ioe) {
				LOGGER.warn("trouble closing termdocs at end of narrow to range: "+ioe, ioe);
			}
		}
	}

	/**
	 * we don't support numeric addresses beyond 999999999.
	 */
	public static final Pattern IS_NUMERIC = Pattern.compile("\\A(\\d{1,9})\\z");
	public static final Pattern HAS_NUMERIC = Pattern.compile("(\\d{1,9})");
	
	private RangeMatch findBestRange(TermDocs termDocs, PlaceMatch matches, 
			String streetNumber, int zip5) throws IOException, GeoCodingException {
		boolean isNumeric = false;
		int target = -1;
		Matcher m = IS_NUMERIC.matcher(streetNumber);
		if (m.matches()) {
			isNumeric = true;
			target = Integer.parseInt(streetNumber);
		}
		if (isNumeric) {
			// try to do it numerically 
			Integer[] tlids = getSortedTlids(matches);
			for (Integer tlid : tlids) {
				Term term = new Term(RangesFields.TLID.getField(), tlid.toString());
				termDocs.seek(term);
				while (termDocs.next()) {
					int docid = termDocs.doc();
					if (_isNumeric.get(docid)) {
						TermFreqVector frVec = _reader.getTermFreqVector(docid, RangesFields.FROM.getField());
						TermFreqVector toVec = _reader.getTermFreqVector(docid, RangesFields.TO.getField());
						if (frVec != null && frVec.size() > 0 && toVec != null && toVec.size() > 0) {
							int fr = Integer.parseInt(frVec.getTerms()[0]);
							int to = Integer.parseInt(toVec.getTerms()[0]);
							if (fr < to) {
								if (fr <= target && target <= to) {
									// hit!
									Document doc = _reader.document(docid);
									RangeMatch match = new RangeMatch();
									match.lrboth = (_isLeft.get(docid) ? PlaceMatch.LEFT : PlaceMatch.RIGHT);
									match.number = ""+target;
									match.tlid = tlid;
									match.toPercent = (fr < to ? ((float)target-fr)/((float)to-fr) : 0f);
									match.zip5 = getZip5(doc);
									return match;
								}
							} else {
								if (to <= target && target <= fr) {
									// hit!
									Document doc = _reader.document(docid);
									RangeMatch match = new RangeMatch();
									match.lrboth = (_isLeft.get(docid) ? PlaceMatch.LEFT : PlaceMatch.RIGHT);
									match.number = ""+target;
									match.tlid = tlid;
									match.toPercent = (fr > to ? ((float)target-to)/((float)fr-to) : 0f);
									match.zip5 = getZip5(doc);
									return match;
								}
							}
						}
					}
				}
			}
		}
		// pure numeric search didn't work--try to match the numeric _portion_
		
		// TODO: what to do if we're about to return null?
		// for now, just return any range
		// TODO: in the future, keep track of which of the above was the 
		// closest numerically, and return that one instead.
		// TODO: we probably want an indicator to the client, that we are taking a range 
		// that we didn't necessarily find exactly
		return takeAnyRange(termDocs, matches, zip5);
	}
	
	private Integer[] merge(Integer[] tlids, PlaceMatch matches, 
			Integer[] zipTlids, PlaceMatch zipMatches) {
		List<Integer> retList = new ArrayList<Integer>();
		int i = 0;
		int j = 0;
		while (i < tlids.length && j < zipTlids.length) {
			if (tlids[i] == zipTlids[j]) {
				short rlboth = matches.tlidSideMap.get(tlids[i]);
				short zipRlboth = zipMatches.tlidSideMap.get(zipTlids[j]);
				if (rlboth == PlaceMatch.BOTH ||
					zipRlboth == PlaceMatch.BOTH ||
					rlboth == zipRlboth) {
					retList.add(tlids[i]);
				}
				i++;
				j++;
			} else if (tlids[i] < zipTlids[j]) {
				// advance i
				i++;
			} else {
				// advance j
				j++;
			}
		}
		if (retList.size() > 0) {
			Integer[] ret= new Integer[retList.size()];
			retList.toArray(ret);
			return ret;
		} else {
			return null;
		}
	}

	private Integer[] getSortedTlids(PlaceMatch matches) {
		Set<Integer> keys = matches.tlidSideMap.keySet();
		Integer[] tlids = new Integer[keys.size()];
		keys.toArray(tlids);
		Arrays.sort(tlids);
		return tlids;
	}
	
	/**
	 * no range specified, take the lowest.
	 * 
	 * @param termDocs
	 * @param matches
	 * @param zip5
	 * @return
	 * @throws IOException
	 * @throws GeoCodingException
	 */
	private RangeMatch takeAnyRange(TermDocs termDocs, PlaceMatch matches, int zip5) throws IOException, GeoCodingException {
		Integer[] tlids = getSortedTlids(matches);

		// match with zip also if possible
		if (zip5 > 0 && zip5 <= 99999) {
			PlaceMatch zipMatches= lookupZip(zip5);
			if (zipMatches != null && zipMatches.tlidSideMap.size() > 0) {
				Integer[] zipTlids = getSortedTlids(zipMatches);
			
				Integer[] tmp = merge(tlids, matches, zipTlids, zipMatches);
				tlids = (tmp != null && tmp.length > 0 ? tmp : tlids);
			}
		}
		
		int minNumeric = Integer.MAX_VALUE;
		int minDocid = -1;
		double minLon = Double.MAX_VALUE;
		double minLat = Double.MAX_VALUE;
		boolean foundNumeric = false; // numeric is preferred
		boolean from = true;
		String minAlpha = new String("zzz");
		// the desired result is the lowest numeric address
		// failing that, the desired result is the lowest alpha address
		for (int i = 0; i < tlids.length; i++) {
			Term term = new Term(RangesFields.TLID.getField(), ""+tlids[i]);
			termDocs.seek(term);
			while (termDocs.next()) {
				int docid = termDocs.doc();
				TermFreqVector frVec = _reader.getTermFreqVector(docid, RangesFields.FROM.getField());
				TermFreqVector toVec = _reader.getTermFreqVector(docid, RangesFields.TO.getField());
				if (frVec != null && frVec.size() > 0 && toVec != null && toVec.size() > 0) {
					if (_isNumeric.get(docid)) {
						int fr = Integer.parseInt(frVec.getTerms()[0]);
						int to = Integer.parseInt(toVec.getTerms()[0]);
						if (fr < to) {
							if (fr < minNumeric) {
								minNumeric = fr;
								minDocid = docid;
								from = true;
								foundNumeric = true;
							}
						} else {
							if (to < minNumeric) {
								minNumeric = to;
								minDocid = docid;
								from = false;
								foundNumeric = true;
							}
						}
					} else if (!foundNumeric) {
						String fr = frVec.getTerms()[0];
						String to = toVec.getTerms()[0];
						if (fr.compareTo(to) < 0) {
							if (fr.compareTo(minAlpha) < 0) {
								minAlpha = fr;
								minDocid = docid;
								from = true;
							}
						} else {
							if (to.compareTo(minAlpha) < 0) {
								minAlpha = to;
								minDocid = docid;
								from = false;
							}
						}
					}
				}
			}
			if (minDocid >= 0) {
				// we have a winner!
				Document doc = _reader.document(minDocid);
				RangeMatch match = new RangeMatch();
				match.tlid = tlids[i];
				match.lrboth = (_isLeft.get(minDocid) ? PlaceMatch.LEFT : PlaceMatch.RIGHT);//matches.tlidSideMap.get(match.tlid);
				match.number = (foundNumeric ? ""+minNumeric : minAlpha);
				 // we will be interpolating, so we should go a tiny bit from the intersetion
				match.toPercent = (from ? 0.01f : 0.99f);
				match.zip5 = getZip5(doc);
				return match;
			}
		}

		// TODO: what to do if we're about to return null?
		// repeat without a zip5
		if (zip5 > 0) {
			return takeAnyRange(termDocs,matches,-1);
		} else {
			return null;
		}
	}
	
	public PlaceMatch lookupZip(int zip5) throws GeoCodingException, IOException {
		if (zip5 <= 0 || zip5 > 99999) {
			LOGGER.warn("can't lookup a zip code out of range: "+zip5+"; returning empty results");
			return new PlaceMatch();
		}
		TermDocs termDocs = null;
		try {
			PlaceMatch match = new PlaceMatch();
			Term term = new Term(RangesFields.ZIP5.getField(), ""+zip5);
			termDocs = _reader.termDocs(term);
			while (termDocs.next()) {
				int docid = termDocs.doc();
				boolean isLeft = _isLeft.get(docid);
				int tlid = getTLID(docid);
				Short val = match.tlidSideMap.get(tlid);
				if (val != null) {
					short lrboth = PlaceMatch.getLRBoth(val);
					if (isLeft) {
						if (lrboth == PlaceMatch.RIGHT) {
							val = PlaceMatch.setLRBoth(val,PlaceMatch.BOTH);
							match.tlidSideMap.put(tlid,val);
						}
					} else if (lrboth == PlaceMatch.LEFT) {
						val = PlaceMatch.setLRBoth(val,PlaceMatch.BOTH);
						match.tlidSideMap.put(tlid,val);
					}
				} else {
					val = PlaceMatch.setLRBoth((short)0, (isLeft ? PlaceMatch.LEFT : PlaceMatch.RIGHT));
					match.tlidSideMap.put(tlid,val);
				}
			}
			return match;
		} finally {
			try {
				if (termDocs != null) {
					termDocs.close();
				}
			} catch (IOException ioe) {
				LOGGER.warn("trouble closing term docs during lookup zip: "+ioe, ioe);
			}
		}
	}
}
