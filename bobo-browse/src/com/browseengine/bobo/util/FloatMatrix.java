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

package com.browseengine.bobo.util;

import java.lang.reflect.Array;

public class FloatMatrix extends PrimitiveMatrix {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public FloatMatrix(int[] sizes) {
		super(float.class, sizes);
	}

	public FloatMatrix() {
		super(float.class);
	}

	public synchronized void set(int x,int y,float n){		
		ensureCapacity(x,y);		
		// get the row
		Object row=Array.get(_matrix, x);
		if (row==null){
			throw new ArrayIndexOutOfBoundsException("index out of bounds: "+x);
		}
		Array.setFloat(row, y, n);
		_rowCount=Math.max(x, _rowCount);
		_colCount=Math.max(y, _colCount);
	}
	
	public float get(int r,int c){
		Object row=Array.get(_matrix, r);
		if (row==null){
			throw new ArrayIndexOutOfBoundsException("index out of bounds: "+r);
		}
		return Array.getFloat(row, c);
	}
	
	public synchronized float[][] toArray(){		
		float[][] ret=new float[_rowCount][_colCount];
		for (int i=0;i<_rowCount;++i){
			Object row=Array.get(_matrix, i);
			System.arraycopy(row,0,ret[i],0,_colCount);
		}
		return ret;		
	}	
}
