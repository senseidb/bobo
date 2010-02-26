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

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import com.browseengine.local.service.index.NamesFields;
import com.browseengine.local.service.index.SegmentsFields;

/**
 * @author spackle
 *
 */
public class AddressAnalyzer extends Analyzer {
	private Analyzer _standard;
	
	public AddressAnalyzer() {
		_standard = new StandardAnalyzer(new String[0]);
	}

	private TokenStream standardTokenStream(String fieldName, Reader reader) {
		return _standard.tokenStream(fieldName, reader);
	}
	
	public TokenStream tokenStream(String fieldName, final Reader reader) {
		if (NamesFields.TOKENIZED_NAME.getField().equals(fieldName) ||
			SegmentsFields.TOKENIZED_PLACEL.getField().equals(fieldName) ||
			SegmentsFields.TOKENIZED_PLACER.getField().equals(fieldName)) {
			// standard analyzer with no stop words
			return standardTokenStream(fieldName, reader);
		} else {
			return lowerUntokenizedTokenStream(fieldName, reader);
		}
	}
	
	private TokenStream lowerUntokenizedTokenStream(String fieldName, Reader reader) {
		// no tokenization--just take the whole thing
		TokenStream token = new KeywordTokenizer(reader);
		// no standard filter normalization
		// yes lower case everything
		token = new LowerCaseFilter(token);
		// no stop word removal
		return token;
	}
}
