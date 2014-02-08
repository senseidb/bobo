package com.browseengine.bobo.facets.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TermStringList extends TermValueList<String> {
  private String sanity = null;
  private boolean withDummy = true;

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
    if (sanity != null && sanity.compareTo(o) >= 0) throw new RuntimeException(
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
      if (o == null) {
        return -1;
      }
      if (o.equals("")) {
        if (_innerList.size() > 1 && "".equals(_innerList.get(1))) {
          return 1;
        } else {
          // the key is not contained in the list return (-(insertion point) - 1)
          // the insertion point of empty string should be 1, 0 is for dummy header
          return -2;
        }
      }
      return Collections.binarySearch(((ArrayList<String>) _innerList), (String) o);
    } else {
      return Collections.binarySearch(((ArrayList<String>) _innerList), (String) o);
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
      if (val == null) {
        return false;
      }
      if (val.equals("")) {
        return _innerList.size() > 1 && "".equals(_innerList.get(1));
      }
      return Collections.binarySearch(((ArrayList<String>) _innerList), val) >= 0;
    } else {
      return Collections.binarySearch(((ArrayList<String>) _innerList), val) >= 0;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public int indexOfWithType(String o) {
    if (withDummy) {
      if (o == null) {
        return -1;
      }
      if (o.equals("")) {
        if (_innerList.size() > 1 && "".equals(_innerList.get(1))) {
          return 1;
        } else {
          // the key is not contained in the list return (-(insertion point) - 1)
          // the insertion point of empty string should be 1, 0 is for dummy header
          return -2;
        }
      }
      return Collections.binarySearch(((ArrayList<String>) _innerList), o);
    } else {
      return Collections.binarySearch(((ArrayList<String>) _innerList), o);
    }
  }

}
