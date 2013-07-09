package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandler.FacetDataNone;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.sort.DocComparatorSource;

public class BucketFacetHandler extends FacetHandler<FacetDataNone> {
  private final Map<String, String[]> _predefinedBuckets;
  private final String _name;
  private final String _dependsOnFacetName;

  public BucketFacetHandler(String name, Map<String, String[]> predefinedBuckets,
      String dependsOnFacetName) {
    super(name, new HashSet<String>(Arrays.asList(new String[] { dependsOnFacetName })));
    _name = name;
    _predefinedBuckets = predefinedBuckets;
    _dependsOnFacetName = dependsOnFacetName;
  }

  @SuppressWarnings("unchecked")
  @Override
  public DocComparatorSource getDocComparatorSource() {
    FacetHandler<FacetDataCache<?>> dependOnFacetHandler = (FacetHandler<FacetDataCache<?>>) getDependedFacetHandler(_dependsOnFacetName);
    return dependOnFacetHandler.getDocComparatorSource();
  }

  @SuppressWarnings("unchecked")
  @Override
  public String[] getFieldValues(BoboSegmentReader reader, int id) {
    FacetHandler<FacetDataCache<?>> dependOnFacetHandler = (FacetHandler<FacetDataCache<?>>) getDependedFacetHandler(_dependsOnFacetName);
    return dependOnFacetHandler.getFieldValues(reader, id);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object[] getRawFieldValues(BoboSegmentReader reader, int id) {
    FacetHandler<FacetDataCache<?>> dependOnFacetHandler = (FacetHandler<FacetDataCache<?>>) getDependedFacetHandler(_dependsOnFacetName);
    return dependOnFacetHandler.getRawFieldValues(reader, id);
  }

  @SuppressWarnings("unchecked")
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String bucketString, Properties prop)
      throws IOException {
    FacetHandler<FacetDataCache<?>> dependOnFacetHandler = (FacetHandler<FacetDataCache<?>>) getDependedFacetHandler(_dependsOnFacetName);
    String[] elems = _predefinedBuckets.get(bucketString);
    if (elems == null || elems.length == 0) return EmptyFilter.getInstance();
    if (elems.length == 1) return dependOnFacetHandler.buildRandomAccessFilter(elems[0], prop);
    return dependOnFacetHandler.buildRandomAccessOrFilter(elems, prop, false);
  }

  @SuppressWarnings("unchecked")
  @Override
  public RandomAccessFilter buildRandomAccessAndFilter(String[] bucketStrings, Properties prop)
      throws IOException {
    List<RandomAccessFilter> filterList = new LinkedList<RandomAccessFilter>();
    FacetHandler<FacetDataCache<?>> dependOnFacetHandler = (FacetHandler<FacetDataCache<?>>) getDependedFacetHandler(_dependsOnFacetName);
    for (String bucketString : bucketStrings) {
      String[] vals = _predefinedBuckets.get(bucketString);
      RandomAccessFilter filter = dependOnFacetHandler.buildRandomAccessOrFilter(vals, prop, false);
      if (filter == EmptyFilter.getInstance()) return EmptyFilter.getInstance();
      filterList.add(filter);
    }
    if (filterList.size() == 0) return EmptyFilter.getInstance();
    if (filterList.size() == 1) return filterList.get(0);
    return new RandomAccessAndFilter(filterList);
  }

  @SuppressWarnings("unchecked")
  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] bucketStrings, Properties prop,
      boolean isNot) throws IOException {
    if (isNot) {
      RandomAccessFilter excludeFilter = buildRandomAccessAndFilter(bucketStrings, prop);
      return new RandomAccessNotFilter(excludeFilter);
    } else {
      FacetHandler<FacetDataCache<?>> dependOnFacetHandler = (FacetHandler<FacetDataCache<?>>) getDependedFacetHandler(_dependsOnFacetName);

      Set<String> selections = new HashSet<String>();
      for (String bucket : bucketStrings) {
        String[] vals = _predefinedBuckets.get(bucket);
        if (vals != null) {
          for (String val : vals) {
            selections.add(val);
          }
        }
      }
      if (selections != null && selections.size() > 0) {
        String[] sels = selections.toArray(new String[0]);
        if (selections.size() == 1) {
          return dependOnFacetHandler.buildRandomAccessFilter(sels[0], prop);
        } else {
          return dependOnFacetHandler.buildRandomAccessOrFilter(sels, prop, false);
        }
      } else {
        return EmptyFilter.getInstance();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public int getNumItems(BoboSegmentReader reader, int id) {
    FacetHandler<FacetDataCache<?>> dependOnFacetHandler = (FacetHandler<FacetDataCache<?>>) getDependedFacetHandler(_dependsOnFacetName);
    FacetDataCache<?> data = dependOnFacetHandler.getFacetData(reader);
    return data.getNumItems(id);
  }

  @Override
  public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel,
      final FacetSpec ospec) {
    return new FacetCountCollectorSource() {
      @SuppressWarnings("unchecked")
      @Override
      public FacetCountCollector getFacetCountCollector(BoboSegmentReader reader, int docBase) {
        FacetHandler<FacetDataCache<?>> dependOnFacetHandler = (FacetHandler<FacetDataCache<?>>) getDependedFacetHandler(_dependsOnFacetName);

        FacetCountCollector defaultCollector = dependOnFacetHandler.getFacetCountCollectorSource(
          sel, ospec).getFacetCountCollector(reader, docBase);
        if (defaultCollector instanceof DefaultFacetCountCollector) {
          return new BucketFacetCountCollector(_name,
              (DefaultFacetCountCollector) defaultCollector, ospec, _predefinedBuckets,
              reader.numDocs());
        } else {
          throw new IllegalStateException("dependent facet handler must build "
              + DefaultFacetCountCollector.class);
        }
      }
    };

  }

  @Override
  public FacetDataNone load(BoboSegmentReader reader) throws IOException {
    return FacetDataNone.instance;
  }
}
