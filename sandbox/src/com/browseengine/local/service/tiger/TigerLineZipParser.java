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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;

/**
 * @author spackle
 *
 */
public class TigerLineZipParser {
	private static final Logger LOGGER = Logger.getLogger(TigerLineZipParser.class);
	
	private InputStream is;
	private ZipInputStream zin;
	private File file;

	public TigerLineZipParser(File tgrXXYYYzip) throws IOException {
		boolean success = false;
		try {
			is = new FileInputStream(tgrXXYYYzip);
			zin = new ZipInputStream(is);
			file = tgrXXYYYzip;
			success= true;
		} finally {
			if (!success) {
				close();
			}
		}
	}
	
	private static final String ENC = "ISO-8859-1"; // as per Tiger/LINE documentation
	private static final long MAX_BYTES = 50L*1000*1000; // 50 MB compressed max for 1 file
	
	private static final Pattern NEW_LINE = Pattern.compile("\n");//Pattern.compile("(\\\\n)");
	
	/**
	 * *.RTC file: list of place names that are pointed to by *.RT1 
	 * records.
	 * 
	 * @param r
	 * @return
	 * @throws IOException
	 * @throws TigerParseException
	 */
	private List<PlaceName> parseRTC(Reader r) throws IOException, TigerParseException {
		String s= dumpReader(r);
		Matcher m = NEW_LINE.matcher(s);
		try {
			List<PlaceName> list = new ArrayList<PlaceName>();
			int idx = 0;
			while (m.find(idx)) {
				String line= s.substring(idx, m.start());
				if (line.charAt(0) == 'C') {
					int version = Integer.parseInt(line.substring(1,5).trim());
					checkVersion(version, line);
					String tmp = line.substring(14,19).trim();
					if (tmp.length()>0) {
						int fipsPub55_3 = Integer.parseInt(tmp);
						tmp = line.substring(62,122).trim();
						if (tmp.length()>0) {
							PlaceName place = new PlaceName(
									fipsPub55_3,
									tmp
									);
							list.add(place);
						} else {
							LOGGER.info("skiping over record in RTC file since there's no name for it: "+line);
						}
					} else {
						LOGGER.debug("skipping over record in RTC file since there's no FIPS PUB 55-3 Code: "+line);
					}
				} else {
					LOGGER.warn("skipping record in RTC file b/c its of the wrong type "+line.charAt(0)+": "+line);
				}
				idx = m.end();
			}
			return list;
		} catch (NumberFormatException nfe) {
			throw new TigerParseException("bad number format in Tiger/LINE file: "+nfe, nfe);
		}
	}
	
	/**
	 * *.RT6 file: additional address ranges, identified by TLID.
	 * 
	 * @param r
	 * @return
	 * @throws IOException
	 * @throws TigerParseException
	 */
	private List<AddtlAddressRange> parseRT6(Reader r) throws IOException, TigerParseException {
		String s= dumpReader(r);
		Matcher m = NEW_LINE.matcher(s);
		try {
			List<AddtlAddressRange> list = new ArrayList<AddtlAddressRange>();
			int idx = 0;
			while (m.find(idx)) {
				String line = s.substring(idx, m.start());
				if (line.charAt(0) == '6') {
					int version = Integer.parseInt(line.substring(1,5).trim());
					checkVersion(version, line);
					int TLID = Integer.parseInt(line.substring(5,15).trim());
					int recordSequenceNumber = Integer.parseInt(line.substring(15,18).trim());
					String frAddL = line.substring(18,29).trim();
					String toAddL = line.substring(29,40).trim();
					String frAddR = line.substring(40,51).trim();
					String toAddR = line.substring(51,62).trim();
					String tmp;
					tmp = line.substring(66,71).trim();
					int zip5left = -1;
					if (tmp.length()>0) {
						zip5left = Integer.parseInt(tmp);
					}
					tmp = line.substring(71,76).trim();
					int zip5right = -1;
					if (tmp.length()>0) {
						zip5right = Integer.parseInt(tmp);
					}
					AddtlAddressRange addressRange = new AddtlAddressRange(
							TLID,
							recordSequenceNumber,
							frAddL,
							toAddL,
							frAddR,
							toAddR,
							zip5left,
							zip5right
							);
					list.add(addressRange);
				} else {
					LOGGER.warn("skipping record in RT6 file b/c its of the wrong type "+line.charAt(0)+": "+line);
				}
				
				idx = m.end();
			}
			return list;
		} catch (NumberFormatException nfe) {
			throw new TigerParseException("bad number format in Tiger/LINE file: "+nfe, nfe);
		}
	}
	
	/**
	 * *.RT5 file: actual street names, with FEAT identifier for each
	 * 
	 * @param r
	 * @return
	 * @throws IOException
	 * @throws TigerParseException
	 */
	private List<AddtlFeatureName> parseRT5(Reader r) throws IOException, TigerParseException {
		String s = dumpReader(r);
		Matcher m = NEW_LINE.matcher(s);
		try {
			List<AddtlFeatureName> list = new ArrayList<AddtlFeatureName>();
			int idx = 0;
			int cnt = 0;
			while (m.find(idx)) {
				cnt++;
				String line = s.substring(idx, m.start());
				if (line.charAt(0) == '5') {
					int version = Integer.parseInt(line.substring(1,5).trim());
					checkVersion(version, line);
					int FEAT = Integer.parseInt(line.substring(10,18).trim());
					String prefix = line.substring(18,20).trim();
					String name = line.substring(20,50).trim();
					String type = line.substring(50,54).trim();
					String suffix = line.substring(55,56).trim();
					AddtlFeatureName cname = new 
					AddtlFeatureName(
							FEAT,
							prefix,
							name,
							type,
							suffix
							);
					list.add(cname);
				} else {
					LOGGER.warn("skipping record in RT5 file b/c its of the wrong type "+line.charAt(0)+": "+line);
				}
				
				idx = m.end();
			}
			
			return list;
			
		} catch (NumberFormatException nfe) {
			throw new TigerParseException("bad number format in Tiger/LINE file: "+nfe, nfe);
		}
	}

	/**
	 * *.RT4 file: mapping from tlid segments to sets of additional 
	 * "feature names" (street names): these are essentially 
	 * maps from *.RT1 file records into one or more *.RT5 file records.
	 * 
	 * @param r
	 * @return
	 * @throws IOException
	 * @throws TigerParseException
	 */
	private List<AddtlFeatureNameSet> parseRT4(Reader r) throws IOException, TigerParseException {
		String s = dumpReader(r);
		Matcher m = NEW_LINE.matcher(s);
		int idx = 0;
		try {
			List<AddtlFeatureNameSet> list = new ArrayList<AddtlFeatureNameSet>();
			while (m.find(idx)) {
				String line = s.substring(idx, m.start());
				if (line.charAt(0) == '4') {
					int version = Integer.parseInt(line.substring(1,5).trim());
					checkVersion(version, line);
					int TLID = Integer.parseInt(line.substring(5,15).trim());
					int recordSequenceNumber = Integer.parseInt(line.substring(15,18).trim());
					List<Integer> alts = new ArrayList<Integer>();
					alts.add(Integer.parseInt(line.substring(18,26).trim()));
					String tmp = line.substring(26,34).trim();
					if (tmp.length()>0) {
						alts.add(Integer.parseInt(tmp));
						tmp = line.substring(34,42).trim();
						if (tmp.length()>0) {
							alts.add(Integer.parseInt(tmp));
							tmp = line.substring(42,50).trim();
							if (tmp.length()>0) {
								alts.add(Integer.parseInt(tmp));
								tmp = line.substring(50,58).trim();
								if (tmp.length()>0) {
									alts.add(Integer.parseInt(tmp));
								}
							}
						}
					}
					int[] arr = new int[alts.size()];
					for (int i = 0; i < alts.size(); i++) {
						arr[i] = alts.get(i);
					}
					AddtlFeatureNameSet alt = new AddtlFeatureNameSet(
							TLID,
							recordSequenceNumber,
							arr
							);
					list.add(alt);
				} else {
					LOGGER.warn("skipping record in RT4 file b/c its of the wrong type: "+line);
				}
				idx = m.end();
			}
			return list;
		} catch (NumberFormatException nfe) {
			throw new TigerParseException("bad number format in Tiger/LINE file: "+nfe, nfe);
		}
	}
	
	/**
	 * *.RT1 file: primary name of street, along with primary address range, place identifying 
	 * codes,  
	 * and segment identifier TLID.
	 * 
	 * @param r
	 * @return
	 * @throws IOException
	 * @throws TigerParseException
	 */
	private List<CompleteChainRecord> parseRT1(Reader r) throws IOException, TigerParseException {
		String s = dumpReader(r);
		Matcher m = NEW_LINE.matcher(s);
		int idx = 0;
		try {
			List<CompleteChainRecord> list = new ArrayList<CompleteChainRecord>();
			
			int noName = 0;
			while (idx < s.length() && m.find(idx)) {
				String line = s.substring(idx, m.start());
				if (line.charAt(0) == '1') {
					int version = Integer.parseInt(line.substring(1,5).trim());
					checkVersion(version, line);
					int TLID = Integer.parseInt(line.substring(5,15).trim());
					String prefix = line.substring(17,19).trim();
					String name = line.substring(19,49).trim();
					String type = line.substring(49,53).trim();
					String suffix = line.substring(53,55).trim();
					String CFCC = line.substring(55,58).trim();
					// without a name, we consider it unsearchable, so we drop it
					// TODO: we should consider keeping 'F' values as well
					// these are supposed to be just boundaries, but seem to also be 
					// named roads fairly often, as their alternate names
					if (CFCC.charAt(0) == 'A' || 
						CFCC.charAt(0) == 'F' || 
						CFCC.charAt(0) == 'P') {
						// skip the name check, it's possible alternate names 
						// might give us something useful
						if (true || name.length()>0) {
							String frAddL = line.substring(58,69).trim();
							String toAddL = line.substring(69,80).trim();
							String frAddR = line.substring(80,91).trim();
							String toAddR = line.substring(91,102).trim();
							String tmp;
							tmp = line.substring(106,111).trim();
							int zip5left = -1;
							if (tmp.length()>0) {
								zip5left = Integer.parseInt(tmp);
							}
							tmp = line.substring(111,116).trim();
							int zip5right = -1;
							if (tmp.length()>0) {
								zip5right = Integer.parseInt(tmp);
							}
							// state name from this code
							tmp = line.substring(130,132).trim();
							int stateL = -1;
							if (tmp.length()>0) {
								stateL = Integer.parseInt(tmp);
							}
							tmp = line.substring(132,134).trim();
							int stateR = -1;
							if (tmp.length()>0) {
								stateR = Integer.parseInt(tmp);
							}
							tmp = line.substring(134,137).trim();
							int countyL = -1;
							if (tmp.length()>0) {
								countyL = Integer.parseInt(tmp);
							}
							tmp = line.substring(137,140).trim();
							int countyR = -1;
							if (tmp.length()>0) {
								countyR = Integer.parseInt(tmp);
							}
							// city name from this code
							tmp = line.substring(160,165).trim();
							int placeL = -1;
							if (tmp.length()>0) {
								placeL = Integer.parseInt(tmp);
							}
							tmp = line.substring(165,170).trim();
							int placeR = -1;
							if (tmp.length()>0) {
								placeR = Integer.parseInt(tmp);
							}
							double startLon = Double.parseDouble(line.substring(190,200).trim());
							double startLat = Double.parseDouble(line.substring(200,209).trim());
							double endLon = Double.parseDouble(line.substring(209,219).trim());
							double endLat = Double.parseDouble(line.substring(219,228).trim());
							CompleteChainRecord record = new 
							CompleteChainRecord(	  TLID,
									  prefix ,
									  name ,
									  type ,
									  suffix ,
									  CFCC ,
									// from/to address
									  frAddL ,
									  toAddL ,
									  frAddR ,
									  toAddR ,
									  zip5left ,
									  zip5right ,
									// state code
									  stateL ,
									  stateR ,
									  countyL ,
									  countyR ,
									// city/place code
									  placeL ,
									  placeR ,
									  startLon ,
									  startLat ,
									  endLon ,
									  endLat 
									);
							list.add(record);
						} else {
							// proper CFCC, but no name
							noName++;
							LOGGER.debug("skipping record b/c it's name is '"+name+"', and without a name, we can't search for it: "+line);
						}
					} else {
						// might be too verbose
						LOGGER.debug("skipping record b/c it's '"+CFCC.charAt(0)+"', not 'A', for some type of road: "+line);
					}
				} else {
					throw new TigerParseException("skipping record in RT1 file b/c it's not type 1: "+line);
				}
				
				
				idx = m.end();
			}
			LOGGER.warn("skipped "+noName+" records due to having no name");
			return list;
		} catch (NumberFormatException nfe) {
			throw new TigerParseException("bad number format in Tiger/LINE file: "+nfe, nfe);
		}
	}
	
	private final void checkVersion(int version, String line) throws TigerParseException {
		for (int supported : SUPPORTED_VERSIONS) {
			if (version == supported) {
				return;
			}
		}
		throw new TigerParseException("unsupported version "+version+" in RT? line: "+line+" of file: "+file);
	}
	
	private static final int[] SUPPORTED_VERSIONS = {
		Integer.parseInt("0505"),
		Integer.parseInt("0605"),
		Integer.parseInt("1105"),
	};
	private static final int MAX_KB = 100*1000;
	
	private static String dumpReader(Reader r) throws IOException {
		StringBuilder buf = new StringBuilder();
		int sz = 1000;
		char[] cbuf = new char[sz];
		int numRead = -1;
		int kb = 0;
		
		while ((numRead = r.read(cbuf)) > 0 && kb++ < MAX_KB) {
			buf.append(cbuf, 0, numRead);
		}
		return buf.toString();
	}
	
	public List<StorableSegment> parse() throws IOException, TigerParseException, TigerDataException {
		Reader r = null;
		ZipEntry entry = null;
		try {
			List<CompleteChainRecord> completeRecords = null;
			List<AddtlFeatureNameSet> addtlFeatureNameSets = null;
			List<AddtlFeatureName> addtlFeatureNames = null;
			List<AddtlAddressRange> addtlAddressRanges = null;
			List<PlaceName> placeNames = null;
			while ((entry = zin.getNextEntry() ) != null) {
				if (entry.getCompressedSize() > MAX_BYTES) {
					throw new TigerParseException("entry too big, entry: "+entry.getName()+" compressed size: "+entry.getCompressedSize()+" bytes, in file: "+file.getAbsolutePath());
				}
				String name = entry.getName();
				if (name.endsWith(".RT1")) {
					// RT1 file
					r = new InputStreamReader(zin, ENC);
					completeRecords = parseRT1(r);
				} else if (name.endsWith(".RT4")) {
					// RT4 file
					r = new InputStreamReader(zin, ENC);
					addtlFeatureNameSets = parseRT4(r);
				} else if (name.endsWith(".RT5")) {
					// RT5 file
					r = new InputStreamReader(zin, ENC);
					addtlFeatureNames = parseRT5(r);
				} else if (name.endsWith(".RT6")) {
					// RT6 file
					r = new InputStreamReader(zin, ENC);
					addtlAddressRanges = parseRT6(r);
				} else if (name.endsWith(".RTC")) {
					// RTC file
					r = new InputStreamReader(zin, ENC);
					placeNames = parseRTC(r);
				} else {
					LOGGER.info("skipping over file entry '"+name+"' within zip");
				}
				zin.closeEntry();
			}
			LOGGER.info("done parsing "+file.getName()+", about to run integrity check:\n  got "+
					(completeRecords != null ? ""+completeRecords.size() : "empty")+" complete records\n      "+
					(addtlFeatureNameSets != null ? ""+addtlFeatureNameSets.size() : "empty")+" additional feature name sets\n      "+
					(addtlFeatureNames != null ? ""+addtlFeatureNames.size() : "empty")+" additional feature names\n      "+
					(addtlAddressRanges != null ? ""+addtlAddressRanges.size() : "empty")+" additional address ranges\n      "+
					(placeNames != null ? ""+placeNames.size() : "empty")+" place names");
			List<StorableSegment> segments = integrityCheck(
					completeRecords,
					addtlFeatureNameSets,
					addtlFeatureNames,
					addtlAddressRanges,
					placeNames
					);
			LOGGER.info("integrity check completed on file "+file.getName());

			return segments;
		} finally {
			try {
				if (r != null) {
					r.close();
				}
			} finally {
				r = null;
			}
		}
	}

	private List<StorableSegment> integrityCheck(
			List<CompleteChainRecord> completeRecords,
			List<AddtlFeatureNameSet> addtlFeatureNameSets,
			List<AddtlFeatureName> addtlFeatureNames,
			List<AddtlAddressRange> addtlAddressRanges,
			List<PlaceName> placeNames
			) throws TigerDataException {		
		// every line record should be unique
		Map<Integer,CompleteChainRecord> recordHash = new Hashtable<Integer,CompleteChainRecord>();
		for (CompleteChainRecord record : completeRecords) {
			CompleteChainRecord r2 = recordHash.get(record.getTLID());
			if (r2 != null) {
				throw new TigerDataException("conflict, repeated TLID: "+record.getTLID());
			} else {
				recordHash.put(record.getTLID(), record);
			}
		}
		
		// every additional feature name should be unique
		Map<Integer,AddtlFeatureName> namesHash = new Hashtable<Integer,AddtlFeatureName>();
		for (AddtlFeatureName name : addtlFeatureNames) {
			AddtlFeatureName n2 = namesHash.get(name.getFEAT());
			if (n2 != null) {
				throw new TigerDataException("conflict, repeated FEAT: "+name.getFEAT());				
			} else {
				namesHash.put(name.getFEAT(), name);
			}
		}
		
		// every place should be unique
		Map<Integer,PlaceName> placeHash = new Hashtable<Integer,PlaceName>();
		for (PlaceName place : placeNames) {
			PlaceName p2 = placeHash.get(place.getFIPS());
			if (p2 != null) {
				LOGGER.debug("same FIPS: "+place.getFIPS()+", stored: "+p2.getName()+", new: "+place.getName());
				if (!p2.getName().equals(place.getName())) {
					LOGGER.warn("conflict, keeping stored; repeated FIPS: "+place.getFIPS()+", stored: "+p2.getName()+", new: "+place.getName());
					//throw new TigerDataException("conflict, repeated FIPS: "+place.getFIPS()+", stored: "+p2.getName()+", new: "+place.getName());
				}
			} else {
				placeHash.put(place.getFIPS(),place);
			}
		}

		// for every entry in addtlFeatureNameSets, the TLID and FEAT should exist in the appropriate hash
		Map<Integer,List<AddtlFeatureNameSet>> namesetHash = new Hashtable<Integer,List<AddtlFeatureNameSet>>();
		int setNoTLIDcnt = 0;
		for (AddtlFeatureNameSet set : addtlFeatureNameSets) {
			if (recordHash.get(set.getTLID()) == null) {
				setNoTLIDcnt++;
				LOGGER.warn("couldn't find TLID in hash for feature name set: "+set);
				//throw new TigerDataException("no TLID found for featureNameSet entry: "+set);
			} else {
				// we only care about it if it has a TLID that we know about
				// not the proper way to handle priority
				List<AddtlFeatureNameSet> stored = namesetHash.get(set.getTLID());
				if (stored != null) {
					// add to the list
					stored.add(set);
					/*
					// merge the ints
					int[] stints = stored.getFEATs();
					int[] thints = set.getFEATs();
					int[] merged = new int[stints.length+thints.length];
					boolean stfirst = stored.getPriority() > set.getPriority();
					int[] first = stfirst ? stints : thints;
					int[] second = stfirst ? thints : stints;
					for (int i = 0; i < first.length; i++) {
						merged[i] = first[i];
					}
					for (int i = 0; i < second.length; i++) {
						merged[first.length+i] = second[i];
					}
					int minPriority = Math.min(set.getPriority(), stored.getPriority());
					AddtlFeatureNameSet mergedSet = new AddtlFeatureNameSet(stored.getTLID(), minPriority, merged);
					namesetHash.put(mergedSet.getTLID(),mergedSet);
					*/
				} else {
					List<AddtlFeatureNameSet> list = new ArrayList<AddtlFeatureNameSet>();
					list.add(set);
					namesetHash.put(set.getTLID(),list);
				}
			}
			int[] feats = set.getFEATs();
			for (int val : feats) {
				if (namesHash.get(val) == null) {
					throw new TigerDataException("no FEAT found for featureNameSet feat: "+val+", entry: "+set);
				}
			}			
		}
		LOGGER.warn("couldn't find TLID in hash for "+setNoTLIDcnt+"/"+addtlFeatureNameSets.size()+" featureNameSets");
		
		// for every addtlAddressRange, there should be a TLID in the appropriate hash
		Map<Integer,List<AddtlAddressRange>> rangeHash = new Hashtable<Integer,List<AddtlAddressRange>>();
		for (AddtlAddressRange addr : addtlAddressRanges) {
			if (recordHash.get(addr.getTLID()) == null) {
				throw new TigerDataException("no TLID found for addtlAddressRange entry: "+addr);
			} else {
				List<AddtlAddressRange> stored = rangeHash.get(addr.getTLID());
				if (stored != null) {
					stored.add(addr);
				} else {
					stored = new ArrayList<AddtlAddressRange>();
					stored.add(addr);
					rangeHash.put(addr.getTLID(),stored);
				}
			}
		}
		
		int stateButNoPlaceCnt = 0;
		// for every completeRecord, there should be a stateCode and placeName that matches
		for (CompleteChainRecord record : completeRecords) {
			int place;
			int state;
			String str;
			int stateNotSetCount = 0;
			for (int left = 0; left < 2; left++) {
				place = left == 0 ? record.getPlaceL() : record.getPlaceR();
				state = left == 0 ? record.getStateL() : record.getStateR();
				if (state == -1) {
					stateNotSetCount++;
				} else if (place == -1) {
					stateButNoPlaceCnt++;
				}
				str = left == 0 ? "left" : "right";
				// apparently, you can have the state set, but not the place
				if (place != -1 && state == -1) {
					throw new TigerDataException("place was set, but state was not for "+str+" record: "+record);
				} else if (stateNotSetCount > 1) {
					throw new TigerDataException("state was not set, for either the right or the left for record: "+record); 
				} else {
					if (state != -1 && StateCodes.getStateAbbrev(state) == null) {
						throw new TigerDataException("null state at "+str+" code: "+state);
					} else if (place != -1 && placeHash.get(place) == null) {
						throw new TigerDataException("no place name with "+str+" code: "+place);
					}
				}
			}
		}
		
		LOGGER.warn("state but no place count: "+stateButNoPlaceCnt+", for "+
				completeRecords.size()+" complete records");
		
		LOGGER.info("TIGER/Line data checked out okay.");
		// we now have recordHash, namesetHash, namesHash, placeHash, rangeHash

		// remove from recordHash, those records with no name AND no nameset, 
		// since there's no way to search for them without ANY name.
		Iterator<CompleteChainRecord> vals = recordHash.values().iterator();
		Map<Integer,CompleteChainRecord> smallerR = new Hashtable<Integer,CompleteChainRecord>();
		int cnt = 0;
		while (vals.hasNext()) {
			CompleteChainRecord record = vals.next();
			if ((record.getName() != null && record.getName().length() > 0) ||
					namesetHash.get(record.getTLID()) != null	) {
				smallerR.put(record.getTLID(),record);
			} else {
				cnt++;
				LOGGER.debug("skipping over record due to no name (unlocatable): "+record);
			}
		}
		recordHash = smallerR;
		LOGGER.warn("skipped over "+cnt+" records, since they had no names (unlocatable); leaves us with "+recordHash.size()+" records");
		
		// we now have recordHash, namesetHash, namesHash, placeHash, rangeHash
		// go through each segment, and create a StorableSegment record
		Iterator<Integer> tlids = recordHash.keySet().iterator();
		List<StorableSegment> segments = new ArrayList<StorableSegment>();
		int nonamesranges = 0;
		int nonnumericranges = 0;
		while (tlids.hasNext()) {
			Integer tlid = tlids.next();
			CompleteChainRecord r = recordHash.get(tlid);
			StorableSegment segment = new StorableSegment();
			segment.TLID = tlid;
			StorableSegment.Name name;
			List<StorableSegment.Name> names = new ArrayList<StorableSegment.Name>();
			// a street name thing without a name or a type is meaningless to us

			// TODO: FOR NOW, WE SKIP OVER MISSING TYPE IN A NAME, THOSE WITH 
			// NO NAMES OR NO ADDRESS RANGES
			if (r.getName() != null && r.getName().trim().length() > 0 &&
				r.getType() != null && r.getType().trim().length() > 0) {
				name = new StorableSegment.Name();
				name.name = r.getName().trim();
				name.type = r.getType().trim();
				name.prefix = (r.getPrefix() != null && r.getPrefix().trim().length() > 0 ? r.getPrefix().trim() : null);
				name.suffix = (r.getSuffix() != null && r.getSuffix().trim().length() > 0 ? r.getSuffix().trim() : null);				
				names.add(name);
			}
			// do all the extra names
			List<AddtlFeatureNameSet> lnamesets;
			if ((lnamesets = namesetHash.get(tlid)) != null) {
				AddtlFeatureNameSet[] namesets = new AddtlFeatureNameSet[lnamesets.size()];
				lnamesets.toArray(namesets);
				Arrays.sort(namesets);
				for (AddtlFeatureNameSet nameset : namesets) {
					int[] feats = nameset.getFEATs();
					for (int j = 0; j < feats.length; j++) {
						AddtlFeatureName fname = namesHash.get(feats[j]);
						if (fname == null) {
							throw new TigerDataException("unable to find AddtlFeatureName for nameset: "+nameset+", record: "+r);
						}
						// a street name thing without a name or a type is meaningless to us
						
						// TODO: FOR NOW, WE SKIP OVER MISSING TYPE IN A NAME, THOSE WITH 
						// NO NAMES OR NO ADDRESS RANGES
						if (fname.getName() != null && fname.getName().trim().length() > 0 &&
						    fname.getType() != null && fname.getType().trim().length() > 0) {
							name = new StorableSegment.Name();
							name.name = fname.getName().trim();
							name.type = fname.getType().trim();
							name.prefix = (fname.getPrefix() != null && fname.getPrefix().trim().length() > 0 ? fname.getPrefix().trim() : null);
							name.suffix = (fname.getSuffix() != null && fname.getSuffix().trim().length() > 0 ? fname.getSuffix().trim() : null);
							names.add(name);						
						}
					}
				}
			}

			List<StorableSegment.NumberAndZip> lefts = new ArrayList<StorableSegment.NumberAndZip>();
			List<StorableSegment.NumberAndZip> rights = new ArrayList<StorableSegment.NumberAndZip>();
			StorableSegment.NumberAndZip numZip;
			numZip = new StorableSegment.NumberAndZip();
			// left
			if (r.getFrAddL() != null && r.getFrAddL().trim().length() > 0 &&
				r.getToAddL() != null && r.getFrAddL().trim().length() > 0 &&
				r.getZip5left() > 0) {
				numZip = new StorableSegment.NumberAndZip();
				numZip.frAdd = r.getFrAddL().trim();
				numZip.toAdd = r.getToAddL().trim();
				numZip.zip5 = r.getZip5left();
				lefts.add(numZip);
				if (!numZip.isNumeric()) {
					nonnumericranges++;
				}
			}
			// right
			if (r.getFrAddR() != null && r.getFrAddR().trim().length() > 0 &&
				r.getToAddR() != null && r.getFrAddR().trim().length() > 0 &&
				r.getZip5right() > 0) {
				numZip = new StorableSegment.NumberAndZip();
				numZip.frAdd = r.getFrAddR().trim();
				numZip.toAdd = r.getToAddR().trim();
				numZip.zip5 = r.getZip5right();
				rights.add(numZip);
				if (!numZip.isNumeric()) {
					nonnumericranges++;
				}
			}
			// additional ranges
			if (rangeHash.get(tlid) != null) {
				List<AddtlAddressRange> rangesList = rangeHash.get(tlid);
				AddtlAddressRange[] ranges = new AddtlAddressRange[rangesList.size()];
				rangesList.toArray(ranges);
				Arrays.sort(ranges);
				for (AddtlAddressRange range : ranges) {
					// left
					if (range.getFrAddL() != null && range.getFrAddL().trim().length() > 0 &&
						range.getToAddL() != null && range.getFrAddL().trim().length() > 0 &&
						range.getZip5left() > 0) {
						numZip = new StorableSegment.NumberAndZip();
						numZip.frAdd = range.getFrAddL().trim();
						numZip.toAdd = range.getToAddL().trim();
						numZip.zip5 = range.getZip5left();
						lefts.add(numZip);
						if (!numZip.isNumeric()) {
							nonnumericranges++;
						}
					}
					// right
					if (range.getFrAddR() != null && range.getFrAddR().trim().length() > 0 &&
						range.getToAddR() != null && range.getFrAddR().trim().length() > 0 &&
						range.getZip5right() > 0) {
						numZip = new StorableSegment.NumberAndZip();
						numZip.frAdd = range.getFrAddR().trim();
						numZip.toAdd = range.getToAddR().trim();
						numZip.zip5 = range.getZip5right();
						rights.add(numZip);
						if (!numZip.isNumeric()) {
							nonnumericranges++;
						}
					}
				}
			}
			
			// city/place code
			if (r.getPlaceL() >= 0) {
				PlaceName pname = placeHash.get(r.getPlaceL());
				if (pname == null || pname.getName() == null || pname.getName().trim().length() <= 0) {
					throw new TigerDataException("invalid place code: "+r.getPlaceL()+" for record: "+r);
				}
				segment.placeL = pname.getName().trim();
			}
			if (r.getPlaceR() >= 0) {
				PlaceName pname = placeHash.get(r.getPlaceR());
				if (pname == null || pname.getName() == null || pname.getName().trim().length() <= 0) {
					throw new TigerDataException("invalid place code: "+r.getPlaceR()+" for record: "+r);
				}
				segment.placeR = pname.getName().trim();				
			}
			
			// state code
			if (r.getStateL() < 0) {
				if (lefts.size() > 0) {
					throw new TigerDataException("no stateL defined, but there are "+lefts.size()+" left address ranges, for record: "+r);
				}
			} else {
				segment.stateL = StateCodes.getStateAbbrev(r.getStateL());
				if (segment.stateL == null) {
					throw new TigerDataException("state abbreviation for code "+r.getStateL()+" had no match, for record: "+r);
				}
			}
			if (r.getStateR() < 0) {
				if (rights.size() > 0) {
					throw new TigerDataException("no stateR defined, but there are "+rights.size()+" right address ranges, for record: "+r);
				}
			} else {
				segment.stateR = StateCodes.getStateAbbrev(r.getStateR());
				if (segment.stateR == null) {
					throw new TigerDataException("state abbreviation for code "+r.getStateR()+" had no match, for record: "+r);
				}
			}
			
			// lat/lon for origin point and end point of segment
			segment.startLon = r.getStartLon();
			segment.startLat = r.getStartLat();
			segment.endLon = r.getEndLon();
			segment.endLat = r.getEndLat();
			
			// if there are no names (should have already been removed), 
			// or there are no address ranges, we throw an exception

			// TODO: FOR NOW, WE SKIP OVER MISSING TYPE IN A NAME, THOSE WITH 
			// NO NAMES OR NO ADDRESS RANGES
			if (names.size() == 0 || 
				(lefts.size() == 0 && rights.size() == 0)) {
				nonamesranges++;
				LOGGER.debug("missing "+(names.size() == 0 ? "names" : "left and right address ranges")+" for record: "+r);
			} else {
				segment.names = new StorableSegment.Name[names.size()];
				names.toArray(segment.names);
				if (lefts.size() > 0) {
					segment.lefts = new StorableSegment.NumberAndZip[lefts.size()];
					lefts.toArray(segment.lefts);
				}
				if (rights.size() > 0) {
					segment.rights = new StorableSegment.NumberAndZip[rights.size()];
					rights.toArray(segment.rights);
				}
			
				segments.add(segment);
			}
		}
		LOGGER.warn("skipped "+nonamesranges+" segments because they had no complete name, or no address ranges");
		LOGGER.warn("contains "+nonnumericranges+" non-numeric ranges, where !(fraddr >= 0 && toaddr >= 0 && zip > 0)");
		LOGGER.info("we have "+segments.size()+" total segments, ready for indexing");
		
		return segments;
	}
	
	public void close() throws IOException {
		try {
			if (zin != null) {
				zin.close();
			}
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} finally {
				zin = null;
				is = null;
			}
		}
	}
}
