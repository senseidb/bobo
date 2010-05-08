/**
 * 
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.search.DocIdSetIterator;

/**
 * An abstract class for terminal nodes of SectionSearchQueryPlan
 */
public abstract class AbstractTerminalNode extends SectionSearchQueryPlan
{
  protected TermPositions _tp;
  protected int _posLeft;
  protected int _curPos;
  
  public AbstractTerminalNode(Term term, IndexReader reader) throws IOException
  {
    super();
    _tp = reader.termPositions();
    _tp.seek(term);
    _posLeft = 0;
  }
  
  @Override
  public int fetchDoc(int targetDoc) throws IOException
  {
    if(targetDoc <= _curDoc) targetDoc = _curDoc + 1;
    
    if(_tp.skipTo(targetDoc))
    {
      _curDoc = _tp.doc();
      _posLeft = _tp.freq();
      _curSec = -1;
      _curPos = -1;
      return _curDoc;
    }
    else
    {
      _curDoc = DocIdSetIterator.NO_MORE_DOCS;
      _tp.close();
      return _curDoc;
    }
  }
  
  abstract public int fetchSec(int targetSec) throws IOException;
}
