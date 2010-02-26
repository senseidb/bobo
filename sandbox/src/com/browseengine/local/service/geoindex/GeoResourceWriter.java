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

package com.browseengine.local.service.geoindex;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.browseengine.local.service.LocalResource;
import com.browseengine.local.service.index.GeoSearchFields;

/**
 * For efficiently adding Geo-coded resources to create a searchable geo
 * index.  This means longitude/latitude coordinates should be 
 * specified.  Knows how to add {@link LocalResource} instances to a 
 * geo search index.
 * 
 * @author spackle
 *
 */
public class GeoResourceWriter {
	private static final Logger LOGGER = Logger.getLogger(GeoResourceWriter.class);

	private IndexWriter _writer;
	private File _path;

	/**
	 * for testing only.
	 * 
	 * @param dir
	 * @param create
	 * @throws GeoIndexingException
	 * @throws IOException
	 */
	public GeoResourceWriter(Directory dir, boolean create) throws GeoIndexingException, IOException {
		init(dir, create);
	}
	
	public GeoResourceWriter(File path) throws GeoIndexingException, IOException {
		_path = path;
		boolean create = false;
		if (path.exists()) {
			if (!path.isDirectory()) {
				throw new GeoIndexingException("path: "+path.getAbsolutePath()+" is not a valid directory");
			}
		} else {
			create = true;
		}
		init(path, create);
	}
	
	private void init(File path, boolean create) throws IOException {
		Directory dir = FSDirectory.getDirectory(path, create);
		init(dir, create);		
	}

	private void init(Directory dir, boolean create) throws IOException {
		Analyzer a = new StandardAnalyzer();
		_writer = new IndexWriter(dir, a, create);
	}
	
	public void addResource(LocalResource resource) throws GeoIndexingException, IOException{
		if (resource == null) {
			throw new GeoIndexingException("can't index a null resource");
		}
		double lon = resource.getLongitudeDeg();
		double lat = resource.getLatitudeDeg();
		if (lon <= -180 || lon > 180 || lat < -90 || lat > 90) {
			throw new GeoIndexingException("lon/lat coordinates ("+lon+", "+lat+") are out-of-range for resource: "+resource.getName());
		}
		// name is required, searchable, and for display
		String name = resource.getName();
		if (name == null || (name = name.trim()).length() == 0) {
			throw new GeoIndexingException("can't index a resource that has no name identifier");
		}
		// address is optional; and for display-only
		String address = resource.getAddressStr();
		address = (address != null ? address.trim() : null);
		// description is optional, and searchable
		String descr = resource.getDescription();
		descr = (descr != null ? descr.trim() : null);
		// get the phone number digits
		String phone = resource.getPhoneNumber();

		Document doc = new Document();
		doc.add(new Field(GeoSearchFields.LON.getField(), ""+lon,
				Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
		doc.add(new Field(GeoSearchFields.LAT.getField(), ""+lat,
				Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
		doc.add(new Field(GeoSearchFields.NAME.getField(), name, 
				Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
		if (address != null && address.length() > 0) {
			doc.add(new Field(GeoSearchFields.ADDRESS.getField(), address,
					Field.Store.YES, Field.Index.NO, Field.TermVector.NO));
		}
		if (descr != null && descr.length() > 0) {
			doc.add(new Field(GeoSearchFields.DESCRIPTION.getField(), address, 
					Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO));
		}
		if (phone != null && phone.length() > 0) {
			// untokenized, so that we can search on 415555*, for example, to search within an exchange.
			doc.add(new Field(GeoSearchFields.PHONE.getField(), phone,
					Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
		}
		if (_writer != null) {
			_writer.addDocument(doc);
		} else {
			throw new GeoIndexingException("attempt to add a resource to a closed "+GeoResourceWriter.class.getName());
		}
	}

	private static class LonDocid implements Comparable<LonDocid>{
		private double lon;
		private int docid;
		public LonDocid(int docid, double lon) {
			this.docid = docid;
			this.lon = lon;
		}
		public int compareTo(LonDocid arg0) {
			int val = new Double(lon).compareTo(arg0.lon);
			if (0 == val) {
				val = new Integer(docid).compareTo(arg0.docid);
			}
			return val;
		}
	}
	
	public synchronized void optimize() throws IOException, GeoIndexingException {
		if (_writer != null) {
			if (_path != null) {
				_writer.optimize();
				File path2 = new File(_path.getParentFile(), _path.getName()+".tmp");
				_writer.close();
				_writer = null;
				if (_path.renameTo(path2)) {
					IndexReader reader = null;
					TermEnum termEnum = null;
					TermDocs termDocs = null;
					try {
						reader = IndexReader.open(path2);
						int maxDoc = reader.maxDoc();
						if (maxDoc <= 0) {
							throw new GeoIndexingException("can't optimize an index with "+maxDoc+" docs");
						}
						LonDocid[] lonDocids = new LonDocid[maxDoc];
						String fld = GeoSearchFields.LON.getField().intern();
						Term term = new Term(fld, "");
						termEnum = reader.terms(term);
						termDocs = reader.termDocs();
						while ((term = termEnum.term()) != null && term.field() == fld) {
							double lon = Double.parseDouble(term.text());
							termDocs.seek(term);
							while (termDocs.next()) {
								int docid = termDocs.doc();
								lonDocids[docid] = new LonDocid(docid,lon);								
							}
							termEnum.next();
						}
						termDocs.close();
						termDocs = null;
						termEnum.close();
						termEnum = null;
						Arrays.sort(lonDocids);
						init(_path, true);
						for (int i = 0; i < lonDocids.length; i++) {
							int docid = lonDocids[i].docid;
							Document doc = reader.document(docid);
							// all fields are stored
							String name = doc.get(GeoSearchFields.NAME.getField());
							String description = doc.get(GeoSearchFields.DESCRIPTION.getField());
							String addressStr = doc.get(GeoSearchFields.ADDRESS.getField());
							String phoneStr = doc.get(GeoSearchFields.PHONE.getField());
							long phoneNumber = LocalResource.NO_PHONE_NUMBER;
							if (phoneStr != null && phoneStr.length() > 0) {
								phoneNumber = Long.parseLong(phoneStr);
							}
							String lonStr = doc.get(GeoSearchFields.LON.getField());
							double lon = Double.parseDouble(lonStr);
							String latStr = doc.get(GeoSearchFields.LAT.getField());
							double lat = Double.parseDouble(latStr);
							
							LocalResource resource = new LocalResource(name, 
									description, addressStr, phoneNumber, lon, lat);
							addResource(resource);
						}
						reader.close();
						reader = null;
						
						_writer.optimize();

						LOGGER.info("successfully completed optimization of index at "+_path.getAbsolutePath());
					} finally {
						try {
							// erase the tmp dir
							recursiveDelete(path2);
						} finally {
							try {
								if (reader != null) {
									reader.close();
								}
							} finally {
								try {
									if (termEnum != null) {
										termEnum.close();
									}
								} finally {
									try {
										if (termDocs != null) {
											termDocs.close();
										}
									} finally {
										reader = null;
										termDocs = null;
										termEnum = null;
									}
								}
							}
						}
					}
				} else {
					init(_path, false);
					throw new GeoIndexingException("trouble doing the rename from "+_path.getAbsolutePath()+" to "+path2.getAbsolutePath()+"; check permissions");
				}
			} else {
				_writer.optimize();
			}
		} else {
			throw new GeoIndexingException("attempt to optimize a closed "+GeoResourceWriter.class.getName());
		}
	}
	
	private static boolean recursiveDelete(File f) throws IOException {
		// try to delete everything at f, and below if it's a directory
		boolean success = true;
		if (f != null && f.exists()) {
			if (f.isDirectory()) {
				File[] fs = f.listFiles();
				for (int i = 0; i < fs.length; i++) {
					success &= recursiveDelete(fs[i]);
				}
			}
			success &= f.delete();
		}
		return success;
	}
	
	public synchronized void close() throws IOException {
		try {
			if (_writer != null) {
				_writer.close();
			}
		} finally {
			_writer = null;
		}
	}
	
	
}
