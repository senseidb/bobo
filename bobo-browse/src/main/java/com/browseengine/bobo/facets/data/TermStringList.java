package com.browseengine.bobo.facets.data;

import com.ibm.icu.text.UTF16;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TermStringList extends TermValueList<String> {
  private String sanity = null;
  private boolean withDummy = true;

  /**
   * A string comparator that orders Java strings according to Unicode codepoint
   * order, same as Lucene, and not code unit order as done by the String class.
   */
  private static final Comparator<String> STRING_COMPARATOR =
    new UTF16.StringComparator(true, false, 0);

  public TermStringList(int capacity) {
    super(capacity);
  }

  public TermStringList() {
    this(-1);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean add(String o) {
    if (_innerList.size() == 0 && o != null) withDummy = false; // the first value added is not null
    if (o == null) o = "";
    if (sanity != null && STRING_COMPARATOR.compare(sanity, o) >= 0) throw new RuntimeException(
        "Values need to be added in ascending order. Previous value: " + sanity + " adding value: "
            + o);
    if (_innerList.size() > 0 || !withDummy) sanity = o;
    return ((List<String>) _innerList).add(o);
  }

  @Override
  protected List<?> buildPrimitiveList(int capacity) {
    _type = String.class;
    if (capacity < 0) {
      return new ArrayList<String>();
    } else {
      return new ArrayList<String>(capacity);
    }
  }

  @Override
  public boolean contains(Object o) {
    if (withDummy) {
      return indexOf(o) > 0;
    } else {
      return indexOf(o) >= 0;
    }
  }

  @Override
  public String format(Object o) {
    return (String) o;
  }

  @SuppressWarnings("unchecked")
  @Override
  public int indexOf(Object o) {
    if (withDummy) {
      if (o == null) return -1;

      if (o.equals("")) {
        if (_innerList.size() > 1 && "".equals(_innerList.get(1))) {
          return 1;
        } else if (_innerList.size() < 2) {
          return -1;
        }
      }
      return Collections.binarySearch(((ArrayList<String>) _innerList), (String) o, STRING_COMPARATOR);
    } else {
      return Collections.binarySearch(((ArrayList<String>) _innerList), (String) o, STRING_COMPARATOR);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void seal() {
    ((ArrayList<String>) _innerList).trimToSize();
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean containsWithType(String val) {
    if (withDummy) {
      if (val == null) return false;
      if (val.equals("")) {
        return _innerList.size() > 1 && "".equals(_innerList.get(1));
      }
      return Collections.binarySearch(((ArrayList<String>) _innerList), val, STRING_COMPARATOR) >= 0;
    } else {
      return Collections.binarySearch(((ArrayList<String>) _innerList), val, STRING_COMPARATOR) >= 0;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public int indexOfWithType(String o) {
    if (withDummy) {
      if (o == null) return -1;
      if (o.equals("")) {
        if (_innerList.size() > 1 && "".equals(_innerList.get(1))) {
          return 1;
        } else if (_innerList.size() < 2) {
          return -1;
        }
      }
      return Collections.binarySearch(((ArrayList<String>) _innerList), o, STRING_COMPARATOR);
    } else {
      return Collections.binarySearch(((ArrayList<String>) _innerList), o, STRING_COMPARATOR);
    }
  }

}
