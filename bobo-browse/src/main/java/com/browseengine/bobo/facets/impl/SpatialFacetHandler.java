/**
 *
 */
package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.search.Filter;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermValueList;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.SpatialFacetFilter;
import com.browseengine.bobo.facets.impl.GeoFacetCountCollector.GeoRange;
import com.browseengine.bobo.sort.DocComparatorSource;
import com.browseengine.bobo.util.BigSegmentedArray;
import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.distance.DistanceUtils;

/** Constructor for SpatialFacetHandler
 * @param name - name of the Spatial facet
 *
 */
public class SpatialFacetHandler extends FacetHandler<FacetDataCache<?>> {

  private final String longitude;
  private final String latitude;
  private final SpatialStrategy spatialStrategy;

  public SpatialFacetHandler(String name, String lonFieldName, String latFieldName,
      String geoFieldName, int geoHashPrefixTreeMaxLevels) {
    super(name);
    longitude = lonFieldName;
    latitude = latFieldName;
    SpatialPrefixTree grid = new GeohashPrefixTree(SpatialContext.GEO, geoHashPrefixTreeMaxLevels);
    spatialStrategy = new RecursivePrefixTreeStrategy(grid, geoFieldName);
  }

  @Override
  public FacetDataCache<?> load(BoboSegmentReader reader) throws IOException {
    // No need to load any data
    return null;
  }

  /**
   * Builds a random access filter.
   * @param value Should be of the form: lat, lon: rad
   * @param selectionProperty
   */
  @Override
  public RandomAccessFilter buildRandomAccessFilter(String value, Properties selectionProperty)
      throws IOException {
    GeoRange range = GeoFacetCountCollector.parse(value);
    SpatialArgs spatialArgs = new SpatialArgs(SpatialOperation.Intersects,
        SpatialContext.GEO.makeCircle(range.getLon(), range.getLat(),
          DistanceUtils.dist2Degrees(range.getRad(), DistanceUtils.EARTH_MEAN_RADIUS_KM)));
    Filter filter = spatialStrategy.makeFilter(spatialArgs);
    if (filter == null) {
      return null;
    }
    return new SpatialFacetFilter(filter);
  }

  @Override
  public DocComparatorSource getDocComparatorSource() {
    throw new UnsupportedOperationException("Doc comparator is not yet supported for spatial facet");
  }

  @Override
  public FacetCountCollectorSource getFacetCountCollectorSource(final BrowseSelection sel,
      final FacetSpec fspec) {
    throw new UnsupportedOperationException(
        "Facet count collector is not yet supported for spatial facet");
  }

  @Override
  public String[] getFieldValues(BoboSegmentReader reader, int id) {
    FacetDataCache<?> latCache = (FacetDataCache<?>) reader.getFacetData(latitude);
    FacetDataCache<?> lonCache = (FacetDataCache<?>) reader.getFacetData(longitude);

    BigSegmentedArray latOrderArray = latCache.orderArray;
    TermValueList<?> latValList = latCache.valArray;

    BigSegmentedArray lonOrderArray = lonCache.orderArray;
    TermValueList<?> lonValList = lonCache.valArray;

    String docLatString = latValList.get(latOrderArray.get(id));
    String docLonString = lonValList.get(lonOrderArray.get(id));
    String[] fieldValues = new String[] { docLatString, docLonString };
    return fieldValues;
  }
}
