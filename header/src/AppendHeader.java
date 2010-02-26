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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author spackle
 *
 */
public class AppendHeader {
	private String _header;
	private Pattern _required;
	private char[] _cbuf = new char[2*1024*1024];

	private static final void usage() {
		System.err.println("Usage: java AppendHeader <header_txt_file> <path_to_change>");
		System.exit(-1);
	}
	
	public static void main(String[] argv) {
		try {
			if (argv.length != 2) {
				usage();
			}
			
			File headerTxt = new File(argv[0]);
			File changeDir = new File(argv[1]);
			AppendHeader appender = new AppendHeader(headerTxt);
			appender.appendJavaHeaders(changeDir);
			
		} catch (Throwable t) {
			System.err.println("fail: "+t);
			t.printStackTrace();
		}
	}
	
	private static final Pattern DEFAULT_REQUIRED = Pattern.compile("\\*\\s+copyright", Pattern.CASE_INSENSITIVE);

	public void appendJavaHeaders(File rootDir) throws IOException {
		Pattern p = Pattern.compile("\\A.*\\.java\\z");
		appendHeaders(rootDir, p);
	}
	
	public void appendHeaders(File rootDir, Pattern matchingFiles) throws IOException {
		if (rootDir.isDirectory()) {
			File[] files = rootDir.listFiles();
			for (File f : files) {
				appendHeaders(f, matchingFiles);
			}
		} else {
			String name = rootDir.getName();
			Matcher m = matchingFiles.matcher(name);
			if (m.matches()) {
				// append the header
				appendHeader(rootDir);
			}
		}
	}

	private static String getString(char[] cbuf, int start, int len) {
		char[] chars = new char[len];
		for (int i = 0; i < len; i++) {
			chars[i] = cbuf[start+i];
		}
		return new String(chars);
	}
	
	private void appendHeader(File f) throws IOException {
		InputStream is = null;
		Reader r=  null;
		OutputStream os = null;
		Writer w = null;
		try {
			is = new FileInputStream(f);
			r = new InputStreamReader(is, "UTF-8");
			int numRead = r.read(_cbuf);
			r.close();
			r = null;
			is.close();
			is = null;
			if (numRead == _cbuf.length) {
				throw new IOException("ran out of room in buffer: "+numRead);
			}
			if (numRead < 0) {
				throw new IOException("didn't read anything from file: "+f.getAbsolutePath()+"; do you want to permit zero-length *.java files?");
			}
			String s = getString(_cbuf, 0, numRead);
			Matcher m = _required.matcher(s);
			if (!m.find()) {
				// we need to add the header to the start of the file.
				File tmp = new File(f.getParent(), f.getName()+".tmp");
				if (!f.renameTo(tmp)) {
					throw new IOException("rename to failed, might be bad.  from: "+f.getAbsolutePath()+" to "+tmp.getAbsolutePath());
				}
				os = new FileOutputStream(f);
				w = new OutputStreamWriter(os, "UTF-8");
				w.write(_header);
				w.write('\n');
				w.write(s);
				w.close();
				w = null;
				os.close();
				os = null;
				if (!tmp.delete()) {
					throw new IOException("trouble deleting tmp file: "+tmp.getAbsolutePath());
				}
			} else {
				System.out.println("already found header for file: "+f.getAbsolutePath());
			}
		} finally {
			try {
				if (w != null) {
					w.close();
				}
			} finally {
				try {
					if (os != null) {
						os.close();
					}
				} finally {
					try {
						if (r != null) {
							r.close();
						}
					} finally {
						try {
							if (is != null) {
								is.close();
							}
						} finally {
							//
						}
					}					
				}
			}
		}
	}
	
	public AppendHeader(String headerContents) throws IOException {
		this(headerContents, DEFAULT_REQUIRED);
	}
	
	public AppendHeader(String headerContents, Pattern p) {
		_header = headerContents;
		_required = p;
	}
	
	public AppendHeader(File headerFile) throws IOException {
		this(headerFile, DEFAULT_REQUIRED);
	}
	
	public AppendHeader(File headerFile, Pattern requiredPattern) throws IOException {
		InputStream is = null;
		Reader r = null;
		try {
			is = new FileInputStream(headerFile);
			r = new InputStreamReader(is, "UTF-8" );
			int numRead = r.read(_cbuf);
			if (numRead < 5) {
				throw new IOException("header is probably too short: "+numRead);
			} else if (numRead == _cbuf.length) {
				throw new IOException("we may have run out of room: "+numRead);
			}
			_header = getString(_cbuf, 0, numRead);
			_required = requiredPattern;
		} finally {
			try {
				if (r != null) {
					r.close();
				}
			} finally {
				try {
					if (is != null) {
						is.close();
					}
				} finally {
					
				}
			}
		}
	}
}
