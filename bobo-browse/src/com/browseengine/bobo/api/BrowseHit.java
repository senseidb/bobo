package com.browseengine.bobo.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;

/**
 * A hit from a browse
 */
public class BrowseHit
	implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * Get the score
	 * @return score
	 * @see #setScore(float)
	 */
	public float getScore()
	{
		return score;
	}
	
	/**
	 * Get the field values
	 * @param field field name
	 * @return field value array
	 * @see #getField(String)
	 */
	public String[] getFields(String field)
	{
		return _fieldValues != null ? _fieldValues.get(field) : null;
	}
	
	/**
	 * Get the field value
	 * @param field field name
	 * @return field value
	 * @see #getFields(String)
	 */
	public String getField(String field)
	{
		String[] fields=getFields(field);
		if (fields!=null && fields.length > 0)
		{
			return fields[0];
		}
		else
		{
			return null;
		}
	}
	                       

	private float score;
	private int docid;
	
	private Map<String,String[]> _fieldValues;
	private Comparable<?> _comparable;
	private Document _storedFields;
	
	public void setComparable(Comparable<?> comparable)
	{
	  _comparable = comparable;
	}
	
	public Comparable<?> getComparable()
	{
	  return _comparable;
	}
	
	/**
	 * Gets the internal document id
	 * @return document id
	 * @see #setDocid(int)
	 */
	public int getDocid() {
		return docid;
	}
	
	/**
	 * Sets the internal document id
	 * @param docid document id
	 * @see #getDocid()
	 */
	public void setDocid(int docid) {
		this.docid = docid;
	}
	
	/**
	 * Gets the field values
	 * @return field value map
	 * @see #setFieldValues(Map)
	 */
	public Map<String,String[]> getFieldValues() {
		return _fieldValues;
	}
	
	/**
	 * Sets the field value map
	 * @param fieldValues field value map
	 * @see #getFieldValues()
	 */
	public void setFieldValues(Map<String,String[]> fieldValues) {
		_fieldValues = fieldValues;
	}
	
	/**
	 * Sets the score
	 * @param score score
	 * @see #getScore()
	 */
	public void setScore(float score) {
		this.score = score;
	}
	
	public void setStoredFields(Document doc){
		_storedFields = doc;
	}
	
	public Document getStoredFields(){
		return _storedFields;
	}
	
	public String toString(Map<String, String[]> map)
	  {
	    StringBuilder buffer = new StringBuilder();
	    Set<Map.Entry<String, String[]>> set = map.entrySet();
	    Iterator<Map.Entry<String, String[]>> iterator = set.iterator();
	    while (iterator.hasNext()) {
	      Map.Entry<String, String[]> e = iterator.next();
	      buffer.append(e.getKey());
	      buffer.append(":");
	      buffer.append(Arrays.asList(e.getValue()));
	      if (iterator.hasNext()) buffer.append(", ");
	    }
	    return buffer.toString();
	  }
	
	@Override
	public String toString() {
		StringBuffer buffer=new StringBuffer();
		buffer.append("docid: ").append(docid);
		buffer.append("score: ").append(score).append('\n');
		buffer.append("field values: ").append(toString(_fieldValues)).append('\n');
		return buffer.toString();
	}
}
