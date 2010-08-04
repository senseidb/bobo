package com.browseengine.bobo.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.browseengine.bobo.index.digest.FileDigester;

public class CarDigester extends FileDigester {

	public CarDigester(File file) throws IOException{
		super(file);
	}

	/*static TagsMaker tagsMaker;
	static{
		try {
			tagsMaker=TagsMaker.loadFile(new File("/Users/john/Desktop/cartags"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}*/
	
	
	static Hashtable<String,NumberFormat> formatterHash=new Hashtable<String,NumberFormat>();	
	static{
		NumberFormat intFormat=NumberFormat.getInstance();
		intFormat.setGroupingUsed(false);
		formatterHash.put("year",intFormat);

		formatterHash.put("mileage",intFormat);

		NumberFormat format=NumberFormat.getInstance();
		format.setGroupingUsed(false);
		format.setMaximumFractionDigits(2);
		format.setMinimumFractionDigits(2);
		formatterHash.put("price",format);
	}
	private static Document makeDocument(Properties prop){

		Document doc=new Document();
		Enumeration nameIter=prop.propertyNames();
		while(nameIter.hasMoreElements()){
			String name=(String)nameIter.nextElement();
			NumberFormat format=formatterHash.get(name);
			String val=prop.getProperty(name);
			
			String[] vals=val.split(",");
			for (int i=0;i<vals.length;++i){
				if (vals[i]!=null && vals[i].length()>0){
					if (format!=null){
						vals[i]=format.format(Double.parseDouble(vals[i]));
					}
					doc.add(new Field(name,vals[i],Field.Store.NO,Field.Index.NOT_ANALYZED,Field.TermVector.NO));
				}
			}	
		}
		
		/*String[] tags=tagsMaker.getTags(prop);
		if (tags.length>0){
			for (int i=0;i<tags.length;++i){
				doc.add(new Field("tags",tags[i],Field.Store.NO,Field.Index.UN_TOKENIZED,Field.TermVector.NO));
			}
		}*/
		return doc;
	}

	@Override
	public void digest(DataHandler handler) throws IOException {
		FileInputStream fin=null;
		try{
			fin=new FileInputStream(getDataFile());
			BufferedReader reader=new BufferedReader(new InputStreamReader(fin,getCharset()));
			String line=null;
			Properties prop=new Properties();
			while(true){
				line=reader.readLine();
				if (line==null){
					break;
				}
				if ("<EOD>".equals(line)){		//new record
					Document doc=makeDocument(prop);
					populateDocument(doc,null);
					handler.handleDocument(doc);
					prop=new Properties();
				}
				else{
					int index=line.indexOf(":");
					if (index!=-1){
						String name=line.substring(0, index);
						String val=line.substring(index+1,line.length());
						prop.put(name.trim(), val.trim());
					}
				}
			}
			reader.close();
		}
		finally{
			if (fin!=null){
				fin.close();
			}
		}
	}
}
