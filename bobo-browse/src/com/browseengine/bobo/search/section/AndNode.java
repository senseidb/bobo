/**
 * 
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;

/**
 * AND operator node for SectionSearchQueryPlan
 */
public class AndNode extends SectionSearchQueryPlan
{
  protected SectionSearchQueryPlan[] _subqueries;
  
  public AndNode(SectionSearchQueryPlan[] subqueries)
  {
    _subqueries = subqueries;
    _curDoc = (subqueries.length > 0 ? -1 : DocIdSetIterator.NO_MORE_DOCS);
  }
  
  @Override
  public int fetchDoc(int targetDoc) throws IOException
  {
    if(_curDoc == DocIdSetIterator.NO_MORE_DOCS)
    {
      return _curDoc;
    }
    
    SectionSearchQueryPlan node = _subqueries[0];
    _curDoc = node.fetchDoc(targetDoc);
    targetDoc = _curDoc;
    
    int i = 1;
    while(i < _subqueries.length)
    {
      node = _subqueries[i];
      if(node._curDoc < targetDoc)
      {
        _curDoc = node.fetchDoc(targetDoc);
        if(_curDoc == DocIdSetIterator.NO_MORE_DOCS)
        {
          return _curDoc;
        }
        
        if(_curDoc > targetDoc)
        {
          targetDoc = _curDoc;
          i = 0;
          continue;
        }
      }
      i++;
    }
    _curSec = -1;
    return _curDoc;
  }
  
  @Override
  public int fetchSec(int targetSec) throws IOException
  {
    SectionSearchQueryPlan node = _subqueries[0];
    targetSec = node.fetchSec(targetSec);
    if (targetSec == SectionSearchQueryPlan.NO_MORE_SECTIONS)
    {
      _curSec = SectionSearchQueryPlan.NO_MORE_SECTIONS;
      return targetSec;
    }
    
    int i = 1;
    while(i < _subqueries.length)
    {
      node = _subqueries[i];
      if(node._curSec < targetSec)
      {
        _curSec = node.fetchSec(targetSec);
        if (_curSec == SectionSearchQueryPlan.NO_MORE_SECTIONS)
        {
          return _curSec;
        }
        
        if(_curSec > targetSec)
        {
          targetSec = _curSec;
          i = 0;
          continue;
        }
      }
      i++;
    }
    return _curSec;
  }
}
