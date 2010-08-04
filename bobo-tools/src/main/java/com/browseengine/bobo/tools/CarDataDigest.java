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

package com.browseengine.bobo.tools;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import com.browseengine.bobo.index.digest.FileDigester;

// used to hack up some cars for data
public class CarDataDigest extends FileDigester {

	private static final String[] cars=new String[]{"Asian/Toyota/Prius,Hybrid,Silver,Gold,Blue,White,Magenta",
	                           "Asian/Toyota/Avalon,Compact,Black,White,Gold,Silver",
	                           "Asian/Honda/Insight,Hybrid,Black,White,Blue,Silver",
	                           "Asian/Honda/Civic Hybrid,Hybrid,Black,White,Blue,Silver,Green",
	                           "European/Volkswagen/Jetta,Compact,Cold,Silver,White,Blue",
	                           "European/Volkswagen/Passat,Compact,Cold,Silver,White,Blue",
	                           "European/Volkswagen/Beetle,Compact,Red,Green,Yellow,Blue,Black",
	                           "European/Volkswagen/Santana,Compact,Red,Blue,Black",
	                           "North American/Plymouth/Horizon,Compact,Blue,White,Green,Silver",
	                           "North American/Chevrolet/Corvette,Exotic,Red,Yellow,Black"};
	
	private static final String[] cities=new String[]{
		"Australia/Gold Coast",
		"Australia/Melbourn",
		"Australia/Perth",
		"Australia/Sydney",
		"Australia/Wollongong",
		"Canada/Calgary",
		"Canada/Montreal",
		"Canada/Toronto",
		"Canada/Vancouver",
		"U.S.A./California/Los Angeles",
		"U.S.A./California/Sacramento",
		"U.S.A./California/San Diego",
		"U.S.A./California/San Francisco",
		"U.S.A./California/San Jose",
		"U.S.A./California/Sunnyvale",
		"U.S.A./Florida/Miami",
		"U.S.A./Florida/Orlando",
		"U.S.A./Florida/Palm Beach",
		"U.S.A./Florida/Tampa",
		"U.S.A./New York/Albany",
		"U.S.A./New York/Binghamton",
		"U.S.A./New York/Buffalo",
		"U.S.A./New York/New York",
		"U.S.A./New York/Rochester",
		"U.S.A./New York/Syracuse",
		"U.S.A./Taxas/Austin",
		"U.S.A./Taxas/Dallas",
		"U.S.A./Texas/Houston",
		"U.S.A./Texas/San Antonio",
		"U.S.A./Utah/Provo",
		"U.S.A./Utah/Salt Lake City",
		"U.S.A./Washington D.C."
	};
	
	private static void makeCar(Document car,Document doc)
	{

		car.add(new Field("color", doc.get("color"),Store.YES, Index.NOT_ANALYZED));

		car.add(new Field("category", doc.get("category"),Store.YES, Index.NOT_ANALYZED));
		car.add(new Field("city", doc.get("city"),Store.YES, Index.NOT_ANALYZED));
		car.add(new Field("makemodel", doc.get("makemodel"),Store.YES, Index.NOT_ANALYZED));
		car.add(new Field("price", doc.get("price"),Store.YES, Index.NOT_ANALYZED));
		car.add(new Field("year", doc.get("year"),Store.YES, Index.NOT_ANALYZED));
		car.add(new Field("mileage", doc.get("mileage"),Store.YES, Index.NOT_ANALYZED));
	}

	private static void makeCar(Document car,String carLine)
	{
		String[] parts=carLine.split(",");
		int numColors=parts.length-2;
		String make=parts[0];
		String category=parts[1];
		
		Random rand=new Random();
		int colorIdx=rand.nextInt(numColors);
		int cityIdx=rand.nextInt(cities.length);
		
		String color=parts[colorIdx+2];
		String city=cities[cityIdx];
		
		String year=rand.nextInt(10)+1993+"";
		
		String price=""+(rand.nextInt(174)+21)*100;
		String mileage=""+(rand.nextInt(80)+101)*100;
		
		car.add(new Field("color", color,Store.YES, Index.NOT_ANALYZED));
		car.add(new Field("category", category,Store.YES, Index.NOT_ANALYZED));
		car.add(new Field("city", city,Store.YES, Index.NOT_ANALYZED));
		car.add(new Field("makemodel", make,Store.YES, Index.NOT_ANALYZED));
		car.add(new Field("price", price,Store.YES, Index.NOT_ANALYZED));
		car.add(new Field("year", year,Store.YES, Index.NOT_ANALYZED));
		car.add(new Field("mileage", mileage,Store.YES, Index.NOT_ANALYZED));		
	}
	
	public CarDataDigest(File file) {
		super(file);
	}

	public void digest(DataHandler handler) throws IOException {
		int numcars=getMaxDocs();
		Random rand=new Random();
				
		IndexReader reader=null;
		try{
			reader=IndexReader.open(FSDirectory.open(getDataFile()),true);
			int carcount=reader.maxDoc();
			
			Document[] docCache=new Document[carcount];
			for (int i=0;i<carcount;++i){
				docCache[i]=reader.document(i);
			}
			
			for (int i=0;i<numcars;++i){
				if (i!=0 && i%1000==0){
					System.out.println(i+" cars indexed.");
				}
				Document doc=new Document();
				int n=rand.nextInt(10);
				if (n==0){
					makeCar(doc,cars[rand.nextInt(cars.length)]);					
				}
				else{
					Document srcDoc=docCache[rand.nextInt(carcount)];
					makeCar(doc,srcDoc);					
				}
				
				populateDocument(doc,null);																
				handler.handleDocument(doc);				
			}
		}
		finally{
			if (reader!=null){
				reader.close();
			}
		}
	}

}
