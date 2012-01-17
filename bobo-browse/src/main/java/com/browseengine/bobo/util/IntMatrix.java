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

public class IntMatrix extends PrimitiveMatrix {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IntMatrix(int[] sizes) {
		super(int.class, sizes);
	}

	public IntMatrix() {
		super(int.class);
	}

	public synchronized void set(int x,int y,int n){		
		ensureCapacity(x,y);		
		// get the row
		Object row=Array.get(_matrix, x);
		if (row==null){
			throw new ArrayIndexOutOfBoundsException("index out of bounds: "+x);
		}
		Array.setInt(row, y, n);
		_rowCount=Math.max(x, _rowCount);
		_colCount=Math.max(y, _colCount);
	}
	
	public int get(int r,int c){
		Object row=Array.get(_matrix, r);
		if (row==null){
			throw new ArrayIndexOutOfBoundsException("index out of bounds: "+r);
		}
		return Array.getInt(row, c);
	}
	
	public synchronized int[][] toArray(){		
		int[][] ret=new int[_rowCount][_colCount];
		for (int i=0;i<_rowCount;++i){
			Object row=Array.get(_matrix, i);
			System.arraycopy(row,0,ret[i],0,_colCount);
		}
		return ret;		
	}
	
	public static void main(String[] args) {
		IntMatrix matrix=new IntMatrix(new int[]{2,4});
		matrix.set(0, 0, 5);
		//matrix.set(1, 1, 6);
		//matrix.set(2, 2, 7);
		matrix.set(100, 100, 9);
		System.out.println(matrix);
		matrix.seal();
		System.out.println(matrix);
		
	}
}
