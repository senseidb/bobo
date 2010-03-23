/**
 * 
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;

/**
 * UNARY-NOT operator node
 * (this node is not supported by SectionSearchQueryPlan)
 */
public class UnaryNotNode extends SectionSearchQueryPlan
{
  private SectionSearchQueryPlan _subquery;
  
  public UnaryNotNode(SectionSearchQueryPlan subquery)
  {
    super();
    _subquery = subquery;
  }
  
  public SectionSearchQueryPlan getSubquery()
  {
    return _subquery;
  }
  
  @Override
  public int fetchDoc(int targetDoc) throws IOException
  {
    throw new UnsupportedOperationException("UnaryNotNode does not support fetchDoc");
  }

  @Override
  public int fetchSec(int targetSec) throws IOException
  {
    throw new UnsupportedOperationException("UnaryNotNode does not support fetchSec");
  }
}
