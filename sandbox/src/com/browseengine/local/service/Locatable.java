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
 * please go to https://sourceforge.net/projects/bobo-browse/.
 */
package com.browseengine.local.service;

/**
 * A location is something that can be represented by a single 
 * (latitude, longitude) pair.
 * 
 * @author spackle
 *
 */
public interface Locatable {
	/**
	 * gets the longitudinal coordinate of this <code>Locatable</code>, 
	 * in degress.
	 * 
	 * @return
	 */
	double getLongitudeDeg();

	/**
	 * @see #getLongitudeDeg(), but in radians
	 * 
	 * @return
	 */
	double getLongitudeRad();

	/**
	 * gets the latitudinal coordinate of this <code>Locatable</code>,
	 * in degrees.
	 * 
	 * @return
	 */
	double getLatitudeDeg();

	/**
	 * @see #getLatitudeDeg(), but in radians
	 *
	 * @return
	 */
	double getLatitudeRad();
}
