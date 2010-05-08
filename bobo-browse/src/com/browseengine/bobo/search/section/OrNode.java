/**
 * 
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;

/**
 * OR operator node for SectionSearchQueryPlan
 */
public class OrNode extends SectionSearchQueryPlan
{
  private NodeQueue _pq;
  
  protected OrNode() { }
  
  public OrNode(SectionSearchQueryPlan[] subqueries)
  {
    if(subqueries.length == 0)
    {
      _curDoc = DocIdSetIterator.NO_MORE_DOCS;
    }
    else
    {
      _pq = new NodeQueue(subqueries.length);
      for(SectionSearchQueryPlan q : subqueries)
      {
        if(q != null) _pq.add(q);
      }
      _curDoc = -1;
    }
  }
  
  @Override
  public int fetchDoc(int targetDoc) throws IOException
  {
    if (_curDoc == DocIdSetIterator.NO_MORE_DOCS) return _curDoc;
    
    if(targetDoc <= _curDoc) targetDoc = _curDoc + 1;
    
    _curSec = -1;
    
    SectionSearchQueryPlan node = (SectionSearchQueryPlan)_pq.top();
    while(true)
    {
      if(node._curDoc < targetDoc)
      {
        if(node.fetchDoc(targetDoc) < DocIdSetIterator.NO_MORE_DOCS)
        {
          node = (SectionSearchQueryPlan)_pq.updateTop();
        }
        else
        {
          _pq.pop();
          if (_pq.size() <= 0)
          {
            _curDoc = DocIdSetIterator.NO_MORE_DOCS;
            return _curDoc;
          }
          node = (SectionSearchQueryPlan)_pq.top();
        }
      }
      else
      {
        _curDoc = node._curDoc;
        return _curDoc;
      }
    }
  }
  
  @Override
  public int fetchSec(int targetSec) throws IOException
  {
    if(_curSec == SectionSearchQueryPlan.NO_MORE_SECTIONS) return _curSec;
    
    if(targetSec <= _curSec) targetSec = _curSec + 1;
    
    SectionSearchQueryPlan node = (SectionSearchQueryPlan)_pq.top();
    while(true)
    {
      if(node._curDoc == _curDoc && _curSec < SectionSearchQueryPlan.NO_MORE_SECTIONS)
      {
        if(node._curSec < targetSec)
        {
          node.fetchSec(targetSec);
          node = (SectionSearchQueryPlan)_pq.updateTop();
        }
        else
        {
          _curSec = node._curSec;
          return _curSec;
        }
      }
      else
      {
        _curSec = SectionSearchQueryPlan.NO_MORE_SECTIONS;
        return _curSec;
      }
    }
  }
}
