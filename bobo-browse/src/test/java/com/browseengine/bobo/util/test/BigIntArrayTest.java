/**
 * 
 */
package com.browseengine.bobo.util.test;

import com.browseengine.bobo.util.BigIntArray;

import junit.framework.TestCase;

public class BigIntArrayTest extends TestCase
{
  public static void testBigIntArray()
  {
    int count = 5000000;
    BigIntArray test = new BigIntArray(count);
    int[] test2 = new int[count];
    for (int i = 0; i < count; i++)
    {
      test.add(i, i);
      test2[i]=i;
    }
    
    for (int i = 0; i< count; i++)
    {
      assertEquals(0, test.get(0));
    }
    
    int k = 0;
    long start = System.currentTimeMillis();
    for (int i = 0; i < count; i++)
    {
      k = test.get(i);
    }
    long end = System.currentTimeMillis();
    System.out.println("Big array took: "+(end-start));
    
    start = System.currentTimeMillis();
    for (int i = 0; i < count; i++)
    {
      k = test2[i];
    }
    end=System.currentTimeMillis();
    System.out.println("int[] took: "+(end-start));
  }
}
