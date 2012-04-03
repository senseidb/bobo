package com.browseengine.bobo.facets.range;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.index.Term;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BoboIndexReader.WorkArea;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.FacetRangeFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.impl.MultiValueFacetHandler.MultiValueDocScorer;
import com.browseengine.bobo.facets.impl.RangeFacetCountCollector;
import com.browseengine.bobo.facets.impl.RangeFacetHandler;
import com.browseengine.bobo.query.scoring.BoboDocScorer;
import com.browseengine.bobo.query.scoring.FacetTermScoringFunctionFactory;
import com.browseengine.bobo.sort.DocComparatorSource;
import com.browseengine.bobo.util.BigNestedIntArray;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class MultiRangeFacetHandler extends RangeFacetHandler {

  private final Term sizePayloadTerm;
  private int maxItems = BigNestedIntArray.MAX_ITEMS;

  public MultiRangeFacetHandler(String name, String indexFieldName, Term sizePayloadTerm,
      TermListFactory termListFactory, List<String> predefinedRanges) {
    super(name, indexFieldName, termListFactory, predefinedRanges);
    this.sizePayloadTerm = sizePayloadTerm;
  }

  @Override
  public DocComparatorSource getDocComparatorSource() {
    return new MultiValueFacetDataCache.MultiFacetDocComparatorSource(new MultiDataCacheBuilder(getName()));
  }

  @Override
  public String[] getFieldValues(BoboIndexReader reader, int id) {
    MultiValueFacetDataCache dataCache = getFacetData(reader);
    if (dataCache != null) {
      return dataCache._nestedArray.getTranslatedData(id, dataCache.valArray);
    }
    return new String[0];
  }

  @Override
  public Object[] getRawFieldValues(BoboIndexReader reader, int id) {

    MultiValueFacetDataCache dataCache = getFacetData(reader);
    if (dataCache != null) {
      return dataCache._nestedArray.getRawData(id, dataCache.valArray);
    }
    return new String[0];
  }

  public MultiValueFacetDataCache getFacetData(BoboIndexReader reader) {
    return (MultiValueFacetDataCache) reader.getFacetData(_name);
  }

  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop) throws IOException {
    return new FacetRangeFilter(this, value);
  }

  @Override
  public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel, final FacetSpec ospec) {
    return new FacetCountCollectorSource() {

      @Override
      public FacetCountCollector getFacetCountCollector(BoboIndexReader reader, int docBase) {
        MultiValueFacetDataCache dataCache = getFacetData(reader);
        final BigNestedIntArray _nestedArray = dataCache._nestedArray;
        return new RangeFacetCountCollector(_name, dataCache, docBase, ospec, _predefinedRanges) {
          public void collect(int docid) {
            _nestedArray.countNoReturn(docid, _count);
          }
        };
      }
    };
  }

  @Override
  public BoboDocScorer getDocScorer(BoboIndexReader reader, FacetTermScoringFunctionFactory scoringFunctionFactory,
      Map<String, Float> boostMap) {
    MultiValueFacetDataCache dataCache = getFacetData(reader);
    float[] boostList = BoboDocScorer.buildBoostList(dataCache.valArray, boostMap);
    return new MultiValueDocScorer(dataCache, scoringFunctionFactory, boostList);
  }

  @Override
  public MultiValueFacetDataCache load(BoboIndexReader reader) throws IOException {
    return load(reader, new WorkArea());
  }

  @Override
  public MultiValueFacetDataCache load(BoboIndexReader reader, WorkArea workArea) throws IOException {
    MultiValueFacetDataCache dataCache = new MultiValueFacetDataCache();
    dataCache.setMaxItems(maxItems);
    if (sizePayloadTerm == null) {
      dataCache.load(_indexFieldName, reader, _termListFactory, workArea);
    } else {
      dataCache.load(_indexFieldName, reader, _termListFactory, sizePayloadTerm);
    }
    return dataCache;
  }

  public void setMaxItems(int maxItems) {
    this.maxItems = maxItems;
  }
}
