package com.browseengine.bobo.facets.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.api.FieldValueAccessor;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermStringList;
import com.browseengine.bobo.facets.filter.FacetRangeFilter;
import com.browseengine.bobo.util.BigIntArray;
import com.browseengine.bobo.util.BigSegmentedArray;
import com.browseengine.bobo.util.IntBoundedPriorityQueue;
import com.browseengine.bobo.util.IntBoundedPriorityQueue.IntComparator;
import com.browseengine.bobo.util.LazyBigIntArray;

public class RangeFacetCountCollector implements FacetCountCollector {
  private final FacetSpec _ospec;
  protected BigSegmentedArray _count;
  private int _countlength;
  private final BigSegmentedArray _array;
  protected FacetDataCache<?> _dataCache;
  private final String _name;
  private final TermStringList _predefinedRanges;
  private int[][] _predefinedRangeIndexes;

  public RangeFacetCountCollector(String name, FacetDataCache<?> dataCache, int docBase,
      FacetSpec ospec, List<String> predefinedRanges) {
    _name = name;
    _dataCache = dataCache;
    _countlength = _dataCache.freqs.length;
    _count = new LazyBigIntArray(_countlength);
    _array = _dataCache.orderArray;
    _ospec = ospec;
    if (predefinedRanges != null) {
      _predefinedRanges = new TermStringList();
      Collections.sort(predefinedRanges);
      _predefinedRanges.addAll(predefinedRanges);
    } else {
      _predefinedRanges = null;
    }

    if (_predefinedRanges != null) {
      _predefinedRangeIndexes = new int[_predefinedRanges.size()][];
      int i = 0;
      for (String range : _predefinedRanges) {
        _predefinedRangeIndexes[i++] = FacetRangeFilter.parse(_dataCache, range);
      }
    }
  }

  /**
   * gets distribution of the value arrays. When predefined ranges are available, this returns distribution by predefined ranges.
   */
  @Override
  public BigSegmentedArray getCountDistribution() {
    BigSegmentedArray dist;
    if (_predefinedRangeIndexes != null) {
      dist = new LazyBigIntArray(_predefinedRangeIndexes.length);
      int n = 0;
      for (int[] range : _predefinedRangeIndexes) {
        int start = range[0];
        int end = range[1];

        int sum = 0;
        for (int i = start; i < end; ++i) {
          sum += _count.get(i);
        }
        dist.add(n++, sum);
      }
    } else {
      dist = _count;
    }

    return dist;
  }

  @Override
  public String getName() {
    return _name;
  }

  @Override
  public BrowseFacet getFacet(String value) {
    BrowseFacet facet = null;
    int[] range = FacetRangeFilter.parse(_dataCache, value);
    if (range != null) {
      int sum = 0;
      for (int i = range[0]; i <= range[1]; ++i) {
        sum += _count.get(i);
      }
      facet = new BrowseFacet(value, sum);
    }
    return facet;
  }

  @Override
  public int getFacetHitsCount(Object value) {
    int[] range = FacetRangeFilter.parse(_dataCache, (String) value);
    int sum = 0;
    if (range != null) {
      for (int i = range[0]; i <= range[1]; ++i) {
        sum += _count.get(i);
      }
    }
    return sum;
  }

  @Override
  public void collect(int docid) {
    int i = _array.get(docid);
    _count.add(i, _count.get(i) + 1);
  }

  @Override
  public final void collectAll() {
    _count = BigIntArray.fromArray(_dataCache.freqs);
    _countlength = _dataCache.freqs.length;
  }

  void convertFacets(BrowseFacet[] facets) {
    int i = 0;
    for (BrowseFacet facet : facets) {
      int hit = facet.getFacetValueHitCount();
      String val = facet.getValue();
      RangeFacet rangeFacet = new RangeFacet();
      rangeFacet.setValues(val, val);
      rangeFacet.setFacetValueHitCount(hit);
      facets[i++] = rangeFacet;
    }
  }

  @Override
  public List<BrowseFacet> getFacets() {
    if (_ospec != null) {
      if (_predefinedRangeIndexes != null) {
        int minCount = _ospec.getMinHitCount();
        // int maxNumOfFacets = _ospec.getMaxCount();
        // if (maxNumOfFacets <= 0 || maxNumOfFacets > _predefinedRangeIndexes.length)
        // maxNumOfFacets = _predefinedRangeIndexes.length;

        int[] rangeCount = new int[_predefinedRangeIndexes.length];

        for (int k = 0; k < _predefinedRangeIndexes.length; ++k) {
          int count = 0;
          int idx = _predefinedRangeIndexes[k][0];
          int end = _predefinedRangeIndexes[k][1];
          while (idx <= end) {
            count += _count.get(idx++);
          }
          rangeCount[k] = count;
        }

        List<BrowseFacet> facetColl = new ArrayList<BrowseFacet>(_predefinedRanges.size());
        for (int k = 0; k < _predefinedRangeIndexes.length; ++k) {
          if (rangeCount[k] >= minCount) {
            BrowseFacet choice = new BrowseFacet(_predefinedRanges.get(k), rangeCount[k]);
            facetColl.add(choice);
          }
          // if(facetColl.size() >= maxNumOfFacets) break;
        }
        return facetColl;
      } else {
        return FacetCountCollector.EMPTY_FACET_LIST;
      }
    } else {
      return FacetCountCollector.EMPTY_FACET_LIST;
    }
  }

  public List<BrowseFacet> getFacetsNew() {
    if (_ospec != null) {
      if (_predefinedRangeIndexes != null) {
        int minCount = _ospec.getMinHitCount();
        int maxNumOfFacets = _ospec.getMaxCount();
        if (maxNumOfFacets <= 0 || maxNumOfFacets > _predefinedRangeIndexes.length) maxNumOfFacets = _predefinedRangeIndexes.length;

        BigSegmentedArray rangeCount = new LazyBigIntArray(_predefinedRangeIndexes.length);

        for (int k = 0; k < _predefinedRangeIndexes.length; ++k) {
          int count = 0;
          int idx = _predefinedRangeIndexes[k][0];
          int end = _predefinedRangeIndexes[k][1];
          while (idx <= end) {
            count += _count.get(idx++);
          }
          rangeCount.add(k, count);
        }

        List<BrowseFacet> facetColl;
        FacetSortSpec sortspec = _ospec.getOrderBy();
        if (sortspec == FacetSortSpec.OrderValueAsc) {
          facetColl = new ArrayList<BrowseFacet>(maxNumOfFacets);
          for (int k = 0; k < _predefinedRangeIndexes.length; ++k) {
            if (rangeCount.get(k) >= minCount) {
              BrowseFacet choice = new BrowseFacet(_predefinedRanges.get(k), rangeCount.get(k));
              facetColl.add(choice);
            }
            if (facetColl.size() >= maxNumOfFacets) break;
          }
        } else // if (sortspec == FacetSortSpec.OrderHitsDesc)
        {
          ComparatorFactory comparatorFactory;
          if (sortspec == FacetSortSpec.OrderHitsDesc) {
            comparatorFactory = new FacetHitcountComparatorFactory();
          } else {
            comparatorFactory = _ospec.getCustomComparatorFactory();
          }

          if (comparatorFactory == null) {
            throw new IllegalArgumentException("facet comparator factory not specified");
          }

          final IntComparator comparator = comparatorFactory.newComparator(
            new FieldValueAccessor() {
              @Override
              public String getFormatedValue(int index) {
                return _predefinedRanges.get(index);
              }

              @Override
              public Object getRawValue(int index) {
                return _predefinedRanges.getRawValue(index);
              }
            }, rangeCount);

          final int forbidden = -1;
          IntBoundedPriorityQueue pq = new IntBoundedPriorityQueue(comparator, maxNumOfFacets,
              forbidden);
          for (int i = 0; i < _predefinedRangeIndexes.length; ++i) {
            if (rangeCount.get(i) >= minCount) pq.offer(i);
          }

          int val;
          facetColl = new LinkedList<BrowseFacet>();
          while ((val = pq.pollInt()) != forbidden) {
            BrowseFacet facet = new BrowseFacet(_predefinedRanges.get(val), rangeCount.get(val));
            ((LinkedList<BrowseFacet>) facetColl).addFirst(facet);
          }
        }
        return facetColl;
      } else {
        return FacetCountCollector.EMPTY_FACET_LIST;
      }
    } else {
      return FacetCountCollector.EMPTY_FACET_LIST;
    }
  }

  private static class RangeFacet extends BrowseFacet {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    String _lower;
    String _upper;

    RangeFacet() {
    }

    void setValues(String lower, String upper) {
      _lower = lower;
      _upper = upper;
      setValue(new StringBuilder("[").append(_lower).append(" TO ").append(_upper).append(']')
          .toString());
    }
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub
  }

  @Override
  public FacetIterator iterator() {
    if (_predefinedRanges != null) {
      BigSegmentedArray rangeCounts = new LazyBigIntArray(_predefinedRangeIndexes.length);
      for (int k = 0; k < _predefinedRangeIndexes.length; ++k) {
        int count = 0;
        int idx = _predefinedRangeIndexes[k][0];
        int end = _predefinedRangeIndexes[k][1];
        while (idx <= end) {
          count += _count.get(idx++);
        }
        rangeCounts.add(k, rangeCounts.get(k) + count);
      }
      return new DefaultFacetIterator(_predefinedRanges, rangeCounts, rangeCounts.size(), true);
    }
    return null;
  }
}
