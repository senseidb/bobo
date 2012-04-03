/**
 * 
 */
package com.browseengine.bobo.util;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BitVector;
import org.apache.lucene.util.OpenBitSet;

import com.browseengine.bobo.facets.data.TermValueList;
import com.browseengine.bobo.query.scoring.FacetTermScoringFunction;

/**
 * write-once big nested int array
 * @author ymatsuda
 *
 */
public final class BigNestedIntArray
{
  public static final int MAX_ITEMS = 1024;
  private static final int MAX_SLOTS = 1024;
  private static final int SLOTID_MASK = 0x3FF;
  private static final int PAGEID_SHIFT = 10;
  private static final int COUNT_MASK = 0x7FF;
  private static final int VALIDX_SHIFT = 11;
  private static final int ROUNDING = 255;
  
  private static final int MISSING = Integer.MIN_VALUE;
  private static final int[] MISSING_PAGE;
  static
  {
    MISSING_PAGE = new int[MAX_SLOTS];
    Arrays.fill(MISSING_PAGE, MISSING);
  }

  private int _maxItems = MAX_ITEMS;
  private int[][] _list;
  private int _size;
  
  static final private String[] EMPTY = new String[0];

  static public abstract class Loader
  {
    private int[][] _list;
    private int _curPageNo;
    private int[] _curPage;
    private int _curSlot;
    private int _curData;

    private int[][] _reuse;
    private int[] _reuseIdx;
    public int reuseUsage;
    private static Comparator<int[]> COMPARE_ARRAYSIZE = new Comparator<int[]>()
    {
      public int compare(int[] o1, int[] o2)
      {
        if(o1 == null || o2 == null)
        {
          if(o1 != null) return -1;
          if(o2 != null) return 1;
          return 0;
        }
        return (o1.length - o2.length);
      }
    };
    
    private void reclaim(int[][] list)
    {
      _reuse = null;
      _reuseIdx = null;
      reuseUsage = 0;
      
      if(list != null && list.length > 0)
      {
        Arrays.sort(list, COMPARE_ARRAYSIZE); // sort by size
        for(int i = (list.length - 1); i >=0; i--)
        {
          if(list[i] != null)
          {
            _reuse = list;
            _reuseIdx = list[i]; // use the largest page for tracking
            break;
          }
        }
        if(_reuseIdx == null) return;
        
        Arrays.fill(_reuseIdx, -1);
        for(int i = 0; i < list.length; i++)
        {
          if(list[i] == null) break;
          
          int idx = (list[i]).length - 1;
          if(idx >= 0 && _reuseIdx[idx] == -1) _reuseIdx[idx] = i;
        }
      }
    }
    
    private int[] alloc(int size)
    {
      size += (ROUNDING - 1);
      size -= (size % ROUNDING);
      
      if(_reuseIdx != null && _reuseIdx.length >= size)
      {
        int location = _reuseIdx[size - 1];
        if(location >= 0 && location < _reuse.length)
        {
          int[] page = _reuse[location];
          if(page != null && page.length == size)
          {
            // found a reusable page
            _reuseIdx[size - 1]++;
            _reuse[location] = null;
            
            if(page == _reuseIdx)
            {
              // find a replacement page for reuseIdx
              for(int i = location; i >= 0; i--)
              {
                if(_reuse[i] != null)
                {
                  _reuseIdx = _reuse[i];
                  System.arraycopy(page, 0, _reuseIdx, 0, _reuseIdx.length);
                }
              }
            }
            reuseUsage += size;
            return page;
          }
          else
          {
            // no more page with this size
            _reuseIdx[size - 1] = -1;
          }
        }
      }
      return new int[size];
    }
    
    /**
     * initializes the loading context
     * @param size
     */
    final public void initialize(int size, int[][] oldList)
    {
      reclaim(oldList);
      
      _list = new int[(size + MAX_SLOTS - 1) / MAX_SLOTS][];
      _curPageNo = 0;
      _curSlot = 0;
      _curData = MAX_SLOTS;
      _curPage = new int[MAX_SLOTS * 2];
    }
    
    /**
     * finishes loading
     */
    final public int[][] finish()
    {
      if(_list.length > _curPageNo)
      {
        // save the last page
        while (_curSlot < MAX_SLOTS)
        {
          _curPage[_curSlot++] = MISSING;
        }
        _list[_curPageNo] = copyPageTo(alloc(_curData));
      }
      _reuse = null;
      _reuseIdx = null;
      
      return _list;
    }
    
    /**
     * loads data
     */
    public abstract void load() throws Exception;

    /**
     * reserves storage for the next int array data
     * @param id
     * @param size
     */
    final protected void reserve(int id, int size)
    {
      final int pageNo = (id >> PAGEID_SHIFT);
      final int slotId = (id & SLOTID_MASK);
      
      if(pageNo != _curPageNo)
      {
        if(pageNo < _curPageNo) throw new IllegalArgumentException("id is out of order");
        
        // save the current page
        
        while (_curSlot < MAX_SLOTS)
        {
          _curPage[_curSlot++] = MISSING;
        }
        _list[_curPageNo++] = copyPageTo(alloc(_curData));
        
        _curSlot = 0;
        _curData = MAX_SLOTS;
        
        while (_curPageNo < pageNo)
        {
          _list[_curPageNo++] = null;
        }      
      }
      else
      {
        if(_curPageNo == pageNo && _curSlot > slotId) throw new IllegalArgumentException("id is out of order");
      }
      
      while (_curSlot < slotId)
      {
        _curPage[_curSlot++] = MISSING;
      }

      if(_curPage.length <= _curData + size)
      {
        // double the size of the variable part at least
        _curPage = copyPageTo(new int[_curPage.length + Math.max((_curPage.length - MAX_SLOTS), size)]);
      }
    }
    
    /**
     * stores int array data. must call reserve(int,int) first to allocate storage 
     * @param data
     * @param off
     * @param len
     */
    final protected void store(int[] data, int off, int len)
    {
      if(len == 0)
      {
        _curPage[_curSlot] = MISSING;
      }
      else if (len == 1 && data[off] >= 0)
      {
        _curPage[_curSlot] = data[off];        
      }
      else
      {
        _curPage[_curSlot] = ((- _curData) << VALIDX_SHIFT | len);
        System.arraycopy(data, off, _curPage, _curData, len);
        _curData += len;
      }
      _curSlot++;
    }
    
    protected void add(int id, int[] data, int off, int len)
    {
      reserve(id, len);
      store(data, off, len);
    }

    /**
     * allocates storage for future calls of setData.
     * @param id
     * @param len
     */
    protected void allocate(int id, int len, boolean nonNegativeIntOnly)
    {
      reserve(id, len);
      if(len == 0)
      {
        _curPage[_curSlot] = MISSING;
      }
      else if (len == 1 && nonNegativeIntOnly)
      {
        _curPage[_curSlot] = 0;
      }
      else
      {
        _curPage[_curSlot] = ((- _curData) << VALIDX_SHIFT);
        _curData += len;
      }
      _curSlot++;
    }

    protected int[] copyPageTo(int[] dst)
    {
      System.arraycopy(_curPage, 0, dst, 0, _curData);
      return dst;
    }
  }

  /**
   * Constructs BigNEstedIntArray
   * @throws Exception
   */
  public BigNestedIntArray()
  {
  }
  
  /**
   * set maximum number of items per doc.
   * @param maxItems
   */
  public void setMaxItems(int maxItems)
  {
    _maxItems = Math.min(maxItems, MAX_ITEMS);
  }
  
  /**
   * get maximum number of items per doc.
   * @return maxItems
   */
  public int getMaxItems()
  {
    return _maxItems;
  }
  
  /**
   * loads data using the loader
   * @param size
   * @param loader
   * @throws Exception
   */
  public final void load(int size, Loader loader) throws Exception
  {
     _size = size;
    loader.initialize(size, _list);
    if(size > 0)
    {
      loader.load();
    }
    _list = loader.finish();    
  }
  
  public int size(){
	  return _size;
  }
  /**
   * gets an int data at [id][idx]
   * @param id
   * @param idx
   * @param defaultValue
   * @return
   */
  public final int getData(int id, int idx, int defaultValue)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) return defaultValue;
    
    int val = page[id & SLOTID_MASK];
    if(val >= 0)
    {
      return val;
    }
    else if(val == MISSING)
    {
      return defaultValue;
    }
    else
    {
      val >>= VALIDX_SHIFT; // signed shift, remember this is a negative number
      return page[idx - val];
    }
  }
  
  /**
   * gets an int data at [id]
   * @param id
   * @param buf
   * @param defaultValue
   * @return length
   */
  public final int getData(int id, int[] buf)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) return 0;
    
    int val = page[id & SLOTID_MASK];
    if(val >= 0)
    {
      buf[0] = val;
      return 1;
    }
    else if(val == MISSING)
    {
      return 0;
    }
    else
    {
      final int num = (val & COUNT_MASK);
      val >>= VALIDX_SHIFT; // signed shift, remember this is a negative number
      System.arraycopy(page, (- val), buf, 0, num);
      return num;
    }
  }

  /**
   * translates the int value using the val list
   * @param <T>
   * @param array
   * @param id
   * @param valarray
   * @return
   */
  public final String[] getTranslatedData(int id, TermValueList valarray)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    
    if(page == null)
    {
      return EMPTY;
    }
    else
    {
      int val = page[id & SLOTID_MASK];
    
      if(val >= 0)
      {
        return new String[]{valarray.get(val)};
      }
      else if(val == MISSING)
      {
        return EMPTY;
      }
      else
      {
        final int num = (val & COUNT_MASK);
        val >>= VALIDX_SHIFT; // signed shift, remember this is a negative number
    
        String[] ret = new String[num];
        for(int i = 0; i < num; i++)
        {
          ret[i] = valarray.get(page[i - val]);
        }
        return ret;
      }
    }
  }
  
  /**
   * translates the int value using the val list
   * @param <T>
   * @param array
   * @param id
   * @param valarray
   * @return
   */
  public final Object[] getRawData(int id, TermValueList valarray)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    
    if(page == null)
    {
      return EMPTY;
    }
    else
    {
      int val = page[id & SLOTID_MASK];
    
      if(val >= 0)
      {
    	return new Object[]{valarray.getRawValue(val)};
      }
      else if(val == MISSING)
      {
        return EMPTY;
      }
      else
      {
        final int num = (val & COUNT_MASK);
        val >>= VALIDX_SHIFT; // signed shift, remember this is a negative number
    
        Object[] ret = new Object[num];
        for(int i = 0; i < num; i++)
        {
          ret[i] = valarray.getRawValue(page[i - val]);
        }
        return ret;
      }
    }
  }
  
  public final float getScores(int id,int[] freqs,float[] boosts,FacetTermScoringFunction function){
	  function.clearScores();
	  final int[] page = _list[id >> PAGEID_SHIFT];
	  int val = page[id & SLOTID_MASK];
	    
      if(val >= 0)
      {
    	return function.score(freqs[val], boosts[val]);
      }
      else
      {
        final int num = (val & COUNT_MASK);
        val >>= VALIDX_SHIFT; // signed shift, remember this is a negative number
        int idx;
        for(int i = 0; i < num; i++)
        {
          idx = page[i-val];
          function.scoreAndCollect(freqs[idx],boosts[idx]);
        }
        return function.getCurrentScore();
      }
  }
  
  public final int compare(int i,int j){
	  final int[] page1 = _list[i >> PAGEID_SHIFT];
	  final int[] page2 = _list[j >> PAGEID_SHIFT];
	  
	  if (page1 == null){
		  if (page2 == null) return 0;
		  else return -1;
	  }
	  else{
		  if (page2 == null) return 1;
	  }
	  
	  final int val1 = page1[i & SLOTID_MASK];
	  final int val2 = page2[j & SLOTID_MASK];
	  
	  if (val1>=0 && val2>=0) return val1 - val2;
	  
	  if (val1>=0){
		  if (val2 == MISSING) return 1;
		  int idx = - (val2 >> VALIDX_SHIFT);// signed shift, remember this is a negative number
	      int val = val1 - page2[idx];
	      if (val == 0){
	    	return -1;   
	      }
	      else{
	    	return val;
	      }
	  }
	  if (val2>=0){
		  if (val1 == MISSING) return -1;
		  int idx = - (val1 >> VALIDX_SHIFT);// signed shift, remember this is a negative number
	      int val = page1[idx] - val2;
	      if (val==0){
	    	  return 1;
	      }
	      else{
	    	  return val;
	      }
	  }
	  
	  if (val1 == MISSING){
		  if (val2 == MISSING){
			  return 0;
		  }
		  else return -1;
	  }
	  else{
		  if (val2 == MISSING){
			  return 1;
		  }
	  }
	  
	  int idx1 = - (val1 >> VALIDX_SHIFT);// signed shift, remember this is a negative number
	  int len1 = (val1 & COUNT_MASK);
	  
	  int idx2 = - (val2 >> VALIDX_SHIFT);// signed shift, remember this is a negative number
	  int len2 = (val2 & COUNT_MASK);
	  
	  for (int k=0;k<len1;++k){
		if (k>=len2){
          return 1;
		}
		  
		int compVal = page1[idx1+k] - page2[idx2+k];
		if (compVal!=0) return compVal;
	  }
	  if (len1 == len2) return 0;
	  return -1;
  }

  public final boolean contains(int id, int value)
  {
    return contains(id, value, false);
  }

  public final boolean contains(int id, int value, boolean withMissing)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) {
      if (withMissing && value == 0)
      {
        return true;
      }
      else
      {
        return false;
      }
    }
    
    final int val = page[id & SLOTID_MASK];
    if (val >= 0)
    {
      return (val == value);
    }
    else if(val != MISSING)
    {
      int idx = - (val >> VALIDX_SHIFT);// signed shift, remember this is a negative number
      int end = idx + (val & COUNT_MASK);
      while(idx < end)          
      {
        if(page[idx++] == value) return true;  
      }
    }
    else if (withMissing)
    {
      return (value == 0);
    }
    return false;
  }
  
  public final boolean contains(int id, BitVector values)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) return false;
    
    final int val = page[id & SLOTID_MASK];
    if (val >= 0)
    {
      return (values.get(val));
    }
    else if(val != MISSING)
    {
      int idx = - (val >> VALIDX_SHIFT);// signed shift, remember this is a negative number
      int end = idx + (val & COUNT_MASK);
      while(idx < end)          
      {
        if(values.get(page[idx++])) return true;  
      }
    }
    return false;
  }
  /**
   * @param id - documentID
   * @param startValueId - inclusive
   * @param endValueId - exclusive
   * @return
   */
  public final boolean containsValueInRange(int id, int startValueId, int endValueId)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) return false;
    
    final int val = page[id & SLOTID_MASK];
    if (val >= 0)
    {
      return val >= startValueId && val < endValueId;
    }
    else if(val != MISSING)
    {
      int idx = - (val >> VALIDX_SHIFT);// signed shift, remember this is a negative number
      int end = idx + (val & COUNT_MASK);
      while(idx < end)          
      {
        
        if(page[idx] >= startValueId && page[idx] < endValueId) return true;  
        idx++;
      }
    }
    return false;
  }
  
  public final boolean contains(int id, OpenBitSet values)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) return false;
    
    final int val = page[id & SLOTID_MASK];
    if (val >= 0)
    {
      return (values.fastGet(val));
    }
    else if(val != MISSING)
    {
      int idx = - (val >> VALIDX_SHIFT);// signed shift, remember this is a negative number
      int end = idx + (val & COUNT_MASK);
      while(idx < end)          
      {
        if(values.fastGet(page[idx++])) return true;  
      }
    }
    return false;
  }
  
  public final int findValue(int value, int id, int maxID)
  {
    return findValue(value, id, maxID, false);
  }

  public final int findValue(int value, int id, int maxID, boolean withMissing)
  {
    int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) page = MISSING_PAGE;
    
    while(true)
    {
      final int val = page[id & SLOTID_MASK];
      if (val >= 0)
      {
        if(val == value) return id;
      }
      else if(val != MISSING)
      {
        int idx = - (val >> VALIDX_SHIFT);// signed shift, remember this is a negative number
        int end = idx + (val & COUNT_MASK);
        while(idx < end)          
        {
          if(page[idx++] == value) return id;  
        }
      }
      else if (withMissing)
      {
        if(0 == value) return id;
      }
      if(id >= maxID) break;
      
      if(((++id) & SLOTID_MASK) == 0)
      {
        page = _list[id >> PAGEID_SHIFT];
        if(page == null) page = MISSING_PAGE;
      }
    }
    
    return DocIdSetIterator.NO_MORE_DOCS;
  }
  
  public final int findValues(BitVector values, int id, int maxID)
  {
    return findValues(values, id, maxID, false);
  }

  public final int findValues(BitVector values, int id, int maxID, boolean withMissing)
  {
    int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) page = MISSING_PAGE;

    while(true)
    {
      final int val = page[id & SLOTID_MASK];
      if (val >= 0)
      {
        if(values.get(val)) return id;
      }
      else if(val != MISSING)
      {
        int idx = - (val >> VALIDX_SHIFT);// signed shift, remember this is a negative number
        int end = idx + (val & COUNT_MASK);
        while(idx < end)          
        {
          if(values.get(page[idx++])) return id;  
        }
      }
      else if(withMissing)
      {
        if(values.get(0)) return id;
      }
      if(id >= maxID) break;
      
      if((++id & SLOTID_MASK) == 0)
      {
        page = _list[id >> PAGEID_SHIFT];
        if(page == null) page = MISSING_PAGE;
      }
    }
    
    return DocIdSetIterator.NO_MORE_DOCS;
  }
  
  public final int findValues(OpenBitSet values, int id, int maxID)
  {
    return findValues(values, id, maxID, false);
  }

  public final int findValues(OpenBitSet values, int id, int maxID, boolean withMissing)
  {
    int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) page = MISSING_PAGE;

    while(true)
    {
      final int val = page[id & SLOTID_MASK];
      if (val >= 0)
      {
        if(values.fastGet(val)) return id;
      }
      else if(val != MISSING)
      {
        int idx = - (val >> VALIDX_SHIFT);// signed shift, remember this is a negative number
        int end = idx + (val & COUNT_MASK);
        while(idx < end)          
        {
          if(values.fastGet(page[idx++])) return id;  
        }
      }
      else if(withMissing)
      {
        if(values.fastGet(0)) return id;
      }
      if(id >= maxID) break;
      
      if((++id & SLOTID_MASK) == 0)
      {
        page = _list[id >> PAGEID_SHIFT];
        if(page == null) page = MISSING_PAGE;
      }
    }
    
    return DocIdSetIterator.NO_MORE_DOCS;
  }
 
  public final int findValuesInRange(int startIndex, int endIndex, int id, int maxID)
  {
    int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) page = MISSING_PAGE;

    while(true)
    {
      int val = page[id & SLOTID_MASK];
      if (val >= 0)
      {
        if(val >= startIndex && val <= endIndex) return id;
      }
      else if(val != MISSING)
      {
        int idx = - (val >> VALIDX_SHIFT);// signed shift, remember this is a negative number
        int end = idx + (val & COUNT_MASK);
        while(idx < end)          
        {
          val = page[idx++];
          if(val >= startIndex && val <= endIndex) return id;  
        }
      }      
      if(id >= maxID) break;
      
      if((++id & SLOTID_MASK) == 0)
      {
        page = _list[id >> PAGEID_SHIFT];
        if(page == null) page = MISSING_PAGE;
      }
    }
    
    return DocIdSetIterator.NO_MORE_DOCS;
  }
  public final int count(final int id, final int[] count)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) {
      count[0]++;
      return 0;
    }
    
    int val = page[id & SLOTID_MASK];
    if(val >= 0)
    {
      count[val]++;
      return 1;
    }
    else if(val != MISSING)
    {
      int idx = - (val >> VALIDX_SHIFT); // signed shift, remember val is a negative number
      int cnt = (val & COUNT_MASK);
      int end = idx + cnt;
      while(idx < end)
      {
        count[page[idx++]]++;
      }
      return cnt;
    }
    count[0]++;
    return 0;
  }
  
  public final void countNoReturn(final int id, final int[] count)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) {
      count[0]++;
      return;
    }
    
    int val = page[id & SLOTID_MASK];
    if(val >= 0)
    {
      count[val]++;
      return;
    }
    else if(val != MISSING)
    {
      int idx = - (val >> VALIDX_SHIFT); // signed shift, remember val is a negative number
      int cnt = (val & COUNT_MASK);
      int end = idx + cnt;
      while(idx < end)
      {
        count[page[idx++]]++;
      }
      return;
    }
    count[0]++;
    return;
  }
  
  public final void countNoReturnWithFilter(final int id, final int[] count, BitVector filter)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) {
      count[0]++;
      return;
    }
    
    int val = page[id & SLOTID_MASK];
    if(val >= 0)
    {
      if (filter.get(val))
      {
        count[val]++;
      }
      return;
    }
    else if(val != MISSING)
    {
      int idx = - (val >> VALIDX_SHIFT); // signed shift, remember val is a negative number
      int cnt = (val & COUNT_MASK);
      int end = idx + cnt;
      while(idx < end)
      {
        int value = page[idx++];
        if (filter.get(value))
        {
          count[value]++;
        }
      }
      return;
    }
    count[0]++;
    return;
  }

  public final void countNoReturnWithFilter(final int id, final int[] count, OpenBitSet filter)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) {
      count[0]++;
      return;
    }
    
    int val = page[id & SLOTID_MASK];
    if(val >= 0)
    {
      if (filter.fastGet(val))
      {
        count[val]++;
      }
      return;
    }
    else if(val != MISSING)
    {
      int idx = - (val >> VALIDX_SHIFT); // signed shift, remember val is a negative number
      int cnt = (val & COUNT_MASK);
      int end = idx + cnt;
      while(idx < end)
      {
        int value = page[idx++];
        if (filter.fastGet(value))
        {
          count[value]++;
        }
      }
      return;
    }
    count[0]++;
    return;
  }


  /**
   * returns the number data items for id
   * @param id
   * @return
   */
  public final int getNumItems(int id)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) return 0;
    
    int val = page[id & SLOTID_MASK];
    
    if(val >= 0) return 1;
    
    if(val == MISSING) return 0;
    
    return (val & COUNT_MASK);
  }
  
  /**
   * adds Data to id
   */
  public final boolean addData(final int id, final int data)
  {
    final int[] page = _list[id >> PAGEID_SHIFT];
    if(page == null) return true;
    
    final int slotId = (id & SLOTID_MASK);
    int val = page[slotId];
    
    if(val == MISSING)
    {
      return true; // don't store
    }
    else if(val >= 0)
    {
      page[slotId] = data; // only one value
      return true;
    }
    else
    {
      int num = (val & COUNT_MASK);
      if(num >= _maxItems) return false;
      
      val >>= VALIDX_SHIFT; // signed shift, remember this is a negative number
      page[num - val] = data;
      val = ((val << VALIDX_SHIFT) | (num + 1));
      page[slotId] = val;
      return true;
    }
  }
  
  /**
   * A loader that buffer all data in memory, then load them to BigNestedIntArray.
   * Data does not need to be sorted prior to the operation.
   * Note that this loader supports only non-negative integer data.
   */
  public final static class BufferedLoader extends Loader
  {
    private static int EOD = Integer.MIN_VALUE;
    private static int SEGSIZE = 8;
    
    private int _size;
    private final BigIntArray _info;
    private BigIntBuffer _buffer;
    private int _maxItems;
    
    public BufferedLoader(int size, int maxItems, BigIntBuffer buffer)
    {
      _size = size;
      _maxItems = Math.min(maxItems, BigNestedIntArray.MAX_ITEMS);
      _info = new BigIntArray(size << 1); // pointer and count
      _info.fill(EOD);
      _buffer = buffer;
    }
    
    public BufferedLoader(int size)
    {
      this(size, MAX_ITEMS, new BigIntBuffer());
    }
    
    /**
     * resets loader. This also resets underlying BigIntBuffer.
     */
    public void reset(int size, int maxItems, BigIntBuffer buffer)
    {
      if(size >= capacity()) throw new IllegalArgumentException("unable to change size");
      _size = size;
      _maxItems = maxItems;
      _info.fill(EOD);
      _buffer = buffer;
    }
    
    /**
     * adds a pair of id and value to the buffer
     * @param id
     * @param val
     */
    public final boolean add(int id, int val)
    {
      int ptr = _info.get(id << 1);
      if(ptr == EOD)
      {
        // 1st insert
        _info.add(id << 1, val);
        return true;
      }
      
      int cnt = _info.get((id << 1) + 1);
      if(cnt == EOD)
      {
        // 2nd insert
        _info.add((id << 1) + 1, val);
        return true;
      }
      
      if(ptr >= 0)
      {
        // this id has two values stored in-line.
        int firstVal = ptr;
        int secondVal = cnt;
        
        ptr = _buffer.alloc(SEGSIZE);
        _buffer.set(ptr++, EOD);
        _buffer.set(ptr++, firstVal);
        _buffer.set(ptr++, secondVal);
        _buffer.set(ptr++, val);
        cnt = 3;
      }
      else
      {
        ptr = (- ptr);
        if (cnt >= _maxItems) return false; // exceeded the limit
      
        if((ptr % SEGSIZE) == 0)
        {
          int oldPtr = ptr;
          ptr = _buffer.alloc(SEGSIZE);
          _buffer.set(ptr++, (- oldPtr));
        }
        _buffer.set(ptr++, val);
        cnt++;
      }
      
      _info.add(id << 1, (- ptr));
      _info.add((id << 1) + 1, cnt);
      
      return true;
    }

    private final int readToBuf(int id, int[] buf)
    {
      int ptr = _info.get(id << 1);
      int cnt = _info.get((id << 1) + 1);
      int i;
      
      if(ptr >=0)
      {
        // read in-line data
        i = 0;
        buf[i++] = ptr;
        if(cnt >= 0) buf[i++] = cnt;
        return i;
      }
      
      // read from segments
      i = cnt;
      while(ptr != EOD)
      {
        ptr = (- ptr) - 1;
        int val;
        while((val = _buffer.get(ptr--)) >= 0)
        {
          buf[--i] = val;
        }
        ptr = val;
      }
      if(i > 0)
      {
        throw new RuntimeException("error reading buffered data back");
      }
      
      return cnt;
    }

    public final void load()
    {
      int[] buf = new int[MAX_ITEMS];
      int size = _size;
      for(int i = 0; i < size; i++)
      {
        int count = readToBuf(i, buf);
        if(count > 0)
        {
          add(i, buf, 0, count);
        }
      }
    }
    
    public final int capacity()
    {
      return _info.capacity() >> 1;
    }
  }
}
