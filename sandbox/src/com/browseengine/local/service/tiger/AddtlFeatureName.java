/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2006  Spackle
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

package com.browseengine.local.service.tiger;

/**
 * an RC5 record.
 * 
 * @author spackle
 *
 */
public class AddtlFeatureName {
	private int FEAT = -1;
	private String prefix;
	private String name;
	private String type;
	private String suffix;

	public AddtlFeatureName(
			int featureIDCode,
			String prefix,
			String name,
			String type,
			String suffix
	) {
		this.FEAT = featureIDCode;
		this.prefix = prefix;
		this.name = name;
		this.type = type;
		this.suffix = suffix;
	}

	public int getFEAT() {
		return FEAT;
	}

	public String getName() {
		return name;
	}

	public String getPrefix() {
		return prefix;
	}

	public String getSuffix() {
		return suffix;
	}

	public String getType() {
		return type;
	}
	
}
