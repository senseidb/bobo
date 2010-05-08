/**
 * 
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

/**
 * TermNode for SectionSearchQueryPlan
 *
 */
public class TermNode extends AbstractTerminalNode
{
  private byte[] _payloadBuf;
  protected int _positionInPhrase;
  
  public TermNode(Term term, IndexReader reader) throws IOException
  {
    this(term, 0, reader);
  }

  public TermNode(Term term, int positionInPhrase, IndexReader reader) throws IOException
  {
    super(term, reader);
    _payloadBuf = new byte[4];
    _positionInPhrase = positionInPhrase; // relative position in a phrase
  }
  
  @Override
  public int fetchSec(int targetSec) throws IOException
  {
    if(_posLeft > 0)
    {
      while(true)
      {
        _curPos = _tp.nextPosition();
        _posLeft--;

        if(readSecId() >= targetSec) return _curSec;

        if(_posLeft <= 0) break;
      }
    }
    _curSec = SectionSearchQueryPlan.NO_MORE_SECTIONS;
    return _curSec;
  }
  
  protected int fetchPos() throws IOException
  {
    if(_posLeft > 0)
    {
      _curPos = _tp.nextPosition();
      _posLeft--;
      return _curPos;
    }
    _curPos = SectionSearchQueryPlan.NO_MORE_POSITIONS;
    return _curPos;
  }
  
  public int readSecId() throws IOException
  {
    if(_tp.isPayloadAvailable())
    {
      _curSec = intDecoders[_tp.getPayloadLength()].decode(_tp.getPayload(_payloadBuf, 0));
    }
    else
    {
      _curSec = -1;
    }
    return _curSec;
  }
  
  private abstract static class IntDecoder
  {
    abstract int decode(byte[] d);
  }
  
  private static final IntDecoder[] intDecoders = 
  {
    new IntDecoder()
    {
      int decode(byte[] d) { return 0; }
    },
    new IntDecoder()
    {
      int decode(byte[] d) { return (d[0]&0xff); } 
    },
    new IntDecoder()
    {
      int decode(byte[] d) { return (d[0]&0xff)|((d[1]&0xff)<<8); } 
    },
    new IntDecoder()
    {
      int decode(byte[] d) { return (d[0]&0xff)|((d[1]&0xff)<<8)|((d[2]&0xff)<<16); } 
    },
    new IntDecoder()
    {
      int decode(byte[] d) { return (d[0]&0xff)|((d[1]&0xff)<<8)|((d[2]&0xff)<<16)|((d[3]&0xff)<<24); } 
    }
  };
}
