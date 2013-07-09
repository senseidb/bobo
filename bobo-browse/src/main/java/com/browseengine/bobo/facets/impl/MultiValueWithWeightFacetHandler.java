package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.Properties;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.api.BoboSegmentReader.WorkArea;
import com.browseengine.bobo.facets.data.MultiValueWithWeightFacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.MultiValueFacetFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.range.MultiDataCacheBuilder;

public class MultiValueWithWeightFacetHandler extends MultiValueFacetHandler {
  public MultiValueWithWeightFacetHandler(String name, String indexFieldName,
      TermListFactory<?> termListFactory) {
    super(name, indexFieldName, termListFactory, null, null);
  }

  public MultiValueWithWeightFacetHandler(String name, String indexFieldName) {
    super(name, indexFieldName, null, null, null);
  }

  public MultiValueWithWeightFacetHandler(String name) {
    super(name, name, null, null, null);
  }

  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop)
      throws IOException {
    MultiValueFacetFilter f = new MultiValueFacetFilter(new MultiDataCacheBuilder(getName(),
        _indexFieldName), value);
    return f;
  }

  @SuppressWarnings("unchecked")
  @Override
  public MultiValueWithWeightFacetDataCache<?> load(BoboSegmentReader reader, WorkArea workArea)
      throws IOException {
    @SuppressWarnings("rawtypes")
    MultiValueWithWeightFacetDataCache<?> dataCache = new MultiValueWithWeightFacetDataCache();

    dataCache.setMaxItems(_maxItems);

    if (_sizePayloadTerm == null) {
      dataCache.load(_indexFieldName, reader, _termListFactory, workArea);
    } else {
      dataCache.load(_indexFieldName, reader, _termListFactory, _sizePayloadTerm);
    }
    return dataCache;
  }
}
