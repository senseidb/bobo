/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  spackle
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
 * contact owner@browseengine.com.
 */

package com.browseengine.local.servlet;

import java.lang.reflect.Method;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;

import com.browseengine.bobo.service.ServiceServlet;
import com.browseengine.local.service.LocalService;
import com.browseengine.local.service.LocalService.LocalException;
import com.browseengine.local.service.impl.SingletonLocalServiceFactory;

/**
 * @author spackle
 *
 */
public class LocalServlet extends ServiceServlet {
	private static final Logger LOGGER = Logger.getLogger(LocalServlet.class);

	public LocalServlet() {
		super();
	}
	
	@Override
	protected Object getServiceInstance(ServletConfig config) throws ServletException {
		String configDir = getInitParameter("geoconfig.dir");
		if (null == configDir || configDir.length() == 0) {
			configDir = getInitParameter("config.dir");
			if (null == configDir || configDir.length() == 0) {
				throw new ServletException("No config directory configured");
			}
		}

		try {
			return SingletonLocalServiceFactory.getLocalServiceImpl(configDir);
		} catch (LocalException e) {
			throw new ServletException(e.toString(), e);
		}

	}

	@Override
	protected Method[] getSupportedMethods() {
		return LocalService.class.getDeclaredMethods();
	}

	@Override
	protected void shutdownService(Object service) {
		LocalService svc=(LocalService)service;
		try {
			svc.close();
		} catch (LocalException e) {
			LOGGER.warn("Trouble shutting down search service.: "+e.getMessage());
		}
	}

}
