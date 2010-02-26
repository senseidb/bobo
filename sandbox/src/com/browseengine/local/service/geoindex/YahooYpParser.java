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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * This implementation is really ugly.
 * But it spits out YahooYpRecords, by iterating through a yahoo file.
 * 
 * @author spackle
 *
 */
public class YahooYpParser implements Iterator<YahooYpParser.YahooYpRecord>{
	private static final Logger LOGGER = Logger.getLogger(YahooYpParser.class);
	
	private Reader _reader;

	public YahooYpParser(File f) throws GeoIndexingException, IOException {
		InputStream in = null;
		try {
			if (f == null || !f.exists() || !f.isFile()) {
				throw new GeoIndexingException("bad file: "+(f != null ? f.getAbsolutePath() : null));
			}
			in = new FileInputStream(f);
			_reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		} catch (GeoIndexingException gie) {
			close();
			if (in != null) {
				in.close();
			}
			throw gie;
		} catch (IOException ioe) {
			close();
			if (in != null) {
				in.close();
			}
			throw ioe;
		} finally {
		}
	}
	
	public static class YahooYpRecord {
		public String name;
		public long phone;
		public String addrLine1;
		public String addrLine2;
	}
	
	public synchronized YahooYpRecord next() {
		try {
			if (!_advanced) {
				// force an advance, to set the _next pointer
				advance();
			}
			_advanced = false;
			return _next;
		} catch (IOException ioe) {
			LOGGER.error("failed next: "+ioe, ioe);
			_advanced = false;
			return null;
		}
	}

	private boolean _advanced = false;
	private YahooYpRecord _next;
	private boolean _totallyDone = false;

	private static final String SOURCE = "yp.yahoo.com";

	public String getSource() {
		return SOURCE;
	}
	
	private static final String WHITESPACE_MINUS_NEWLINEFEED = " \\t\\x0B\f";
	
	private boolean advance() throws IOException {
		boolean done = false;
		boolean innerAdvance = false;
		while (!done) {
			innerAdvance = innerAdvance();
			if (innerAdvance) {
				done = _next != null;
			} else {
				done = true;
			}
		}
		return innerAdvance;
	}
	
	private boolean innerAdvance() throws IOException {
		_advanced = true;
		_next = null;
		if (_totallyDone) {
			return false;
		}
		StringBuilder buf = new StringBuilder();
		char c;
		int read;
		boolean done = false;
		boolean emptyLine = true;
		int nonEmptyLinesRead = 0;
		while (!done && (read = _reader.read()) >= 0) {
			c = (char)read;
			buf.append(c);
			if (c == '\n' || c == '\r') {
				emptyLine = true;
				if (nonEmptyLinesRead >= 3) {
					done = true;
				}
			} else {
				if (emptyLine && WHITESPACE_MINUS_NEWLINEFEED.indexOf(c) < 0) {
					// we are looking at a non-whitespace character
					nonEmptyLinesRead++;
					emptyLine = false;
				}
			}
		}
		if (done) {
			// create the current
			String threeLines = buf.toString().trim();
			String[] lines = threeLines.split("[\n\r]");
			if (lines.length == 3) {
				String name = lines[0];
				Matcher m = SECOND_LINE.matcher(lines[1]);
				if (m.matches()) {
					buf = new StringBuilder();
					long phoneNum = -1L;
					try {
						phoneNum = Long.parseLong(buf.append(m.group(1)).append(m.group(2)).append(m.group(3)).toString());
					} catch (NumberFormatException nfe) {
						LOGGER.warn("skipping line '"+lines[1]+"' due to "+nfe);
					}
					String addr1 = m.group(6);
					String addr2 = lines[2].trim();
					if (addr2.endsWith("Map")) {
						addr2 = addr2.substring(0, addr2.length()-3).trim();
					}
					// send it off to geocoding.
					String addr = addr1+", "+addr2;
					
					YahooYpRecord record= new YahooYpRecord();
					record.name = name;
					record.phone = phoneNum;
					record.addrLine1 = addr1;
					record.addrLine2 = addr2;
					_next = record;
				}
			}
			return true;
		} else {
			_totallyDone = true;
			return false;
		}
	}

	private static final Pattern SECOND_LINE = Pattern.compile("\\A\\s*\\((\\d{3})\\)\\s+(\\d{3})-(\\d{4})\\s+(Web Site)?\\s*(Write a Review)?\\s*(\\d{1}.*)\\z");
	
	public void close() throws IOException {
		try {
			if (_reader != null) {
				_reader.close();
			}
		} finally {
			_reader = null;
		}
	}

	public synchronized boolean hasNext() {
		try {
		if (!_advanced) {
			//advance and mark it as advanced; subsequent calls to hasNext before calling next, will skip this part
			advance();
		}
		return _next != null;
		} catch (IOException ioe) {
			LOGGER.error("hasNext failed: "+ioe, ioe);
			return false;
		}
	}

	public void remove() {
		// TODO Auto-generated method stub
		
	}
}
