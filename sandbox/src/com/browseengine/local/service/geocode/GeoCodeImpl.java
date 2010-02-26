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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.browseengine.local.service.Address;
import com.browseengine.local.service.LocatedAddress;
import com.browseengine.local.service.index.IndexConstants;
import com.browseengine.local.service.tiger.TigerDataException;
import com.browseengine.local.service.tiger.TigerParseException;

/**
 * @author spackle
 *
 */
public class GeoCodeImpl implements GeoCode {
	private static final Logger LOGGER = Logger.getLogger(GeoCodeImpl.class);
	
	private SegmentSearcher _segments;
	
	private RangeSearcher _ranges;
	private NameSearcher _names;
	
	public GeoCodeImpl(File geocodeDir) throws IOException, TigerDataException {
		File fs = new File(geocodeDir,IndexConstants.SEGMENTS_INDEX);
		File fr = new File(geocodeDir,IndexConstants.RANGES_INDEX);
		File fn = new File(geocodeDir,IndexConstants.NAMES_INDEX);
		if (!fs.exists() || !fs.isDirectory() ||
			!fr.exists() || !fr.isDirectory() ||
			!fn.exists() || !fn.isDirectory()) {
			throw new TigerDataException("one of the sub-indexes is not valid: segments: "+fs.getAbsolutePath()+", ranges: "+fr.getAbsolutePath()+", names: "+fn.getAbsolutePath());
		}
		try {
			_segments= new SegmentSearcher(fs);
			_ranges = new RangeSearcher(fr);
			_names = new NameSearcher(fn);
		} catch (IOException ioe) {
			try {
				close();
			} catch (GeoCodingException ioe2) {
				// do nothing; already logged by close();
			}
			throw ioe;
		}
	}
	
	public synchronized void close() throws GeoCodingException {
		IOException toThrow = null;
		try {
			if (_segments != null) {
				_segments.close();
			}
		} catch (IOException ioe2) {
			toThrow = (toThrow == null ? ioe2 : toThrow);
			LOGGER.warn("trouble closing segments searcher", ioe2);
		} finally {
			try {
				if (_ranges != null) {
					_ranges.close();
				}
			} catch (IOException ioe2) {
				toThrow = (toThrow == null ? ioe2 : toThrow);
				LOGGER.warn("trouble closing ranges searcher", ioe2);						
			} finally {
				try {
					if (_names != null) {
						_names.close();
					}
				} catch (IOException ioe2) {
					toThrow = (toThrow == null ? ioe2 : toThrow);
					LOGGER.warn("trouble closing names searcher", ioe2);
				} finally {
					_segments = null;
					_ranges = null;
					_names = null;
				}
			}
		}
		if (toThrow != null) {
			throw new GeoCodingException(toThrow.toString(), toThrow);
		}
	}
	
	private static final Pattern ZIP_ANYWHERE = Pattern.compile("\\b(\\d{5})(-\\d{4})?\\b");
	private static final String DEFAULT_COUNTRY = "U.S.";
	private static final Pattern BEGINNING = Pattern.compile("\\A(\\d+)\\s+([\\p{Alpha}\\s]*)\\s+(\\p{Alpha}+)\\.?\\z");
	private static final Pattern DIRECTION = Pattern.compile("\\A(\\b[NSEWO][EWOX]?\\b)?\\s*([\\w\\s\\p{Punct}]+)\\z");
	private static final Pattern CITY_STATE_ZIP = Pattern.compile("\\A(\\b[^\\d^,]*\\b),?\\s*(\\w{2})\\s*((\\d{5})(-\\d{4})?)?");
	// th, rd, nd, st
	private static final Pattern NTH_PATTERN = Pattern.compile("(\\b\\d{1,3})([trns][hdt])\\b");

	private static final String[] NTHS = {
		"Zeroth",
		"First",
		"Second",
		"Third",
		"Fourth",
		"Fifth",
		"Sixth",
		"Seventh",
		"Eighth",
		"Ninth",
		"Tenth",
		"Eleventh",
		"Twelfth",
		"Thirteenth",
		"Fourteenth",
		"Fifteenth",
		"Sixteenth",
		"Seventeenth",
		"Eighteenth",
		"Nineteenth",
		"Twentieth",
	};
	
	private String getNth(String nstr) {
		int n = Integer.parseInt(nstr);
		if (n <= NTHS.length) {
			return NTHS[n];
		}
		return null;
	}
	
	/**
	 * does a rudimentary parsing, which assumes there are commas, and 
	 * we have a complete address.
	 */
	public Address parseAddress(String addressStr) throws TigerParseException {
		if (addressStr == null) {
			throw new TigerParseException("no way to parse a null address");
		}
		addressStr = addressStr.trim();
		Matcher m = NTH_PATTERN.matcher(addressStr);
		if (m.find(0)) {
			int idx = 0;
			String nth;
			StringBuilder buf = new StringBuilder();
			do {
				buf.append(addressStr.substring(idx, m.start()));
				nth = m.group(1);
				nth = getNth(nth);
				if (nth == null) {
					buf.append(m.group());
				} else {
					buf.append(nth);
				}
				idx = m.end();
			} while (idx < addressStr.length() && m.find(idx));
			if (idx < addressStr.length()) {
				buf.append(addressStr.substring(idx,addressStr.length()));
			}
			addressStr = buf.toString();
		}
		int idx = addressStr.indexOf(',');
		if (idx >= 0 && idx+1 < addressStr.length()) {
			String beginning = addressStr.substring(0,idx).trim();
			m = BEGINNING.matcher(beginning);
			if (m.find()) {
				String number = m.group(1);
				beginning = m.group(2);
				String type = m.group(3);
				m = DIRECTION.matcher(beginning);
				String prefix;
				String name;
				if (m.find()) {
					prefix = m.group(1);
					name = beginning.substring(m.start(2)).trim();
				} else {
					prefix = null;
					name = beginning.trim();
				}
				String suffix = null;
				addressStr = addressStr.substring(idx+1).trim();
				m = CITY_STATE_ZIP.matcher(addressStr);
				if (m.find(0)) {
					String city = m.group(1);
					String state = m.group(2);
					// 3 is whole 9 digit zip with '-'
					String zip = m.group(4);
					// 5 is just the last 4 digits
					int zip5 = Address.NO_ZIP5;
					if (zip != null && zip.length() > 0) {
						zip5 = Integer.parseInt(zip);
					}
					Address addr = new Address(
							number, prefix, 
							name, type, 
							suffix, null, city, 
							state, zip5, 
							DEFAULT_COUNTRY
							);
					return addr;
				}
				/*
				idx = addressStr.indexOf(',');
				if (idx >= 0 && idx+1 < addressStr.length()) {
					String city = addressStr.substring(0,idx).trim();
					addressStr = addressStr.substring(idx+1).trim();
					if (addressStr.length() > 5) {
						String state = addressStr.substring(0,2);
						String zip = addressStr.substring(2).trim();
						m = ZIP_ANYWHERE.matcher(zip);
						if (state.length() == 2 && m.find()) {
							int zip5 = Integer.parseInt(m.group(1));
							Address addr = new Address(
									number, prefix, 
									name, type, 
									suffix, null, city, 
									state, zip5, 
									DEFAULT_COUNTRY
									);
							return addr;
						}
					}
				}
				*/
			}
		}
		return null;
	}
	
	public LocatedAddress lookupAddress(Address address) throws IOException, GeoCodingException {
		// we need a few searchers
		if (address == null) {
			throw new GeoCodingException("no way to look up a null address");
		}
		if (address.getCountry() == null) {
			throw new GeoCodingException("no way to look up an address with no country");
		}
		if (!address.getCountry().equalsIgnoreCase(DEFAULT_COUNTRY)) {
			String country = address.getCountry();
			// try one of the accepted forms
			if (!(country.equalsIgnoreCase("US") ||
				  country.equalsIgnoreCase("USA") ||
				  country.equalsIgnoreCase("U.S.") || 
				  country.equalsIgnoreCase("U.S.A.") ||
				  country.equalsIgnoreCase("United States") || 
				  country.equalsIgnoreCase("United States of America"))) {
				throw new GeoCodingException("unsupported country :"+address.getCountry()+
					"; currently can only geocode addresses in country: "+DEFAULT_COUNTRY);
			}
		}
		
		// this look-up only understands either zip code, or (city, state)
		// assume: city, state should dominate; zip codes are often wrong
		String city;
		String state;
		PlaceMatch match = new PlaceMatch();
		if ((city = address.getCity()) != null && city.length() > 0 && 
			(state = address.getState()) != null && state.length() > 0) {
			// city, state are already trimmed
			match = _segments.lookupPlace(city, state);
			if (match.tlidSideMap.size() > 0) {
				// check names
				match = _names.lookupName(match, 
						address.getStreetPrefix(), address.getStreetName(), 
						address.getStreetType(), address.getStreetSuffix());
			}
		}
		// if match is null, we should try it via zip code instead
		if (match.tlidSideMap.size() <= 0 && address.getZip5() > 0) {
			match = _ranges.lookupZip(address.getZip5());
			if (match.tlidSideMap.size() > 0) {
				// check names
				match = _names.lookupName(match, 
						address.getStreetPrefix(), address.getStreetName(),
						address.getStreetType(), address.getStreetSuffix());
			}
		}
		// is match is not empty, then we will try to zone in on the true match!
		if (match.tlidSideMap.size() > 0) {
			RangeMatch fullmatch = _ranges.narrowToRange(match, address.getNumber(), address.getZip5());
			if (fullmatch != null) {
				LocatedAddress resource = constructLocalResource(fullmatch);
				if (resource != null) {
					return resource;
				}
			}
		}
		// TODO: if we have no results, and we got here via the city, state, we should probably try zip.
		throw new GeoCodingException("unable to find/properly geocode the input address: "+address);
	}

	private LocatedAddress constructLocalResource(RangeMatch rangeMatch) throws IOException {
		Address fromName = _names.getAddressAt(rangeMatch.tlid);
		if (fromName != null) {
			Address address = new Address(rangeMatch.number, 
					fromName.getStreetPrefix(), 
					fromName.getStreetName(), fromName.getStreetType(), 
					fromName.getStreetSuffix(), null, null,
					null, rangeMatch.zip5, 
					DEFAULT_COUNTRY);
			return _segments.constructLocalResource(rangeMatch, address);
		} else {
			return null;
		}
	}
	
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	static String replaceWhitespacesWithASpace(String in) {
		Matcher m = WHITESPACE.matcher(in);
		return m.replaceAll(" ");
	}
	
	/**
	 * requires: at "tmpidx", a geocode index that includes 
	 * tgr06115.zip.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			File f = new File("tmpidx");
			GeoCode geocoder = new GeoCodeImpl(f);
			String addrStr;
			Address addr;
			LocatedAddress resource;
			
			addrStr = "5999 Lindhurst Ave., Marysville, CA 95901";
			addr = geocoder.parseAddress(addrStr);
			LOGGER.info("address: "+addrStr+" parsed into: "+addr);
			resource = geocoder.lookupAddress(addr);
			LOGGER.info("geocoder found address: "+resource);
			
			addrStr = "5999 Lindhurst Ave., Linda, CA 95901";
			addr = geocoder.parseAddress(addrStr);
			LOGGER.info("address: "+addrStr+" parsed into: "+addr);
			resource = geocoder.lookupAddress(addr);
			LOGGER.info("geocoder found address: "+resource);
			
			addrStr = "5997 Lindhurst Ave., Linda, CA 95901";
			addr = geocoder.parseAddress(addrStr);
			LOGGER.info("address: "+addrStr+" parsed into: "+addr);
			resource = geocoder.lookupAddress(addr);
			LOGGER.info("geocoder found address: "+resource);

			addrStr = "1608 N Beale Rd., Linda, CA 95901";
			addr = geocoder.parseAddress(addrStr);
			resource = geocoder.lookupAddress(addr);
			LOGGER.info("geocoder found address: "+resource);

			addrStr = "1608 Beale Rd., Linda, CA 95901";
			addr = geocoder.parseAddress(addrStr);
			resource = geocoder.lookupAddress(addr);
			LOGGER.info("geocoder found address: "+resource);

			addrStr = "1665 Beale Rd., Linda, CA 95901";
			addr = geocoder.parseAddress(addrStr);
			resource = geocoder.lookupAddress(addr);
			LOGGER.info("geocoder found address: "+resource);

		} catch (Throwable t) {
			LOGGER.error(t.toString(), t);
		}
	}
}
