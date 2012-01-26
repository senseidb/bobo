package com.browseengine.bobo.facets.range;

import org.apache.lucene.util.OpenBitSet;

import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.filter.FacetValueConverter;

public class ValueConverterBitSetBuilder implements BitSetBuilder {
  private final FacetValueConverter facetValueConverter;
  private final String[] vals;
  private final boolean takeCompliment;

  public ValueConverterBitSetBuilder(FacetValueConverter facetValueConverter, String[] vals,boolean takeCompliment) {
    this.facetValueConverter = facetValueConverter;
    this.vals = vals;
    this.takeCompliment = takeCompliment;    
  }

  @Override
  public OpenBitSet bitSet(FacetDataCache dataCache) {
    int[] index = facetValueConverter.convert(dataCache, vals);
    
    OpenBitSet bitset = new OpenBitSet(dataCache.valArray.size());
    for (int i : index) {
      bitset.fastSet(i);
    }
    if (takeCompliment)
    {
      // flip the bits
      for (int i=0; i < index.length; ++i){
        bitset.fastFlip(i);
      }
    }
    return bitset;
  }

}
