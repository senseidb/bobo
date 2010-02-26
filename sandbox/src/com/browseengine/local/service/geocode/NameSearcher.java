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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;

import com.browseengine.local.service.Address;
import com.browseengine.local.service.index.NamesFields;

/**
 * @author spackle
 *
 */
public class NameSearcher {
	private static final Logger LOGGER = Logger.getLogger(NameSearcher.class);

	private Searcher _searcher; // TLID,prefix,name,type,suffix
	private IndexReader _reader;
	private int _maxDoc;
	
	public NameSearcher(File f) throws IOException {
		try {
			_reader = IndexReader.open(f);
			_searcher = new IndexSearcher(_reader);
			_maxDoc = _reader.maxDoc();
		} catch (IOException ioe) {
			try {
				close();
			} catch (IOException ioe2) {
				LOGGER.warn("trouble closing NameSearcher, after trouble opening it", ioe2);
			}
			throw ioe;
		}
	}

	public PlaceMatch lookupName(PlaceMatch matches, String prefix, String name, String type, String suffix) throws IOException, GeoCodingException {
		if (name == null || name.length() <= 0 || type == null || type.length() <= 0) {
			throw new GeoCodingException("can't find an address with no street name or type specified");
		}
		TermDocs termDocs = null;
		try {
			// type might eventually be loaded into memory, or a browse engine field
			// also could consider loading prefix and suffix into memory
			name = GeoCodeImpl.replaceWhitespacesWithASpace(name);
			name = name.toLowerCase();
			
			type = type.toLowerCase();
			
			termDocs = _reader.termDocs();
			PlaceMatch ret = getNameAndTypeBits(matches, termDocs, name, type);
			
			return ret;
		} finally {
			try {
				if (termDocs != null) {
					termDocs.close();
				}
			} catch (IOException ioe) {
				//
			}
		}
	}
	
	private BitSet getBits(TermDocs termDocs, String field, String type) throws IOException {
		BitSet bits = new BitSet(_maxDoc);
		
		Term term = new Term(field, type);
		termDocs.seek(term);
		while (termDocs.next()) {
			bits.set(termDocs.doc());
		}
		return bits;
	}
	
	private PlaceMatch getNameAndTypeBits(PlaceMatch matches, TermDocs termDocs, String name, String type) throws IOException {
		PlaceMatch myPlaceMatch = new PlaceMatch();
		if (matches.tlidSideMap.size() <= 0) {
			return myPlaceMatch;
		}
		// we want name and type first, if they both match and they are both in matches
		BitSet typeBits = getBits(termDocs, NamesFields.TYPE.getField(), type);
		
		BitSet nameBits = getBits(termDocs, NamesFields.NAME.getField(), name);
		
		// cross reference (name ^ type) to find out if they are in matches
		// TODO: for now, ignore prefix and suffix
		if (!typeBits.isEmpty() && !nameBits.isEmpty()) {
			BitSet nameAndType = (BitSet)typeBits.clone();
			nameAndType.and(nameBits);
			if (!nameAndType.isEmpty()) {
				// check with matches
				myPlaceMatch = checkWithMatches(termDocs, nameAndType, matches);
				if (myPlaceMatch.tlidSideMap.isEmpty()) {
					// relax the type constraint
					myPlaceMatch = checkWithMatches(termDocs, nameBits, matches);
					// TODO: SOUNDEX query across name, if no matches?
					// TODO: possibly use extra name field that is tokenized to search
				}
			} else {
				// relax the type constraint
				myPlaceMatch = checkWithMatches(termDocs, nameBits, matches);
				// TODO: SOUNDEX query across name, if no matches?
				// TODO: possibly use extra name field that is tokenized to search
			}
		} else if (!nameBits.isEmpty()) {
			// we are okay ignoring type constraint, which was empty
			myPlaceMatch = checkWithMatches(termDocs, nameBits, matches);
			// TODO: SOUNDEX query across name, if no matches
			// TODO: possibly use extra name field that is tokenized to search
		} else {
			// nameBits was empty
			// TODO: SOUNDEX query across name, if no matches?
			// TODO: possibly use extra name field that is tokenized to search
		}
		
		return myPlaceMatch;
	}
	
	private PlaceMatch checkWithMatches(TermDocs termDocs, BitSet bits, PlaceMatch matches) throws IOException {
		PlaceMatch bitsAndMatches = new PlaceMatch();
		Set<Integer> set = matches.tlidSideMap.keySet();
		Integer[] keys = new Integer[set.size()];
		set.toArray(keys);
		Arrays.sort(keys);
		for (int i = 0; i < keys.length; i++) {
			int tlid = keys[i];
			Term term = new Term(NamesFields.TLID.getField(),""+tlid);
			termDocs.seek(term);
			while (termDocs.next()) {
				int docid = termDocs.doc();
				if (bits.get(docid)) {
					bitsAndMatches.tlidSideMap.put(tlid,matches.tlidSideMap.get(tlid));
				}
			}
		}
		return bitsAndMatches;
	}
	
	public Address getAddressAt(int tlid) throws IOException {
		TermDocs termDocs = null;
		try {
			Term term = new Term(NamesFields.TLID.getField(),""+tlid);
			termDocs = _reader.termDocs(term);
			if (termDocs.next()) {
				int docid = termDocs.doc();
				Document doc =  _reader.document(docid);
				return new Address(null, doc.get(NamesFields.PREFIX.getField()), 
						doc.get(NamesFields.NAME.getField()), doc.get(NamesFields.TYPE.getField()), 
						doc.get(NamesFields.SUFFIX.getField()), null, null, 
						null, -1, 
						null);
			} else {
				return null;
			}
		} finally {
			try {
				if (termDocs != null) {
					termDocs.close();
				}
			} catch (IOException ioe) {
				LOGGER.warn("trouble closing termDocs in NameSearcher.getAddressAt");
			}
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
}
