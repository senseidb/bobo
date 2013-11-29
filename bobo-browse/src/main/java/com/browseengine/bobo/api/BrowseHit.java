package com.browseengine.bobo.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Explanation;

/**
 * A hit from a browse
 */
public class BrowseHit implements Serializable {
  private static final long serialVersionUID = 1L;

  public static class BoboTerm implements Serializable {
    private static final long serialVersionUID = 1L;
    public String term;
    public Integer freq;
    public List<Integer> positions;
    public List<Integer> startOffsets;
    public List<Integer> endOffsets;
  }

  public static class SerializableField implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String name;

    /** Field's value */
    private Object fieldsData;

    public SerializableField(IndexableField field) {
      name = field.name();
      if (field.numericValue() != null) {
        fieldsData = field.numericValue();
      } else if (field.stringValue() != null) {
        fieldsData = field.stringValue();
      } else if (field.binaryValue() != null) {
        fieldsData = field.binaryValue().bytes;
      } else {
        throw new UnsupportedOperationException("Doesn't support this field type so far");
      }
    }

    public String name() {
      return name;
    }

    public String stringValue() {
      if (fieldsData instanceof String || fieldsData instanceof Number) {
        return fieldsData.toString();
      } else {
        return null;
      }
    }

    public Number numericValue() {
      if (fieldsData instanceof Number) {
        return (Number) fieldsData;
      } else {
        return null;
      }
    }

    public byte[] binaryValue() {
      if (fieldsData instanceof byte[]) {
        return (byte[]) fieldsData;
      } else {
        return null;
      }
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof SerializableField)) {
        return false;
      }
      SerializableField other = (SerializableField) o;
      if (!name.equals(other.name())) {
        return false;
      }
      String value = stringValue();
      if (value != null) {
        if (value.equals(other.stringValue())) {
          return true;
        }
        return false;
      }
      byte[] binValue = binaryValue();
      if (binValue != null) {
        return Arrays.equals(binValue, other.binaryValue());
      }
      return false;
    }
  }

  public static class SerializableExplanation implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private float value; // the value of this node
    private String description;
    private ArrayList<SerializableExplanation> details; // sub-explanations

    public SerializableExplanation(Explanation explanation) {
      setValue(explanation.getValue());
      setDescription(explanation.getDescription());
      Explanation[] details = explanation.getDetails();
      if (details == null) {
        return;
      }
      for (Explanation exp : details) {
        addDetail(new SerializableExplanation(exp));
      }
    }

    /** The value assigned to this explanation node. */
    public float getValue() {
      return value;
    }

    /** Sets the value assigned to this explanation node. */
    public void setValue(float value) {
      this.value = value;
    }

    /** A description of this explanation node. */
    public String getDescription() {
      return description;
    }

    /** Sets the description of this explanation node. */
    public void setDescription(String description) {
      this.description = description;
    }

    /** The sub-nodes of this explanation node. */
    public SerializableExplanation[] getDetails() {
      if (details == null) {
        return null;
      }
      return details.toArray(new SerializableExplanation[0]);
    }

    /** Adds a sub-node to this explanation node. */
    public void addDetail(SerializableExplanation detail) {
      if (details == null) {
        details = new ArrayList<SerializableExplanation>();
      }
      details.add(detail);
    }

    @Override
    public String toString() {
      return toString(0);
    }

    protected String toString(int depth) {
      StringBuilder buffer = new StringBuilder();
      for (int i = 0; i < depth; i++) {
        buffer.append("  ");
      }
      buffer.append(getValue() + " = " + getDescription());
      buffer.append("\n");

      SerializableExplanation[] details = getDetails();
      if (details != null) {
        for (int i = 0; i < details.length; i++) {
          buffer.append(details[i].toString(depth + 1));
        }
      }

      return buffer.toString();
    }
  }

  /**
   * Get the score
   * @return score
   * @see #setScore(float)
   */
  public float getScore() {
    return score;
  }

  /**
   * Get the field values
   * @param field field name
   * @return field value array
   * @see #getField(String)
   */
  public String[] getFields(String field) {
    return _fieldValues != null ? _fieldValues.get(field) : null;
  }

  /**
   * Get the raw field values
   * @param field field name
   * @return field value array
   * @see #getRawField(String)
   */
  public Object[] getRawFields(String field) {
    return _rawFieldValues != null ? _rawFieldValues.get(field) : null;
  }

  /**
   * Get the field value
   * @param field field name
   * @return field value
   * @see #getFields(String)
   */
  public String getField(String field) {
    String[] fields = getFields(field);
    if (fields != null && fields.length > 0) {
      return fields[0];
    } else {
      return null;
    }
  }

  /**
   * Get the raw field value
   * @param field field name
   * @return raw field value
   * @see #getRawFields(String)
   */
  public Object getRawField(String field) {
    Object[] fields = getRawFields(field);
    if (fields != null && fields.length > 0) {
      return fields[0];
    } else {
      return null;
    }
  }

  private float score;
  private int docid;

  private Map<String, String[]> _fieldValues;
  private Map<String, Object[]> _rawFieldValues;
  private transient Comparable<?> _comparable;
  private List<SerializableField> _storedFields;
  private int _groupPosition; // the position of the _groupField inside groupBy request.
  private String _groupField;
  private String _groupValue;
  private Object _rawGroupValue;
  private int _groupHitsCount;
  private BrowseHit[] _groupHits;
  private SerializableExplanation _explanation;

  private Map<String, List<BoboTerm>> _termVectorMap = new HashMap<String, List<BoboTerm>>();

  public Map<String, List<BoboTerm>> getTermVectorMap() {
    return _termVectorMap;
  }

  public BrowseHit setTermVectorMap(Map<String, List<BoboTerm>> termVectorMap) {
    _termVectorMap = termVectorMap;
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

  public SerializableExplanation getExplanation() {
    return _explanation;
  }

  public BrowseHit setExplanation(SerializableExplanation explanation) {
    _explanation = explanation;
    return this;
  }

  public BrowseHit setExplanation(Explanation explanation) {
    _explanation = new SerializableExplanation(explanation);
    return this;
  }

  public BrowseHit setComparable(Comparable<?> comparable) {
    _comparable = comparable;
    return this;
  }

  public Comparable<?> getComparable() {
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
  public Map<String, String[]> getFieldValues() {
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
  public Map<String, Object[]> getRawFieldValues() {
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

  public BrowseHit setStoredFields(Document doc) {
    if (doc == null) {
      _storedFields = null;
      return this;
    }
    _storedFields = new ArrayList<SerializableField>();
    Iterator<IndexableField> it = doc.iterator();
    while (it.hasNext()) {
      _storedFields.add(new SerializableField(it.next()));
    }
    return this;
  }

  public BrowseHit setStoredFields(List<SerializableField> fields) {
    _storedFields = fields;
    return this;
  }

  public List<SerializableField> getStoredFields() {
    return _storedFields;
  }

  public byte[] getFieldBinaryValue(String fieldName) {
    if (_storedFields == null) {
      return null;
    }
    for (SerializableField field : _storedFields) {
      if (fieldName.equals(field.name())) {
        return field.binaryValue();
      }
    }
    return null;
  }

  public String getFieldStringValue(String fieldName) {
    if (_storedFields == null) {
      return null;
    }
    for (SerializableField field : _storedFields) {
      if (fieldName.equals(field.name())) {
        return field.stringValue();
      }
    }
    return null;
  }

  public String toString(Map<String, String[]> map) {
    StringBuilder buffer = new StringBuilder();
    Set<Map.Entry<String, String[]>> set = map.entrySet();
    Iterator<Map.Entry<String, String[]>> iterator = set.iterator();
    while (iterator.hasNext()) {
      Map.Entry<String, String[]> e = iterator.next();
      buffer.append(e.getKey());
      buffer.append(":");
      String[] vals = e.getValue();
      buffer.append(vals == null ? null : Arrays.toString(vals));
      if (iterator.hasNext()) buffer.append(", ");
    }
    return buffer.toString();
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("docid: ").append(docid).append('\n');
    buffer.append("score: ").append(score).append('\n');
    buffer.append("field values: ").append(toString(_fieldValues)).append('\n');
    return buffer.toString();
  }
}
