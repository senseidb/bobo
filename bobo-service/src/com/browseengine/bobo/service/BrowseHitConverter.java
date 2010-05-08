package com.browseengine.bobo.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.browseengine.bobo.api.BrowseHit;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Marshaling code for #BrowseHit
 */
public class BrowseHitConverter implements Converter {

	public void marshal(Object obj, HierarchicalStreamWriter writer,
			MarshallingContext ctx) {
		writer.startNode("hit");
		BrowseHit hit=(BrowseHit)obj;
		writer.addAttribute("score", String.valueOf(hit.getScore()));
		writer.addAttribute("docid", String.valueOf(hit.getDocid()));

		
		Map<String,String[]> fieldVals=hit.getFieldValues();
		writer.addAttribute("numfields", fieldVals==null ? "0" : String.valueOf(fieldVals.size()));
		
		if (fieldVals!=null)
		{
			
			Iterator<String> iter=fieldVals.keySet().iterator();
			while(iter.hasNext()){
				String name=iter.next();
				writer.startNode(name);
				writer.setValue(Arrays.toString(fieldVals.get(name)));
				writer.endNode();
			}
		}
		writer.endNode();
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext ctx) {
		BrowseHit hit=new BrowseHit();
		reader.moveDown();
		String scoreString=reader.getAttribute("score");
		if (scoreString!=null){
			hit.setScore(Float.parseFloat(scoreString));
		}
		String docidString=reader.getAttribute("docid");
		if (docidString!=null){
			hit.setDocid(Integer.parseInt(docidString));
		}
		
		int numFields=0;
		String fieldCountString=reader.getAttribute("numfields");
		if (fieldCountString!=null){
			numFields=Integer.parseInt(fieldCountString);
		}
		
		Map<String,String[]> fieldVals=new HashMap<String,String[]>();
		hit.setFieldValues(fieldVals);
		for (int i=0;i<numFields;++i){
			reader.moveDown();
			String fieldname=reader.getNodeName();
			String fieldval = reader.getValue();
			String s2=fieldval.substring(1,fieldval.length()-1);
			String[] parts=s2.split(", ");
			fieldVals.put(fieldname, parts);
			reader.moveUp();
		}
		reader.moveUp();
		return hit;
	}

	public boolean canConvert(Class clazz) {
		return BrowseHit.class.equals(clazz);
	}
}
