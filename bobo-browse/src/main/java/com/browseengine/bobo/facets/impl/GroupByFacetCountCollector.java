package com.browseengine.bobo.facets.impl;

import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.data.FacetDataCache;

public abstract class GroupByFacetCountCollector extends DefaultFacetCountCollector
{
  private int _totalGroups;

  public GroupByFacetCountCollector(String name,
                                    FacetDataCache dataCache,
                                    int docBase,
                                    BrowseSelection sel,
                                    FacetSpec ospec)
  {
    super(name, dataCache, docBase, sel, ospec);
  }

  abstract public int getTotalGroups();
}

