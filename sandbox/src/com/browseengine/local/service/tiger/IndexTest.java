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

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

/**
 * @author spackle
 *
 */
public class IndexTest {
	private static final Logger LOGGER = Logger.getLogger(IndexTest.class);

	private IndexWriter _writer;
	public IndexTest() throws IOException {
		File f = new File("tmptest");
		_writer = new IndexWriter(f, new StandardAnalyzer(), true);
	}

	public void indexADocument() throws IOException {
		Document doc = new Document();
		doc.add(new Field("id", ""+12345, 
				Field.Store.YES, Field.Index.UN_TOKENIZED, Field.TermVector.NO));
		_writer.addDocument(doc);
	}
	
	public void optimize() throws IOException {
		_writer.optimize();
	}
	
	public void close() throws IOException {
		try {
			if (_writer != null) {
				_writer.close();
			}
		}finally {
			_writer = null;
		}
	}
	
	public static void main(String[] argv) {
		IndexTest test = null;
		try {
			test = new IndexTest();
			test.indexADocument();
			test.optimize();
			test.close();
			test = null;
			LOGGER.info("successfully indexed a document");
		} catch (IOException ioe) {
			LOGGER.error(ioe.toString(), ioe);
		} finally {
			try {
				if (test != null) {
					test.close();
				}
			} catch (IOException ioe) {
				LOGGER.error(ioe.toString(), ioe);
			}
		}
		
	}
}
