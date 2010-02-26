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

package com.browseengine.bobo.fields;

import java.util.HashMap;

import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.impl.CompactMultiValueFacetHandler;
import com.browseengine.bobo.facets.impl.MultiValueFacetHandler;
import com.browseengine.bobo.facets.impl.PathFacetHandler;
import com.browseengine.bobo.facets.impl.RangeFacetHandler;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;

public class FieldRegistry {
	private HashMap<String, Class<? extends FacetHandler>> _pluginMap;

	private FieldRegistry() {
		_pluginMap = new HashMap<String, Class<? extends FacetHandler>>();
	}

	private static final FieldRegistry instance = new FieldRegistry();

	static{
		instance.registerFieldPlugin("path", PathFacetHandler.class);
		instance.registerFieldPlugin("simple", SimpleFacetHandler.class);
		instance.registerFieldPlugin("range", RangeFacetHandler.class);
		instance.registerFieldPlugin("tags", MultiValueFacetHandler.class);
		instance.registerFieldPlugin("multi", MultiValueFacetHandler.class);
		instance.registerFieldPlugin("compact", CompactMultiValueFacetHandler.class);
	}
	
	public static FieldRegistry getInstance() {
		return instance;
	}
	
	public Class<? extends FacetHandler> getFieldPlugin(String typename){
		synchronized(_pluginMap){
			return _pluginMap.get(typename);
		}
	}

	public void registerFieldPlugin(String typename, Class<? extends FacetHandler> cls) {
		if (typename != null) {
			if (FacetHandler.class.isAssignableFrom(cls)) {
				synchronized (_pluginMap) {
					String name = typename.trim().toLowerCase();
					if (_pluginMap.get(name) == null) {
						try {
							//plugin = (FieldPlugin) cls.newInstance();
							_pluginMap.put(name, cls);
						} catch (Exception e) {
							throw new RuntimeException(e.getMessage(), e);
						}
					} else {
						throw new RuntimeException("plugin: " + name
								+ " already exists.");
					}
				}
			}
			else{
				throw new RuntimeException(cls+" is not a valid subclass of "+FacetHandler.class);
			}
		}
	}
}
