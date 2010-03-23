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

package com.browseengine.bobo.index.digest;

import java.io.IOException;
import java.util.Collection;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

import com.browseengine.bobo.facets.FacetHandler;

public abstract class DataDigester {

	public static interface DataHandler{
		void handleDocument(Document doc) throws IOException;
	}
			
	protected DataDigester() {
		super();
	}		
	
	//public static void populateDocument(Document doc,Map data,FieldConfiguration fConf){
	public static void populateDocument(Document doc,Collection<FacetHandler<?>> handlers){
		StringBuilder tokenBuffer=new StringBuilder();
		
		for (FacetHandler<?> handler : handlers){
			String name = handler.getName();
			String[] values=doc.getValues(name);
			if (values!=null){
				doc.removeFields(name);
				for (String value : values){
					doc.add(new Field(name, value,Store.NO, Index.NOT_ANALYZED));
					tokenBuffer.append(' ').append(value);
				}
			}
		}
	}

	public abstract void digest(DataHandler handler) throws IOException;
}
