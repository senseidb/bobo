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
  public BoboBrowser(BoboSegmentReader reader) throws IOException {
    super(createBrowsables(reader));
  }

  public static void gatherSubReaders(List<BoboSegmentReader> readerList, BoboSegmentReader reader) {
    readerList.add(reader);
  }

  public static BoboSubBrowser[] createSegmentedBrowsables(List<BoboSegmentReader> readerList) {
    BoboSubBrowser[] browsables = new BoboSubBrowser[readerList.size()];
    for (int i = 0; i < readerList.size(); ++i) {
      browsables[i] = new BoboSubBrowser(readerList.get(i));
    }
    return browsables;
  }

  public static Browsable[] createBrowsables(BoboSegmentReader reader) {
    List<BoboSegmentReader> readerList = new ArrayList<BoboSegmentReader>();
    readerList.add(reader);
    return createSegmentedBrowsables(readerList);
  }

  public static Browsable[] createBrowsables(List<BoboSegmentReader> readerList) {
    return createSegmentedBrowsables(readerList);
  }

  /**
   * Gets a set of facet names
   *
   * @return set of facet names
   */
  @Override
  public Set<String> getFacetNames() {
    return _subBrowsers[0].getFacetNames();
  }

  @Override
  public FacetHandler<?> getFacetHandler(String name) {
    return _subBrowsers[0].getFacetHandler(name);
  }
}
