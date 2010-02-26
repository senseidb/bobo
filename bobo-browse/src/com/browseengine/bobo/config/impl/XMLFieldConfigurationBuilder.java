package com.browseengine.bobo.config.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.config.FieldConfiguration;
import com.browseengine.bobo.config.FieldConfigurationBuilder;
import com.browseengine.bobo.util.XStreamDispenser;
import com.thoughtworks.xstream.XStream;

public class XMLFieldConfigurationBuilder implements FieldConfigurationBuilder {
	private static final Logger log = Logger.getLogger(XMLFieldConfigurationBuilder.class);
	
	private final File _xmlFile;
	
	public XMLFieldConfigurationBuilder(File xmlFile)
	{
		_xmlFile=xmlFile;
	}
	

	public static FieldConfiguration loadFieldConfiguration(File file) throws IOException{
		FileInputStream fin=null;	
		try{
			fin=new FileInputStream(file);
			return loadFieldConfiguration(fin);
		}
		finally{
			if (fin!=null){				
				fin.close();				
			}
		}		
	}
	
	public static FieldConfiguration loadFieldConfiguration(InputStream input) throws IOException{
			XStream xstream=XStreamDispenser.getXMLXStream();
			return (FieldConfiguration)xstream.fromXML(input);
	}
	
	public FieldConfiguration build() throws BrowseException {
		FileInputStream fin=null;	
		try{
			fin=new FileInputStream(_xmlFile);
			return loadFieldConfiguration(fin);
		}
		catch(IOException ioe)
		{
			throw new BrowseException(ioe.getMessage(),ioe);
		}
		finally{
			if (fin!=null){				
				try {
					fin.close();
				} catch (IOException e) {
					log.error(e.getMessage(),e);
				}				
			}
		}
	}
}
