/**
 * 
 */
package com.browseengine.bobo.analysis.section;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Payload;

/**
 * TokenStream for the section meta data. This returns a single token with a payload.
 */
public final class IntMetaDataTokenStream extends TokenStream
{
  private final String _tokenText;
  private final TermAttribute _termAttribute;
  private final OffsetAttribute _offsetAttribute;
  private final PayloadAttribute _payloadAtt;
  private Payload _payload;  
  private boolean _returnToken = false;

  public IntMetaDataTokenStream(String tokenText)
  {
    _tokenText = tokenText;
    _termAttribute = (TermAttribute)addAttribute(TermAttribute.class);
    _offsetAttribute = (OffsetAttribute)addAttribute(OffsetAttribute.class);
    _payloadAtt = (PayloadAttribute)addAttribute(PayloadAttribute.class);
  }

  /**
   * sets meta data
   * @param data array of integer metadata indexed by section id
   */
  public void setMetaData(int[] data)
  {
    byte[] buf = new byte[data.length * 4];
    int i = 0;
    
    for(int j = 0; j < data.length; j++)
    {
      int datum = data[j];
      buf[i++] = (byte)(datum);
      buf[i++] = (byte)(datum >>> 8);
      buf[i++] = (byte)(datum >>> 16);
      buf[i++] = (byte)(datum >>> 24);
    }
    
    _payload = new Payload(buf);
    _returnToken = true;
  }

  /**
   * Return the single token created.
   */
  public boolean incrementToken() throws IOException
  {
    if(_returnToken)
    {
      _termAttribute.setTermBuffer(_tokenText);
      _offsetAttribute.setOffset(0, 0);
      _payloadAtt.setPayload(_payload);
      _returnToken = false;
      return true;
    }
    return false;
  }
}
