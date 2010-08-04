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

package com.browseengine.bobo.index;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.store.FSDirectory;
import com.browseengine.bobo.index.digest.FileDigester;

public class MakeBobo {
	private static void usage(Options options){
		HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "BoboIndexer", options );
	}
		
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub

		Option help = new Option( "help",false, "print this message" );

		OptionBuilder.withArgName("path");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("data source - required");
		Option src = OptionBuilder.create("source");
		src.setRequired(true);

		OptionBuilder.withArgName("path");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("index to create - required");
		Option index = OptionBuilder.create("index");
		index.setRequired(true);

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("field configuration - optional");
		Option conf = OptionBuilder.create("conf");

		OptionBuilder.withArgName("class");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("class name of the data digester - default: xml digester");
		Option digesterOpt = OptionBuilder.create("digester");

		OptionBuilder.withArgName("name");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("character set name - default: UTF-8");
		Option charset = OptionBuilder.create("charset");
		
		OptionBuilder.withArgName("maxdocs");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("maximum number of documents - default: 100");
		Option maxdocs = OptionBuilder.create("maxdocs");

		Options options=new Options();
		options.addOption(help);
		options.addOption(conf);
		options.addOption(index);		
		options.addOption(src);
		options.addOption(charset);
		options.addOption(digesterOpt);
		options.addOption(maxdocs);
						
//		 create the parser
		
		CommandLineParser parser = new BasicParser();
	    try {	    	
	        // parse the command line arguments
	        CommandLine line = parser.parse( options, args );
	        String output=line.getOptionValue("index");
        	File data=new File(line.getOptionValue("source"));
        	
        	Class digesterClass;
        	if (line.hasOption("digester"))
        		digesterClass=Class.forName(line.getOptionValue("digester"));
        	else
        		throw new RuntimeException("digester not specified");
        	
        	Charset chset;
        	if (line.hasOption("charset")){
        		chset=Charset.forName(line.getOptionValue("charset"));
        	}
        	else{
        		chset=Charset.forName("UTF-8");
        	}
        	
        	int maxDocs;
    		try{
    			maxDocs=Integer.parseInt(line.getOptionValue("maxdocs"));
    		}
    		catch(Exception e){
    			maxDocs=100;
    		}
       
        	FileDigester digester;
        	try {
        		Constructor constructor=digesterClass.getConstructor(new Class[]{File.class});        		
    			digester=(FileDigester) constructor.newInstance(new Object[]{data});
    			digester.setCharset(chset);
    			digester.setMaxDocs(maxDocs);
    		} catch (Exception e) {
    			throw new RuntimeException("Invalid digester class.",e);
    		} 	
    		
    		
        	BoboIndexer indexer=new BoboIndexer(digester,FSDirectory.open(new File(output)));
        	indexer.index();
	    }
	    catch( ParseException exp ) {
	    	exp.printStackTrace();
	        usage(options);	        
	    } catch (ClassNotFoundException e) {
			System.out.println("Invalid digester class.");
			usage(options);
		} 
	}
}
