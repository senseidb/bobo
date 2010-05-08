/**
 * 
 */
package com.browseengine.bobo.search.section;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermPositions;

/**
 *
 */
public class IntMetaDataCache implements MetaDataCache
{
  private static final int MAX_SLOTS = 1024; 
  private static final int MISSING = Integer.MIN_VALUE;
  
  private final IndexReader _reader;
  private int[][] _list;
  
  private int _curPageNo;
  private int[] _curPage;
  private int _curSlot;
  private int _curData;

  public IntMetaDataCache(Term term, IndexReader reader) throws IOException
  {
    _reader = reader;
    
    int maxDoc = reader.maxDoc();
    _list = new int[(maxDoc + MAX_SLOTS - 1) / MAX_SLOTS][];
    _curPageNo = 0;
    _curSlot = 0;
    _curData = MAX_SLOTS;
    
    if(maxDoc > 0)
    {
      _curPage = new int[MAX_SLOTS * 2];
      loadPayload(term);
    }
    
    _curPage = null;
  }
  
  protected void add(int docid, byte[] data, int blen)
  {
    int pageNo = docid / MAX_SLOTS;
    if(pageNo != _curPageNo)
    {
      // save the page
      
      while (_curSlot < MAX_SLOTS)
      {
        _curPage[_curSlot++] = MISSING;
      }
      _list[_curPageNo++] = copyPage(new int[_curData]);  // optimize the page to make getMaxItems work
      _curSlot = 0;
      _curData = MAX_SLOTS;

      while (_curPageNo < pageNo)
      {
        _list[_curPageNo++] = null;
      }
    }
    
    while (_curSlot < docid % MAX_SLOTS)
    {
      _curPage[_curSlot++] = MISSING;
    }

    if(blen <= 4)
    {
      int val = 0;
      if(blen == 0)
      {
        val = MISSING;
      }
      else
      {
        for(int i = 0; i < 4; i++)
        {
          if(i >= data.length) break;
          
          val |= ((data[i] & 0xff) << (i * 8));
        }
      }
      if (val >= 0) 
      {
        _curPage[_curSlot] = val;
      }
      else
      {
        appendToTail(data, blen);
      }
    }
    else
    {
      appendToTail(data, blen);
    }
    _curSlot++;
  }
  
  private void appendToTail(byte[] data, int blen)
  {
    int ilen = (blen + 3) / 4; // length in ints
    
    if(_curPage.length <= _curData + ilen)
    {
      // double the size of the variable part at least
      _curPage = copyPage(new int[_curPage.length + Math.max((_curPage.length - MAX_SLOTS), ilen)]);
    }
    _curPage[_curSlot] = (- _curData);
    _curData = copyByteToInt(data, 0, blen, _curPage, _curData);
  }
  
  private int copyByteToInt(byte[] src, int off, int blen, int[] dst, int dstoff)
  {
    while(blen > 0)
    {
      int val = 0;
      for(int i = 0; i < 4; i++)
      {
        blen--;
        
        if(off >= src.length) break; // may not have all bytes            
        val |= ((src[off++] & 0xff) << (i * 8));
      }

      dst[dstoff++] = val;
    }
    return dstoff;
  }
  
  private int[] copyPage(int[] dst)
  {
    System.arraycopy(_curPage, 0, dst, 0, _curData);
    return dst;
  }

  protected void loadPayload(Term term) throws IOException
  {
    byte[] payloadBuf = null;
    TermPositions tp = _reader.termPositions();
    tp.seek(term);
    while(tp.next())
    {
      if(tp.freq() > 0)
      {
        tp.nextPosition();
        if(tp.isPayloadAvailable())
        {
          int len = tp.getPayloadLength();
          payloadBuf = tp.getPayload(payloadBuf, 0);
          add(tp.doc(), payloadBuf, len);
        }
      }
    }
    
    // save the last page
    
    while (_curSlot < MAX_SLOTS)
    {
      _curPage[_curSlot++] = MISSING;
    }
    _list[_curPageNo] = copyPage(new int[_curData]); // optimize the page to make getNumItems work
    _curPage = null;
  }

  public int getValue(int docid, int idx, int defaultValue)
  {
    int[] page = _list[docid / MAX_SLOTS];
    if(page == null) return defaultValue;
    
    int val = page[docid % MAX_SLOTS];
    if (val >= 0)
    {
      return val;
    }
    else
    {
      return (val == MISSING ?  defaultValue : page[idx - val]);
    }
  }
  
  public int getNumItems(int docid)
  {
    int[] page = _list[docid / MAX_SLOTS];
    if(page == null) return 0;
    
    int slotNo = docid % MAX_SLOTS;
    int val = page[slotNo];
    
    if (val >= 0) return 1;
    
    if(val == MISSING) return 0;
    
    slotNo++;
    while(slotNo < MAX_SLOTS)
    {
      int nextVal = page[slotNo++];
      if (nextVal < 0 && nextVal != MISSING)
      {
        return (val - nextVal);
      }
    }
    return (val + page.length);
  }
  
  public int maxDoc()
  {
    return _reader.maxDoc();
  }
}
