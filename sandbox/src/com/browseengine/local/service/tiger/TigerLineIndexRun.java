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
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * @author spackle
 *
 */
public class TigerLineIndexRun {
	private static final Logger LOGGER = Logger.getLogger(TigerLineIndexRun.class);

	private static final String COPYRIGHT = 
		"Copyright (c) bobo-browse project 2006, based at https://sourceforge.net/projects/bobo-browse/.\nProtected by Gnu Lesser Public License (LGPL), available at http://www.gnu.org/licenses/lgpl.html.\nAll rights reserved.  See source code for more details.\n";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//System.out.println(COPYRIGHT);
		if (args.length != 2) {
			usage();
		}
		File f = new File(args[0]);
		File index = new File(args[1]);
		try {
			TigerLineIndexRun runner = new TigerLineIndexRun(f, index);
			runner.processFiles();
		} catch (IOException ioe) {
			LOGGER.error(ioe.toString(), ioe);
		} catch (TigerParseException tlpe) {
			LOGGER.error(tlpe.toString(), tlpe);
		} catch (TigerDataException tde) {
			LOGGER.error(tde.toString(), tde);
		}
	}
	
	private static void usage() {
		String mainClass = TigerLineIndexRun.class.getCanonicalName();
		System.out.println("incorrect arguments to "+mainClass+".main(String[])");
		System.err.println("usage: java "+mainClass+" <tiger_line_path> <dest_index_path>\n"+
		        "     where <tiger_line_path> is a either the path to a tgrXXYYY.zip file,\n"+
				"     or a directory hierarchy that contains these zip files;\n"+
		        "     and <dest_index_path> is the path to the output index you want to create");
		System.exit(-1);
	}

	private File startLocation;
	private static final FileFilter FILE_FILTER = new TigerFileFilter();
	
	private static class TigerFileFilter implements FileFilter {
		public boolean accept(File f) {
			if (f.exists()) {
				if (f.isDirectory()) {
					return true;
				} else if (f.isFile() && f.getName().startsWith("tgr") &&
						f.getName().endsWith(".zip")) {
					return true;
				}
			}
			return false;
		}
	}
	
	public TigerLineIndexRun(File tigerLineFiles, File indexPath) 
	throws TigerParseException, TigerDataException, IOException {
		startLocation = tigerLineFiles;
		if (!FILE_FILTER.accept(startLocation)) {
			throw new TigerParseException("invalid TIGER/Line file or dir "+startLocation.getAbsolutePath());
		}
		_writer = new SegmentIndexWriter(indexPath);
	}
	
	public void processFiles() throws IOException, TigerParseException, TigerDataException {
		try {
			processRecursive(startLocation);
			LOGGER.info("about to optimize...");
			_writer.optimize();
			LOGGER.info("...done optimizing.");
		} finally {
			_writer.close();
			LOGGER.info("done indexing.");
		}
	}
	
	private SegmentIndexWriter _writer;
	
	private void processRecursive(File f) throws IOException, TigerParseException, TigerDataException {
		if (f.isDirectory()) {
			File[] theFiles = f.listFiles(FILE_FILTER);
			for (File file : theFiles) {
				processRecursive(file);
			}
		} else if (f.isFile()) {
			LOGGER.info("about to start file: '"+f.getName()+"'...");
			TigerLineZipParser parser = new TigerLineZipParser(f);
			List<StorableSegment> segments = parser.parse();
			int count = 0;
			for (StorableSegment segment : segments) {
				_writer.addSegment(segment);
				if (++count % 10000 == 0) {
					LOGGER.info("done indexing "+count+" documents from this file");
				}
			}
			parser.close();
			LOGGER.info("...done with file: '"+f.getName()+"'");
		} 
	}
	
}
