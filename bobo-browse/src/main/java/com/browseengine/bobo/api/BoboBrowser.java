/**
 *
 */
package com.browseengine.bobo.api;

import java.io.IOException;
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

  public BoboBrowser(BoboMultiReader reader) throws IOException {
    super(createBrowsables(reader));
  }

  public static Browsable[] createBrowsables(BoboSegmentReader reader) {
    BoboSubBrowser[] browsables = new BoboSubBrowser[1];
    browsables[0] = new BoboSubBrowser(reader);
    return browsables;
  }

  public static Browsable[] createBrowsables(BoboMultiReader reader) {
    List<BoboSegmentReader> readerList = reader.getSubReaders();
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
    return _subBrowsers[0].getFacetNames();
  }

  @Override
  public FacetHandler<?> getFacetHandler(String name) {
    return _subBrowsers[0].getFacetHandler(name);
  }
}
