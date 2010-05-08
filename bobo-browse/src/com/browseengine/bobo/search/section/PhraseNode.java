/**
 * 
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

/**
 * Phrase operator node for SectionSearchQUeryPlan
 *
 */
public class PhraseNode extends AndNode
{
  private TermNode[] _termNodes;
  private int _curPos;
  
  public PhraseNode(TermNode[] termNodes, IndexReader reader) throws IOException
  {
    super(termNodes);
    _termNodes = termNodes;
  }
  
  @Override
  public int fetchDoc(int targetDoc) throws IOException
  {
    _curPos = -1;
    return super.fetchDoc(targetDoc);
  }
  
  @Override
  public int fetchSec(int targetSec) throws IOException
  {
    TermNode firstNode = _termNodes[0];
    
    while(fetchPos() < SectionSearchQueryPlan.NO_MORE_POSITIONS)
    {
      int secId = firstNode.readSecId();
      if(secId >= targetSec)
      {
        targetSec = secId;
        boolean matched = true;
        for(int i = 1; i < _termNodes.length; i++)
        {
          matched = (targetSec == _termNodes[i].readSecId());
          if(!matched) break;
        }
        if(matched)
        {
          _curSec = targetSec;
          return _curSec;
        }
      }
    }
    _curSec = SectionSearchQueryPlan.NO_MORE_SECTIONS;
    return _curSec;
  }
  
  @Override
  protected int fetchPos() throws IOException
  {
    int targetPhrasePos = _curPos + 1;
    
    int i = 0;
    while(i < _termNodes.length)
    {
      TermNode node = _termNodes[i];
      int targetTermPos = (targetPhrasePos + node._positionInPhrase);
      while(node._curPos < targetTermPos)
      {
        if(node.fetchPos() == SectionSearchQueryPlan.NO_MORE_POSITIONS)
        {
          _curPos = SectionSearchQueryPlan.NO_MORE_POSITIONS;
          return _curPos;
        }
      }
      if(node._curPos == targetTermPos)
      {
        i++;
      }
      else
      {
        targetPhrasePos = node._curPos - i;
        i = 0;
      }
    }
    _curPos = targetPhrasePos;
    return _curPos;
  }
}
