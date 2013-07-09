package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.search.Explanation;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.BitSetFilter;
import com.browseengine.bobo.facets.filter.FacetRangeFilter;
import com.browseengine.bobo.facets.filter.FacetRangeFilter.FacetRangeValueConverter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.facets.range.SimpleDataCacheBuilder;
import com.browseengine.bobo.facets.range.ValueConverterBitSetBuilder;
import com.browseengine.bobo.query.scoring.BoboDocScorer;
import com.browseengine.bobo.query.scoring.FacetScoreable;
import com.browseengine.bobo.query.scoring.FacetTermScoringFunctionFactory;
import com.browseengine.bobo.sort.DocComparatorSource;

public class RangeFacetHandler extends FacetHandler<FacetDataCache<?>> implements FacetScoreable {
  protected final String _indexFieldName;
  @SuppressWarnings("rawtypes")
  protected final TermListFactory _termListFactory;
  protected final List<String> _predefinedRanges;

  public RangeFacetHandler(String name, String indexFieldName, TermListFactory<?> termListFactory,
      List<String> predefinedRanges) {
    super(name);
    _indexFieldName = indexFieldName;
    _termListFactory = termListFactory;
    _predefinedRanges = predefinedRanges;
  }

  public RangeFacetHandler(String name, TermListFactory<?> termListFactory,
      List<String> predefinedRanges) {
    this(name, name, termListFactory, predefinedRanges);
  }

  public RangeFacetHandler(String name, List<String> predefinedRanges) {
    this(name, name, null, predefinedRanges);
  }

  public RangeFacetHandler(String name, String indexFieldName, List<String> predefinedRanges) {
    this(name, indexFieldName, null, predefinedRanges);
  }

  @Override
  public DocComparatorSource getDocComparatorSource() {
    return new FacetDataCache.FacetDocComparatorSource(this);
  }

  @Override
  public int getNumItems(BoboSegmentReader reader, int id) {
    FacetDataCache<?> data = getFacetData(reader);
    if (data == null) return 0;
    return data.getNumItems(id);
  }

  @Override
  public String[] getFieldValues(BoboSegmentReader reader, int id) {
    FacetDataCache<?> dataCache = getFacetData(reader);
    if (dataCache != null) {
      return new String[] { dataCache.valArray.get(dataCache.orderArray.get(id)) };
    }
    return new String[0];
  }

  @Override
  public Object[] getRawFieldValues(BoboSegmentReader reader, int id) {
    FacetDataCache<?> dataCache = getFacetData(reader);
    if (dataCache != null) {
      return new Object[] { dataCache.valArray.getRawValue(dataCache.orderArray.get(id)) };
    }
    return new String[0];
  }

  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop)
      throws IOException {
    return new FacetRangeFilter(this, value);
  }

  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(String[] vals, Properties prop, boolean isNot)
      throws IOException {
    if (vals.length > 1) {
      return new BitSetFilter(new ValueConverterBitSetBuilder(FacetRangeValueConverter.instance,
          vals, isNot), new SimpleDataCacheBuilder(getName(), _indexFieldName));
    } else {
      RandomAccessFilter filter = buildRandomAccessFilter(vals[0], prop);
      if (filter == null) return filter;
      if (isNot) {
        filter = new RandomAccessNotFilter(filter);
      }
      return filter;
    }
  }

  @Override
  public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel,
      final FacetSpec ospec) {
    return new FacetCountCollectorSource() {

      @Override
      public FacetCountCollector getFacetCountCollector(BoboSegmentReader reader, int docBase) {
        FacetDataCache<?> dataCache = getFacetData(reader);
        return new RangeFacetCountCollector(_name, dataCache, docBase, ospec, _predefinedRanges);
      }
    };

  }

  public boolean hasPredefinedRanges() {
    return (_predefinedRanges != null);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  public FacetDataCache<?> load(BoboSegmentReader reader) throws IOException {
    FacetDataCache<?> dataCache = new FacetDataCache();
    dataCache.load(_indexFieldName, reader, _termListFactory);
    return dataCache;
  }

  @Override
  public BoboDocScorer getDocScorer(BoboSegmentReader reader,
      FacetTermScoringFunctionFactory scoringFunctionFactory, Map<String, Float> boostMap) {
    FacetDataCache<?> dataCache = getFacetData(reader);
    float[] boostList = BoboDocScorer.buildBoostList(dataCache.valArray, boostMap);
    return new RangeBoboDocScorer(dataCache, scoringFunctionFactory, boostList);
  }

  public static final class RangeBoboDocScorer extends BoboDocScorer {
    private final FacetDataCache<?> _dataCache;

    public RangeBoboDocScorer(FacetDataCache<?> dataCache,
        FacetTermScoringFunctionFactory scoreFunctionFactory, float[] boostList) {
      super(scoreFunctionFactory.getFacetTermScoringFunction(dataCache.valArray.size(),
        dataCache.orderArray.size()), boostList);
      _dataCache = dataCache;
    }

    @Override
    public Explanation explain(int doc) {
      int idx = _dataCache.orderArray.get(doc);
      return _function.explain(_dataCache.freqs[idx], _boostList[idx]);
    }

    @Override
    public final float score(int docid) {
      int idx = _dataCache.orderArray.get(docid);
      return _function.score(_dataCache.freqs[idx], _boostList[idx]);
    }
  }
}
