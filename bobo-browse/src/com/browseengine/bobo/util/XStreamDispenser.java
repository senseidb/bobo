package com.browseengine.bobo.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.config.FieldConfiguration;
import com.browseengine.bobo.service.BrowseHitConverter;
import com.browseengine.bobo.service.BrowseResultConverter;
import com.browseengine.bobo.service.FieldConfConverter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.xml.DomDriver;


public class XStreamDispenser {
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface CustomConverter{
		Class value();
	}
	
	private static Collection<Converter> converterSet=new LinkedList<Converter>();
	private static HashMap<Class<?>,String> aliasMap=new HashMap<Class<?>,String>();
	
	static{
	  converterSet.add(new  BrowseHitConverter());
	  aliasMap.put(BrowseHit.class, "hit");
	  
	  converterSet.add(new BrowseResultConverter());
      aliasMap.put(BrowseResult.class, "result");
      
      converterSet.add(new FieldConfConverter());
      aliasMap.put(FieldConfiguration.class, "field-info"); 
	}
	
	private static class DOMXStream extends ThreadLocal {

		protected Object initialValue(){
			return  new BoboXStream(new DomDriver());
		}
	}
	
	private static class JSONXStream extends ThreadLocal {

		protected Object initialValue(){
			return  new BoboXStream(new BoboJSONStreamDriver());
		}
	}

	public static class BoboXStream extends XStream{
		public BoboXStream(HierarchicalStreamDriver driver) {
			super(driver);
			init();
		}
		
		private void init(){
		  Iterator<Class<?>> iter=aliasMap.keySet().iterator();
		  while(iter.hasNext()){
		    Class<?> cls=iter.next();
		    alias(aliasMap.get(cls), cls);
		  }
		  
		  Iterator<Converter> iter2=converterSet.iterator();
		  while(iter2.hasNext()){
		    this.registerConverter(iter2.next());
		  }
		}
	}
	
	private static DOMXStream _xmlInstance = new DOMXStream();
	private static JSONXStream _jsonInstance = new JSONXStream();
	
	public static XStream getXMLXStream(){
		return (XStream)_xmlInstance.get();
	}
	
	public static XStream getJSONXStream(){
		return (XStream)_jsonInstance.get();
	}
}
