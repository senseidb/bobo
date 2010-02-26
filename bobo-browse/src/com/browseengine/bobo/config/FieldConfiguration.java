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

package com.browseengine.bobo.config;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.PredefinedTermListFactory;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.fields.FieldRegistry;

public class FieldConfiguration {
	private HashMap<String,FacetHandler> _map;
	
	private static Logger logger=Logger.getLogger(FieldConfiguration.class);
	private static Map<String,Class<?>> VALUE_TYPE_MAP = new HashMap<String,Class<?>>();
	
	
	static
	{
		VALUE_TYPE_MAP.put("integer", int.class);
		VALUE_TYPE_MAP.put("char", char.class);
		VALUE_TYPE_MAP.put("date", Date.class);
		VALUE_TYPE_MAP.put("double", double.class);
		VALUE_TYPE_MAP.put("float", float.class);
		VALUE_TYPE_MAP.put("long", long.class);
	}
	
	public FieldConfiguration() {
		super();
		_map=new HashMap<String,FacetHandler>();
	}		
	
	public void addPlugin(String name,String type,Properties props){
      Class<? extends FacetHandler> cls=FieldRegistry.getInstance().getFieldPlugin(type);
      if (cls!=null){
        try
        {
          Constructor<? extends FacetHandler> c=null;
          FacetHandler plugin = null;
          if ("path".equals(type))
          {
        	c = cls.getConstructor(String.class);  
        	plugin=c.newInstance(name);
          }
          else if ("range".equals(type))
          {
        	c = cls.getConstructor(String.class,boolean.class);  
          	plugin=c.newInstance(name,true);
          }
          else
          {
        	c = cls.getConstructor(String.class,TermListFactory.class);
        	String valType=props==null ? "string" : props.getProperty("value_type");
        	String formatString=props==null ? null : props.getProperty("format");
        	
        	TermListFactory termFactory=null;
        	if (valType!=null)
        	{
        		Class<?> supportedType=VALUE_TYPE_MAP.get(valType);
        		if (supportedType!=null)
        		{
        			termFactory = new PredefinedTermListFactory(supportedType,formatString);
        		}
        	}
        	plugin=c.newInstance(name,termFactory);
          }
          _map.put(name, plugin);
        }
        catch (Exception e)
        {
        	e.printStackTrace();
          logger.error(e.getMessage(),e);
        }
      }
      else{
        logger.error(type+" not supported, skipped.");
      }
	}
	
	public Collection<FacetHandler> getFacetHandlers()
	{
		return _map.values();
	}
	
	public boolean fieldDefined(String fieldName){
		return _map.containsKey(fieldName);
	}
	
	public FacetHandler getFieldPlugin(String fieldName){
		return _map.get(fieldName);
	}
	
	public String[] getFieldNames(){
		Set<String> keys=_map.keySet();
		String[] names=new String[keys.size()];
		names=keys.toArray(names);
		return names;
	}
	
	@Override
	public String toString(){
		StringBuffer buffer=new StringBuffer();
		buffer.append(_map);
		return buffer.toString();
	}
}
