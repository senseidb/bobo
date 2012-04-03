package com.browseengine.bobo.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
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
	
	public static class TermFrequencyVector implements Serializable{

	  private static final long serialVersionUID = 1L;
	  public final String[] terms;
	  public final int[] freqs;
	  
	  public TermFrequencyVector(String[] terms,int[] freqs){
	    this.terms = terms;
	    this.freqs = freqs;
	  }
	}

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
	private transient Comparable<?> _comparable;
	private Document _storedFields;
  private int _groupPosition; // the position of the _groupField inside groupBy request.
  private String _groupField;
  private String _groupValue;
  private Object _rawGroupValue;
  private int _groupHitsCount;
  private BrowseHit[] _groupHits;
	private Explanation _explanation;
	
	private Map<String,TermFrequencyVector> _termFreqMap = new HashMap<String,TermFrequencyVector>();
	
	public Map<String,TermFrequencyVector> getTermFreqMap(){
	  return _termFreqMap;
	}
	
	public BrowseHit setTermFreqMap(Map<String, TermFrequencyVector> termFreqMap){
	  _termFreqMap = termFreqMap;
    return this;
	}

    public int getGroupPosition() {
      return _groupPosition;
    }

    public BrowseHit setGroupPosition(int pos) {
      _groupPosition = pos;
      return this;
    }

    public String getGroupField() {
      return _groupField;
    }

    public BrowseHit setGroupField(String field) {
      _groupField = field;
      return this;
    }

    public String getGroupValue() {
      return _groupValue;
    }

    public BrowseHit setGroupValue(String group) {
      _groupValue = group;
      return this;
    }

    public Object getRawGroupValue() {
      return _rawGroupValue;
    }

    public BrowseHit setRawGroupValue(Object group) {
      _rawGroupValue = group;
      return this;
    }

    public int getGroupHitsCount() {
      return _groupHitsCount;
    }

  public BrowseHit setGroupHitsCount(int count) {
    _groupHitsCount = count;
    return this;
  }

  public BrowseHit[] getGroupHits() {
    return _groupHits;
  }

  public BrowseHit setGroupHits(BrowseHit[] hits) {
    _groupHits = hits;
    return this;
  }
	
	public Explanation getExplanation() {
		return _explanation;
	}

	public BrowseHit setExplanation(Explanation explanation) {
		_explanation = explanation;
    return this;
	}

	public BrowseHit setComparable(Comparable<?> comparable)
	{
	  _comparable = comparable;
    return this;
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
	 *
   * @param docid document id
   * @see #getDocid()
	 */
	public BrowseHit setDocid(int docid) {
		this.docid = docid;
    return this;
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
	 *
   * @param rawFieldValues raw field value map
   * @see #getRawFieldValues()
	 */
	public BrowseHit setRawFieldValues(Map<String, Object[]> rawFieldValues) {
		_rawFieldValues = rawFieldValues;
    return this;
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
	 *
   * @param fieldValues field value map
   * @see #getFieldValues()
	 */
	public BrowseHit setFieldValues(Map<String, String[]> fieldValues) {
		_fieldValues = fieldValues;
    return this;
	}
	
	/**
	 * Sets the score
	 *
   * @param score score
   * @see #getScore()
	 */
	public BrowseHit setScore(float score) {
		this.score = score;
    return this;
	}
	
	public BrowseHit setStoredFields(Document doc){
		_storedFields = doc;
    return this;
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
