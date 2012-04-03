package com.browseengine.bobo.facets.attribute;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.index.Term;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.data.MultiValueFacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.range.MultiRangeFacetHandler;

public class AttributesFacetHandler extends MultiRangeFacetHandler {
  public static final char DEFAULT_SEPARATOR = '=';
  private char separator;
  private int numFacetsPerKey = 7;
  public static final String SEPARATOR_PROP_NAME = "separator";
  public static final String MAX_FACETS_PER_KEY_PROP_NAME = "maxFacetsPerKey";
  
  public AttributesFacetHandler(String name, String indexFieldName, TermListFactory termListFactory, Term sizePayloadTerm, Map<String, String> facetProps) {
    super(name, indexFieldName, sizePayloadTerm, termListFactory, Collections.EMPTY_LIST);
    if (facetProps.containsKey(SEPARATOR_PROP_NAME)) {
      this.separator = narrow(facetProps.get(SEPARATOR_PROP_NAME)).charAt(0); 
    } else {
      this.separator = DEFAULT_SEPARATOR;
    }
    if (facetProps.containsKey(MAX_FACETS_PER_KEY_PROP_NAME)) {
      this.numFacetsPerKey = Integer.parseInt(narrow(facetProps.get(MAX_FACETS_PER_KEY_PROP_NAME))); 
    }
  }
  private String narrow(String string) {   
    return string.replaceAll("\\[", "").replaceAll("\\]", "");
  }
  public char getSeparator(BrowseSelection browseSelection) {
    if (browseSelection == null || !browseSelection.getSelectionProperties().containsKey(SEPARATOR_PROP_NAME)) {
      return separator;
    }
    return browseSelection.getSelectionProperties().get(SEPARATOR_PROP_NAME).toString().charAt(0);
  }
  
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties prop) throws IOException {    
    return super.buildRandomAccessFilter(convertToRangeString(value, separator), prop);
  }
  public static String convertToRangeString(String key, char separator) {
    if (key.startsWith("[") && key.contains(" TO ")) {
      return key;
    }
    return "[" + key + separator + " TO " + key + (char)(separator + 1) + ")";
  }
  @Override
  public RandomAccessFilter buildRandomAccessOrFilter(final String[] vals, Properties prop, boolean isNot) throws IOException {
    String[] ranges = new String [vals.length];
    for (int i = 0; i < vals.length; i++) {
      ranges[i] = convertToRangeString(vals[i], separator);
    }
    return super.buildRandomAccessOrFilter(ranges, prop, isNot);
  }
  
  
  
  public int getFacetsPerKey(BrowseSelection browseSelection) {
    if (browseSelection == null || !browseSelection.getSelectionProperties().containsKey(MAX_FACETS_PER_KEY_PROP_NAME)) {
      return numFacetsPerKey;
    }
    return Integer.valueOf(browseSelection.getSelectionProperties().get(MAX_FACETS_PER_KEY_PROP_NAME).toString());
  }
  
  @Override
  public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection browseSelection, final FacetSpec ospec){
   
    return new FacetCountCollectorSource(){
    
    @Override
    public FacetCountCollector getFacetCountCollector(
        BoboIndexReader reader, int docBase) {
      int facetsPerKey = getFacetsPerKey(browseSelection);
      if (ospec.getProperties() != null && ospec.getProperties().containsKey(MAX_FACETS_PER_KEY_PROP_NAME)) {
        facetsPerKey = Integer.parseInt(ospec.getProperties().get(MAX_FACETS_PER_KEY_PROP_NAME));
      }
      MultiValueFacetDataCache dataCache = (MultiValueFacetDataCache) reader.getFacetData(_name);
      return new AttributesFacetCountCollector(AttributesFacetHandler.this, _name,dataCache,docBase,browseSelection, ospec, facetsPerKey, getSeparator(browseSelection));
    }
  };
  }
}
