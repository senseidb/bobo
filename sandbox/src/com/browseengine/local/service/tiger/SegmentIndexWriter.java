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

package com.browseengine.local.service.tiger;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

import com.browseengine.local.service.index.IndexConstants;
import com.browseengine.local.service.index.NamesFields;
import com.browseengine.local.service.index.RangesFields;
import com.browseengine.local.service.index.SegmentsFields;

/**
 * @author spackle
 *
 */
public class SegmentIndexWriter {
	private IndexWriter _segmentsWriter;
	private IndexWriter _namesWriter;
	private IndexWriter _rangesWriter;

	/**
	 * assumes we can either create, or are in append mode
	 * @param path
	 * @throws TigerDataException
	 * @throws IOException
	 */
	public SegmentIndexWriter(File path) throws TigerDataException, IOException {
		if (path.exists()) {
			if (!path.isDirectory()) {
				throw new TigerDataException("path: "+path.getAbsolutePath()+" is not a valid directory");
			}
		} else if (!path.mkdirs()) {
			throw new TigerDataException("unable to create directory at path: "+path.getAbsolutePath());
		}
		Analyzer a = new AddressAnalyzer();
		// they should either all exist, or none exist
		File sdir = new File(path, IndexConstants.SEGMENTS_INDEX);
		File ndir = new File(path, IndexConstants.NAMES_INDEX);
		File rdir = new File(path, IndexConstants.RANGES_INDEX);
		boolean create;
		if (sdir.exists()) {
			if (!(sdir.exists() && sdir.isDirectory() &&
				ndir.exists() && ndir.isDirectory() &&
				rdir.exists() && rdir.isDirectory())) {
				throw new TigerDataException("one of sdir, ndir, rdir exists without the others, or one is not a directory, at path: "+path.getAbsolutePath());
			}
			// we have a valid existing set of directories
			create = false;
		} else {
			if (ndir.exists() || rdir.exists()) {
				throw new TigerDataException("one of sdir, ndir, rdir exists without the others, at path: "+path.getAbsolutePath());
			}
			// we have a valid set of directories to create
			create = true;
		}
		_segmentsWriter = new IndexWriter(sdir, a, create);
		_namesWriter = new IndexWriter(ndir, a, create);
		_rangesWriter = new IndexWriter(rdir, a, create);
	}
	
	public void addSegment(StorableSegment segment) throws IOException {
		Document doc = new Document();
		doc.add(new Field(SegmentsFields.TLID.getField(), ""+segment.getTLID(), 
				Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
		// NO_NORMS will not tokenize the city or state abbrev, so it will need to be an exact match
		if (segment.getPlaceL() != null) {
			doc.add(new Field(SegmentsFields.PLACEL.getField(), segment.getPlaceL(),
				Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
			doc.add(new Field(SegmentsFields.TOKENIZED_PLACEL.getField(), segment.getPlaceL(),
					Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));			
		}
		if (segment.getPlaceR() != null) {
			doc.add(new Field(SegmentsFields.PLACER.getField(), segment.getPlaceR(),
					Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
			doc.add(new Field(SegmentsFields.TOKENIZED_PLACER.getField(), segment.getPlaceR(),
					Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
		}
		if (segment.getStateL() != null) {
			doc.add(new Field(SegmentsFields.STATEL.getField(), segment.getStateL(),
				Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
		}
		if (segment.getStateR() != null) {
			doc.add(new Field(SegmentsFields.STATER.getField(), segment.getStateR(),
				Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
		}
		doc.add(new Field(SegmentsFields.START_LON.getField(), 
				""+((long)segment.getStartLon()),
				Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
		doc.add(new Field(SegmentsFields.START_LAT.getField(), 
				""+((long)segment.getStartLat()),
				Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
		doc.add(new Field(SegmentsFields.END_LON.getField(), 
				""+((long)segment.getEndLon()),
				Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
		doc.add(new Field(SegmentsFields.END_LAT.getField(), 
				""+((long)segment.getEndLat()),
				Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
		this._segmentsWriter.addDocument(doc);
		
		// store the names and address ranges.
		StorableSegment.Name[] names = segment.getNames();
		if (names != null) {
			for (StorableSegment.Name name : names) {
				doc = new Document();
				doc.add(new Field(NamesFields.TLID.getField(), ""+segment.getTLID(), 
						Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
				if (name.getPrefix() != null) {
					doc.add(new Field(NamesFields.PREFIX.getField(), name.getPrefix(), 
						Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
				}
				// TODO: FOR NOW, WE SKIP OVER MISSING TYPE IN A NAME, THOSE WITH 
				// NO NAMES OR NO ADDRESS RANGES
				doc.add(new Field(NamesFields.NAME.getField(), name.getName(),
						Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
				doc.add(new Field(NamesFields.TOKENIZED_NAME.getField(), name.getName(),
						Field.Store.NO, Field.Index.TOKENIZED, Field.TermVector.NO));
				doc.add(new Field(NamesFields.TYPE.getField(), name.getType(),
						Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
				if (name.getSuffix() != null) {
					doc.add(new Field(NamesFields.SUFFIX.getField(), name.getSuffix(), 
							Field.Store.YES, Field.Index.TOKENIZED));
				}
				this._namesWriter.addDocument(doc);
			}
		}
		
		StorableSegment.NumberAndZip[] ranges;
		boolean isLeft;
		for (int i = 0; i < 2; i++) {
			isLeft = (i%2 == 0 ? true : false);
			ranges = (isLeft ? segment.getLefts() : segment.getRights());
			if (ranges != null) {
				for (StorableSegment.NumberAndZip range : ranges) {
					doc = new Document();
					doc.add(new Field(RangesFields.TLID.getField(), ""+segment.getTLID(), 
							Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
					doc.add(new Field(RangesFields.LEFT.getField(), (isLeft ? "1" : "0"), 
							Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
					doc.add(new Field(RangesFields.IS_NUMERIC.getField(), (range.isNumeric() ? "1" : "0"), 
							Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
					if (range.getFrAdd() != null) {
						doc.add(new Field(RangesFields.FROM.getField(), range.getFrAdd(), 
								Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.YES));
					}
					if (range.getToAdd() != null) {
						doc.add(new Field(RangesFields.TO.getField(), range.getToAdd(), 
								Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.YES));
					}
					if (range.getZip5() > 0) {
						doc.add(new Field(RangesFields.ZIP5.getField(), pad(range.getZip5(),5), 
								Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
					}
					this._rangesWriter.addDocument(doc);
				}
			}
		}
	}

	public synchronized void optimize() throws IOException {
		if (_segmentsWriter != null) {
			_segmentsWriter.optimize();
		}
		if (_namesWriter != null) {
			_namesWriter.optimize();
		}
		if (_rangesWriter != null) {
			_rangesWriter.optimize();
		}
	}
	
	public synchronized void close() throws IOException {
		try {
			if (_segmentsWriter != null) {
				_segmentsWriter.close();
			}
		} finally {
			try {
				if (_namesWriter != null) {
					_namesWriter.close();
				}
			} finally {
				try {
					if (_rangesWriter != null) {
						_rangesWriter.close();
					}
				} finally {
					_segmentsWriter = _namesWriter = _rangesWriter = null;
				}
			}
		}
	}
	
	/**
	 * assumes: i > 0 && ndigits > 0 && ndigits < log(Integer.MAX_INT)
	 * 
	 * @param i
	 * @param ndigits
	 * @return
	 */
	private static String pad(int i, int ndigits) {
		int val = 1;
		while (--ndigits > 0) {
			val *= 10;
		}
		StringBuilder buf = new StringBuilder();
		while (i < val) {
			// we are in a pad situation
			buf.append('0');
			val /= 10;
		}
		return buf.toString()+i;
	}
	
	public static void main(String[] argv) {
		int i;
		int ndigits = 5;
		
		i = 2215;
		System.out.println("for zip: "+i+", pad is: "+pad(i, ndigits));

		i = 94108;
		System.out.println("for zip: "+i+", pad is: "+pad(i, ndigits));

		i = 156;
		System.out.println("for zip: "+i+", pad is: "+pad(i, ndigits));
		i = 10;
		System.out.println("for zip: "+i+", pad is: "+pad(i, ndigits));
		i = 9;
		System.out.println("for zip: "+i+", pad is: "+pad(i, ndigits));
		i = 1;
		System.out.println("for zip: "+i+", pad is: "+pad(i, ndigits));
	}
}
