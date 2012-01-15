/**
 * 
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;

/**
 * AND-NOT operator node for SectionSearchQueryPlan
 */
public class AndNotNode extends SectionSearchQueryPlan
{
  SectionSearchQueryPlan _positiveNode;
  SectionSearchQueryPlan _negativeNode;
  
  public AndNotNode(SectionSearchQueryPlan positiveNode, SectionSearchQueryPlan negativeNode)
  {
    super();
    _positiveNode = positiveNode;
    _negativeNode = negativeNode;
  }
  
  @Override
  public int fetchDoc(int targetDoc) throws IOException
  {
    _curDoc = _positiveNode.fetchDoc(targetDoc);
    _curSec = -1;
    return _curDoc;
  }
  
  @Override
  public int fetchSec(int targetSec) throws IOException
  {
    while(_curSec < SectionSearchQueryPlan.NO_MORE_SECTIONS)
    {
      _curSec = _positiveNode.fetchSec(targetSec);
      if (_curSec == SectionSearchQueryPlan.NO_MORE_SECTIONS) break;

      targetSec = _curSec;

      if(_negativeNode._curDoc < _curDoc)
      {
        if(_negativeNode.fetchDoc(_curDoc) == DocIdSetIterator.NO_MORE_DOCS) break;
      }
          
      if(_negativeNode._curDoc == _curDoc &&
          (_negativeNode._curSec == SectionSearchQueryPlan.NO_MORE_SECTIONS ||
           _negativeNode.fetchSec(targetSec) > _curSec))
      {
        break;
      }
    }
    return _curSec;
  }
}
