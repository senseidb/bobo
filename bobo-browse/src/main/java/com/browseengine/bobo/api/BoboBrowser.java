/**
 *
 */
package com.browseengine.bobo.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.browseengine.bobo.facets.FacetHandler;

public class BoboBrowser extends MultiBoboBrowser {
  /**
   * @param reader BoboMultiReader
   * @throws IOException
   */
  public BoboBrowser(BoboMultiReader reader) throws IOException {
    super(createBrowsables(reader.getSubReaders()));
  }

  public static List<BoboSegmentReader> gatherSubReaders(List<BoboMultiReader> readerList) {
    List<BoboSegmentReader> subReaderList = new ArrayList<BoboSegmentReader>();
    for (BoboMultiReader reader : readerList) {
      for (BoboSegmentReader subReader : reader.getSubReaders()) {
        subReaderList.add(subReader);
      }
    }
    return subReaderList;
  }

  public static Browsable[] createBrowsables(List<BoboSegmentReader> readerList) {
    BoboSubBrowser[] browsables = new BoboSubBrowser[readerList.size()];
    for (int i = 0; i < readerList.size(); ++i) {
      browsables[i] = new BoboSubBrowser(readerList.get(i));
    }
    return browsables;
  }

  /**
   * Gets a set of facet names
   *
   * @return set of facet names
   */
  @Override
  public Set<String> getFacetNames() {
    if (_subBrowsers.length == 0) {
      return null;
    }
    return _subBrowsers[0].getFacetNames();
  }

  @Override
  public FacetHandler<?> getFacetHandler(String name) {
    if (_subBrowsers.length == 0) {
      return null;
    }
    return _subBrowsers[0].getFacetHandler(name);
  }
}
