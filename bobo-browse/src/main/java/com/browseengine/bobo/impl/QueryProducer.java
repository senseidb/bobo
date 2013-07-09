/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation
 * that handles various types of semi-structured data.  Written in Java.
 *
 * Copyright (C) 2005-2006  John Wang
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

package com.browseengine.bobo.impl;

import java.text.ParseException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Version;


public class QueryProducer {
  public static final String CONTENT_FIELD = "contents";

  public static Query convert(String queryString, String defaultField) throws ParseException, org.apache.lucene.queryparser.classic.ParseException {
    if (queryString == null || queryString.length() == 0) {
      return null;
    } else {
      Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_43);
      if (defaultField == null) defaultField = "contents";
      return new QueryParser(Version.LUCENE_43, defaultField, analyzer).parse(queryString);
    }
  }

  final static SortField[] DEFAULT_SORT = new SortField[] { SortField.FIELD_SCORE };

  public Query buildQuery(String query) throws ParseException, org.apache.lucene.queryparser.classic.ParseException {
    return convert(query, CONTENT_FIELD);
  }
}
