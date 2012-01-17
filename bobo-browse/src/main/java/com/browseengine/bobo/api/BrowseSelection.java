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

package com.browseengine.bobo.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import com.browseengine.bobo.facets.impl.PathFacetHandler;

/**
 * Browse selection.
 */
public class BrowseSelection implements Serializable{
	
	/**
	 * Sets how selections filter given multiple of selections within one field
	 */
	public static enum ValueOperation{
		ValueOperationOr,
		ValueOperationAnd;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private ValueOperation selectionOperation;
	
	private String fieldName;	
	
	protected ArrayList<String> values;
	protected ArrayList<String> notValues;
	
	private Properties _selectionProperties;
	
	public void setSelectionProperty(String key,String val){
		_selectionProperties.setProperty(key, val);
	}
	
	public void setSelectionProperties(Map<String,String> props){
		_selectionProperties.putAll(props);
	}
	
	/**
	 * Gets if strict applied for counting. Used if the field is of type <b><i>path</i></b>.
	 * @return is strict applied
	 * @see #setStrict(boolean)
	 * @deprecated use {@link #getSelectionProperties()}
	 */
	public boolean isStrict() {
		return Boolean.valueOf(_selectionProperties.getProperty(PathFacetHandler.SEL_PROP_NAME_STRICT));
	}

	/**
	 * Sets is strict applied for counting. Used if the field is of type <b><i>path</i></b>.
	 * @param strict is strict applied
	 * @see #isStrict()
	 * @deprecated use {@link #setSelectionOperation(ValueOperation)}
	 */
	public BrowseSelection setStrict(boolean strict) {
	  _selectionProperties.setProperty(PathFacetHandler.SEL_PROP_NAME_STRICT, String.valueOf(strict));
    return this;
	}
	
	/**
	 * Gets the depth.  Used if the field is of type <b><i>path</i></b>.
	 * @return depth
	 * @see #setDepth(int)
	 * @deprecated use {@link #getSelectionProperties()}
	 */
	public int getDepth() {
	  try
	  {
	    return Integer.parseInt(_selectionProperties.getProperty(PathFacetHandler.SEL_PROP_NAME_DEPTH));
	  }
	  catch(Exception e)
	  {
	    return 0;
	  }
	}

	/**
	 * Sets the depth.  Used if the field is of type <b><i>path</i></b>.
	 * @param depth depth
	 * @see #getDepth()
	 * @deprecated use {@link #getSelectionProperties()}
	 */
	public BrowseSelection setDepth(int depth) {
	  _selectionProperties.setProperty(PathFacetHandler.SEL_PROP_NAME_DEPTH, String.valueOf(depth));
    return this;
	}
	
	/**
	 * Gets the field name
	 * @return field name
	 */
	public String getFieldName(){
		return fieldName;
	}
	
	/**
	 * Gets the selected values
	 * @return selected values
	 * @see #setValues(String[])
	 */
	public String[] getValues(){		
		return values.toArray(new String[values.size()]);		
	}
	
	/**
	 * Gets the selected NOT values
	 * @return selected NOT values
	 */
	public String[] getNotValues(){
		return notValues.toArray(new String[notValues.size()]);
	}
	
	/**
	 * Sets the selected values
	 * @param vals selected values
	 * @see #getValues()
	 */
	public BrowseSelection setValues(String[] vals){
		values.clear();
		for (int i=0;i<vals.length;++i){
			values.add(vals[i]);
		}
    return this;
	}

	/**
	 * Add a select value
	 * @param val select value
	 */
	public BrowseSelection addValue(String val){
		values.add(val);
    return this;
	}
	
	/**
	 * Add a select NOT value
	 * @param val select NOT value
	 */
	public BrowseSelection addNotValue(String val){
		notValues.add(val);
    return this;
	}
	
	/**
	 * Sets the NOT values
   * @param notVals NOT values
   */
	public BrowseSelection setNotValues(String[] notVals){
		notValues.clear();
		for (int i=0;i<notVals.length;++i){
			notValues.add(notVals[i]);
		}
    return this;
	}
	
	/**
	 * Constructor
	 * @param fieldName field name
	 */
	public BrowseSelection(String fieldName) {
		super();
		this.fieldName=fieldName;
		values=new ArrayList<String>();
		notValues=new ArrayList<String>();
		selectionOperation=ValueOperation.ValueOperationOr;
		_selectionProperties = new Properties();
	}
	
	public Properties getSelectionProperties()
	{
	  return _selectionProperties;
	}

	/**
	 * Gets value operation.
	 * @return value operation
	 * @see BrowseSelection#setSelectionOperation(ValueOperation)
	 */
	public ValueOperation getSelectionOperation() {
		return selectionOperation;
	}

	/**
	 * Sets value operation
	 *
   * @param selectionOperation value operation
   * @see #getSelectionOperation()
	 */
	public BrowseSelection setSelectionOperation(ValueOperation selectionOperation) {
		this.selectionOperation = selectionOperation;
    return this;
	}

	@Override
	public String toString() {
		StringBuffer buf=new StringBuffer();
		buf.append("name: ").append(fieldName);
		buf.append("values: "+values);
		buf.append("nots: "+notValues);
		buf.append("op: "+selectionOperation);
		buf.append("sel props: "+_selectionProperties);
		return buf.toString();
	}
	
}
