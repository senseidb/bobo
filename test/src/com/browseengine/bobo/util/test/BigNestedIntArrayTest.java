/**
 * 
 */
package com.browseengine.bobo.util.test;

import com.browseengine.bobo.util.BigIntBuffer;
import com.browseengine.bobo.util.BigNestedIntArray;
import com.browseengine.bobo.util.BigNestedIntArray.BufferedLoader;
import com.browseengine.bobo.util.BigNestedIntArray.Loader;
import org.apache.lucene.util.BitVector;

import java.util.Random;
import java.util.Arrays;

import junit.framework.TestCase;

/**
 * @author ymatsuda
 *
 */
public class BigNestedIntArrayTest extends TestCase
{
  public void testBasic() throws Throwable
  {
    int maxId = 3000;
    int[] count = new int[maxId];
    BufferedLoader loader = new BufferedLoader(maxId);
    for(int id = 0; id < maxId; id++)
    {
      for(int val = 0; val < 2000; val += (id + 1))
      {
        if(loader.add(id, val)) count[id]++;
      }
    }
    BigNestedIntArray nestedArray = new BigNestedIntArray();
    nestedArray.load(maxId, loader);
    
    int[] buf = new int[1024];
    for(int id = 0; id < maxId; id++)
    {
      int cnt = nestedArray.getData(id, buf);
      assertEquals("item count", count[id], cnt);
      
      if(cnt > 0)
      {
        int val = 0;
        for(int i = 0; i < cnt; i++)
        {
          assertEquals("item["+i+"]", val, buf[i]);
          val += (id + 1);
        }
      }
    }
  }
  
  public void testSparseIds() throws Throwable
  {
    int maxId = 100000;
    int[] count = new int[maxId];
    BufferedLoader loader = new BufferedLoader(maxId);
    for(int id = 0; id < maxId; id += ((id >> 2) + 1))
    {
      for(int val = 0; val < 3000; val += (id + 1))
      {
        if(loader.add(id, val)) count[id]++;
      }
    }
    BigNestedIntArray nestedArray = new BigNestedIntArray();
    nestedArray.load(maxId, loader);
    
    int[] buf = new int[1024];
    for(int id = 0; id < maxId; id++)
    {
      int cnt = nestedArray.getData(id, buf);
      assertEquals("item count", count[id], cnt);
      
      if(cnt > 0)
      {
        int val = 0;
        for(int i = 0; i < cnt; i++)
        {
          assertEquals("item["+i+"]", val, buf[i]);
          val += (id + 1);
        }
      }
    }
  }
  
  public void testBufferedLoaderReuse() throws Throwable
  {
    int maxId = 5000;
    int[] maxNumItems = { 25, 50, 20, 100, 15, 500, 10, 1000, 5, 2000, 2 };
    int[][] count = new int[maxNumItems.length][maxId];
    BigIntBuffer buffer = new BigIntBuffer();
    BufferedLoader loader = new BufferedLoader(maxId, BigNestedIntArray.MAX_ITEMS, buffer);
    BigNestedIntArray[] nestedArray = new BigNestedIntArray[maxNumItems.length];
    
    for(int i = 0 ; i < maxNumItems.length; i++)
    {
      for(int id = 0; id < maxId; id++)
      {
        int cnt = id % (maxNumItems[i] + 1);
        for(int val = 0; val < cnt; val++)
        {
          if(loader.add(id, val)) count[i][id]++;
        }
      }
      nestedArray[i] = new BigNestedIntArray();
      nestedArray[i].load(maxId, loader);
      
      loader.reset(maxId, BigNestedIntArray.MAX_ITEMS, buffer);
    }
    
    for(int i = 0 ; i < maxNumItems.length; i++)
    {
      int[] buf = new int[1024];
      for(int id = 0; id < maxId; id++)
      {
        int cnt = nestedArray[i].getData(id, buf);
        assertEquals("count["+i+","+id+"]", count[i][id], cnt);
      
        if(cnt > 0)
        {
          for(int val = 0; val < cnt; val++)
          {
            assertEquals("item["+i+","+id+","+val+"]", val, buf[val]);
          }
        }
      }
    }
  }
  
  public void testMemoryReuse() throws Throwable
  {
    int maxId = 4096;
    int[] maxNumItems = {1, 1, 2, 2, 3, 3, 3, 3, 1, 1 };
    int[] minNumItems = {1, 1, 0, 1, 0, 0, 2, 3, 1, 0 };
    int[] count = new int[maxId];
    BigIntBuffer buffer = new BigIntBuffer();
    BufferedLoader loader = null;
    BigNestedIntArray nestedArray = new BigNestedIntArray();
    Random rand = new Random();
    
    for(int i = 0; i < maxNumItems.length; i++)
    {
      loader = new BufferedLoader(maxId, BigNestedIntArray.MAX_ITEMS, buffer);
      for(int id = 0; id < maxId; id++)
      {
        count[id] = 0;
        int cnt = Math.max(rand.nextInt(maxNumItems[i] + 1), minNumItems[i]); 
        for(int val = 0; val < cnt; val++)
        {
          if(loader.add(id, val)) count[id]++;
        }
      }
      
      nestedArray.load(maxId, loader);
      
      int[] buf = new int[1024];
      for(int id = 0; id < maxId; id++)
      {
        int cnt = nestedArray.getData(id, buf);
        assertEquals("count["+i+","+id+"]", count[id], cnt);
        
        if(cnt > 0)
        {
          for(int val = 0; val < cnt; val++)
          {
            assertEquals("item["+i+","+id+","+val+"]", val, buf[val]);
          }
        }
      }
      
      if(i == 0)
      {
        maxId = maxId * 2;
        count = new int[maxId];
      }
    }
  }
  
  public void testAllocThenAddData() throws Throwable
  {
    int maxId = 5000;
    int[] maxNumItems = { 25, 50, 20, 100, 15, 500, 10, 1000, 5, 1024, 2 };
    int[][] count = new int[maxNumItems.length][maxId];
    AllocOnlyTestLoader loader = new AllocOnlyTestLoader(maxId);
    BigNestedIntArray[] nestedArray = new BigNestedIntArray[maxNumItems.length];
    
    for(int i = 0 ; i < maxNumItems.length; i++)
    {
      for(int id = 0; id < maxId; id++)
      {
        int cnt = id % (maxNumItems[i] + 1);
        loader.addSize(id, cnt);
        count[i][id] = cnt;
      }
      nestedArray[i] = new BigNestedIntArray();
      nestedArray[i].load(maxId, loader);
      loader.reset();

      for(int id = 0; id < maxId; id++)
      {
        for(int data = 0; data < count[i][id]; data++)
        {
          nestedArray[i].addData(id, data);
        }
      }
    }
    
    for(int i = 0 ; i < maxNumItems.length; i++)
    {
      int[] buf = new int[1024];
      for(int id = 0; id < maxId; id++)
      {
        int cnt = nestedArray[i].getData(id, buf);
        assertEquals("count["+i+","+id+"]", count[i][id], cnt);
      
        if(cnt > 0)
        {
          for(int val = 0; val < cnt; val++)
          {
            assertEquals("item["+i+","+id+","+val+"]", val, buf[val]);
          }
        }
      }
    }
  }
  
  /**
   * A loader that allocate data storage wihtout loading data to BigNestedIntArray.
   * Note that this loader supports only non-negative integer data.
   */
  public final static class AllocOnlyTestLoader extends Loader
  {
    private int[] _maxNumItems;
    
    public AllocOnlyTestLoader(int maxdoc)
    {
      _maxNumItems = new int[maxdoc];
    }

    public void addSize(int docid, int size)
    {
      _maxNumItems[docid] = size;
    }
    
    public void reset()
    {
      Arrays.fill(_maxNumItems, 0);
    }
    
    @Override
    public void load() throws Exception
    {
      for(int i = 0; i < _maxNumItems.length; i++)
      {
        if(_maxNumItems[i] > 0)
        {
          allocate(i, _maxNumItems[i], true);
        }
      }
    }
  }
  
  public void testMaxItems() throws Throwable
  {
    int maxId = 5000;
    int[] maxNumItems = { 25, 50, 20, 100, 15, 500, 10, 1000, 5, 1024, 2 };
    int[][] count = new int[maxNumItems.length][maxId];
    AllocOnlyTestLoader loader = new AllocOnlyTestLoader(maxId);
    BigNestedIntArray[] nestedArray = new BigNestedIntArray[maxNumItems.length];
    
    for(int i = 0 ; i < maxNumItems.length; i++)
    {
      for(int id = 0; id < maxId; id++)
      {
        int cnt = id % 2000;
        loader.addSize(id, cnt);
        count[i][id] = cnt;
      }
      nestedArray[i] = new BigNestedIntArray();
      nestedArray[i].setMaxItems(maxNumItems[i]);
      nestedArray[i].load(maxId, loader);
      loader.reset();

      for(int id = 0; id < maxId; id++)
      {
        boolean failed = false;
        for(int data = 0; data < count[i][id]; data++)
        {
          if(nestedArray[i].addData(id, data))
          {
            if(!failed && (data + 1 > maxNumItems[i]))
            {
              failed = true;
              assertEquals("maxItems", data, maxNumItems[i]);
            }
          }
        }
      }
    }
  }

  public void testCountNoReturnWithFilter() throws Throwable
  {
    int maxId = 20;
    int numVals = 10;
    int[] count = new int[numVals];
    
    BufferedLoader loader = new BufferedLoader(maxId);
    for (int val = 0; val < numVals; val++)
    {
      for (int i = 0; i < maxId - val; i++)
      {
        loader.add(i, val);
      }
    }

    BigNestedIntArray nestedArray = new BigNestedIntArray();
    nestedArray.load(maxId, loader);

    BitVector filter = new BitVector(numVals);
    for (int i = 0; i < numVals; i++)
    {
      if (i % 2 == 0)
      {
        filter.set(i);
      }
    }

    for (int i = 0; i < maxId; i++)
    {
      nestedArray.countNoReturnWithFilter(i, count, filter);
    }

    for (int i = 0; i < numVals; i++)
    {
      if (i % 2 == 0) 
      {
        assertTrue(count[i] == maxId - i);
      }
      else
      {
        assertTrue(count[i] == 0);
      }
    }
    return;
  }
}
