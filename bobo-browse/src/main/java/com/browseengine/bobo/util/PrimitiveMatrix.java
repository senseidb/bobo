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

import java.io.Serializable;
import java.lang.reflect.Array;

public abstract class PrimitiveMatrix implements Serializable{
	protected Object _matrix;
	protected int _rowCount;
	protected int _colCount;
	private int _growth;
	private int[] _dim;
	private transient Class _type;
	
	private static final int DEFAULT_SIZE=1000;
	
	protected PrimitiveMatrix(Class type,int[] sizes) {
		super();
		_type=type;
		if (sizes==null || sizes.length!=2) throw new IllegalArgumentException("Invalid dimension specified.");
		
		if (sizes[0]<=0 || sizes[1]<=0) throw new IllegalArgumentException("Invalid size specified.");
		
		_matrix=Array.newInstance(type,sizes);		
		_rowCount=0;
		_colCount=0;
		_growth=10;
		_dim=sizes;
	}
	
	protected PrimitiveMatrix(Class type){
		this(type,new int[]{DEFAULT_SIZE,DEFAULT_SIZE});
	}
	
	protected synchronized void expandRows(int upTo){
		if (upTo<=_dim[0]) return;
		
		int old=_dim[0];
		_dim[0]=upTo*_growth;
		Object newMatrix=Array.newInstance(_type,_dim);
		for (int i=0;i<old;++i){
			Object oldRow=Array.get(_matrix, i);
			Object newRow=Array.get(newMatrix, i);
			System.arraycopy(oldRow,0,newRow,0,_dim[1]);
		}
		_growth*=10;
		_matrix=newMatrix;
	}
	
	protected synchronized void expandCols(int upTo){
		if (upTo<=_dim[1]) return;
		int old=_dim[1];
		_dim[1]=upTo*_growth;
		Object newMatrix=Array.newInstance(_type,_dim);
		for (int i=0;i<_dim[0];++i){
			Object oldRow=Array.get(_matrix, i);
			Object newRow=Array.get(newMatrix, i);
			System.arraycopy(oldRow,0,newRow,0,old);
		}
		_growth*=10;
		_matrix=newMatrix;
	}
	
	protected synchronized void expand(int rUpto,int cUpTo){		
		int[] newDim=new int[]{rUpto+_growth,cUpTo+_growth};
		Object newMatrix=Array.newInstance(_type,newDim);
		for (int i=0;i<_dim[0];++i){
			Object oldRow=Array.get(_matrix, i);
			Object newRow=Array.get(newMatrix, i);
			System.arraycopy(oldRow,0,newRow,0,_dim[1]);
		}
		_matrix=newMatrix;	
		_dim=newDim;
		_growth*=10;
	}
	
	public void ensureCapacity(int x,int y){
		_rowCount=Math.max(x,_rowCount);
		_colCount=Math.max(y, _colCount);
		
		if (_rowCount>=_dim[0] && _colCount>=_dim[1]){
			expand(x,y);
		}
		else if (_rowCount>=_dim[0]){
			expandRows(x);
		}
		else if (_colCount>=_dim[1]){
			expandCols(y);
		}
	}
		
	/**
	 * called to shrink the array size to the current # of elements to save memory.
	 *
	 */
	public synchronized void seal(){
		if (_dim[0] > _rowCount || _dim[1] > _colCount){	
			int[] newDim=new int[]{_dim[0],_dim[1]};
			Object newMatrix=Array.newInstance(_type,newDim);
			for (int i=0;i<newDim[0];++i){
				Object oldRow=Array.get(_matrix, i);
				Object newRow=Array.get(newMatrix, i);
				System.arraycopy(oldRow,0,newRow,0,newDim[1]);
			}
			_matrix=newMatrix;	
			_dim=newDim;
		}
		_growth=10;
	}
	
	public String toString(){
		StringBuffer buffer=new StringBuffer("{");
		for (int i=0;i<=_rowCount;++i){
			if (i!=0){
				buffer.append('\n');
			}
			Object row=Array.get(_matrix, i);
			for (int j=0;j<=_colCount;++j){
				if (j!=0){
					buffer.append(", ");
				}
				buffer.append(Array.get(row, j));
			}
		}
		buffer.append('}');
		return buffer.toString();
	}
}
