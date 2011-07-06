package com.browseengine.bobo.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Explanation;

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
	 * Get the raw field values
	 * @param field field name
	 * @return field value array
	 * @see #getRawField(String)
	 */
	public Object[] getRawFields(String field)
	{
		return _rawFieldValues != null ? _rawFieldValues.get(field) : null;
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
	
	/**
	 * Get the raw field value
	 * @param field field name
	 * @return raw field value
	 * @see #getRawFields(String)
	 */
	public Object getRawField(String field)
	{
		Object[] fields=getRawFields(field);
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
	private Map<String,Object[]> _rawFieldValues;
	private Comparable<?> _comparable;
	private Document _storedFields;
  private String _groupValue;
  private int _groupHitsCount;
	private Explanation _explanation;

  public String getGroupValue() {
    return _groupValue;
  }

  public void setGroupValue(String group) {
    _groupValue = group;
  }

  public int getGroupHitsCount() {
    return _groupHitsCount;
  }

  public void setGroupHitsCount(int count) {
    _groupHitsCount = count;
  }
	
	public Explanation getExplanation() {
		return _explanation;
	}

	public void setExplanation(Explanation explanation) {
		_explanation = explanation;
	}

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
	 * Sets the raw field value map
	 * @param rawFieldValues raw field value map
	 * @see #getRawFieldValues()
	 */
	public void setRawFieldValues(Map<String,Object[]> rawFieldValues) {
		_rawFieldValues = rawFieldValues;
	}
	
	/**
	 * Gets the raw field values
	 * @return raw field value map
	 * @see #setRawFieldValues(Map)
	 */
	public Map<String,Object[]> getRawFieldValues() {
		return _rawFieldValues;
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
	      String[] vals = e.getValue();
	      buffer.append(vals == null ? null: Arrays.toString(vals));
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
