package com.browseengine.bobo.util.test;

import org.apache.lucene.search.DocIdSetIterator;

import junit.framework.TestCase;

import com.browseengine.bobo.util.BigByteArray;
import com.browseengine.bobo.util.BigIntArray;
import com.browseengine.bobo.util.BigSegmentedArray;
import com.browseengine.bobo.util.BigShortArray;
import com.browseengine.bobo.util.LazyBigIntArray;

/**
 * @author jko
 * 
 * Test BigSegmentedArray
 */
public class BigSegmentedArrayTest extends TestCase
{
  
  
  public static void testEmptyArray()
  {
    emptyArrayTestHelper(new BigIntArray(0));
    emptyArrayTestHelper(new BigByteArray(0));
    emptyArrayTestHelper(new BigShortArray(0));
    emptyArrayTestHelper(new LazyBigIntArray(0));
  }
  
  private static void emptyArrayTestHelper(BigSegmentedArray array)
  {
    assertEquals(0, array.get(0));
    assertEquals(0, array.size());
  }
  
  public static void testCountUp()
  {
    countUpTestHelper(new BigIntArray(Short.MAX_VALUE * 2));
    countUpTestHelper(new LazyBigIntArray(Short.MAX_VALUE * 2));
    countUpTestHelper(new BigShortArray(Short.MAX_VALUE * 2));
    countUpTestHelper(new BigByteArray(Short.MAX_VALUE * 2));
  }
  
  private static void countUpTestHelper(BigSegmentedArray array)
  {
    initialize(array);
    assertEquals(Short.MAX_VALUE  * 2, array.size());
    for (int i = 0; i < array.size(); i++)
    {
      assertEquals(i % array.maxValue(), array.get(i));
    }
  }
  
  public static void testFindValues()
  {
    findValueHelper(new BigIntArray(Short.MAX_VALUE * 2));
    findValueHelper(new LazyBigIntArray(Short.MAX_VALUE * 2));
    findValueHelper(new BigShortArray(Short.MAX_VALUE * 2));
    findValueHelper(new BigByteArray(Short.MAX_VALUE * 2));
  }
  
  private static void findValueHelper(BigSegmentedArray array)
  {
    final int a = array.maxValue() / 16;
    final int b = a * 2;
    final int c = a * 3;
    
    array.add(1000, a);
    array.add(2000, b);
    assertEquals(1000, array.findValue(a, 0, 2000));
    assertEquals(DocIdSetIterator.NO_MORE_DOCS, array.findValue(a, 1001, 2000));
    assertEquals(2000, array.findValue(b, 2000, 3000));
    
    array.fill(c);
    assertEquals(DocIdSetIterator.NO_MORE_DOCS, array.findValue(b, 2000, 3000));
    assertEquals(4000, array.findValue(c, 4000, 4000));
  }
  
  public static void testFindValueRange()
  {
    findValueRangeHelper(new BigIntArray(Short.MAX_VALUE * 2));
    findValueRangeHelper(new LazyBigIntArray(Short.MAX_VALUE * 2));
    findValueRangeHelper(new BigShortArray(Short.MAX_VALUE * 2));
    findValueRangeHelper(new BigByteArray(Short.MAX_VALUE * 2));
  }
  
  private static void findValueRangeHelper(BigSegmentedArray array)
  {
    final int a = array.maxValue() / 16;
    final int b = a * 2;
    final int c = a * 3;
    final int d = a * 4;
    final int e = a * 5;
    final int f = a * 6;
    
    array.add(10000, b);
    assertEquals(DocIdSetIterator.NO_MORE_DOCS, array.findValueRange(d, e, 0, array.size()));
    assertEquals(10000, array.findValueRange(a, e, 0, array.size()));
    assertEquals(10000, array.findValueRange(a, e, 10000, array.size()));
    assertEquals(10000, array.findValueRange(a, e, 0, 10000));

    assertEquals(10000, array.findValueRange(a, b, 9000, 10100));
    assertEquals(10000, array.findValueRange(b, e, 9000, 10000));
    assertEquals(10000, array.findValueRange(b, b, 9000, 10000));
}
  
  public static void testFill()
  {
    fillTestHelper(new BigIntArray(Short.MAX_VALUE << 1));
    fillTestHelper(new LazyBigIntArray(Short.MAX_VALUE << 1));
    fillTestHelper(new BigShortArray(Short.MAX_VALUE << 1));
    fillTestHelper(new BigByteArray(Short.MAX_VALUE << 1));
  }
  
  private static void fillTestHelper(BigSegmentedArray array)
  {
    final int a = array.maxValue() / 4;
    final int b = array.maxValue() / 2;
    final int c = array.maxValue() - 1;
    
    assertEquals(0, array.get(20000));
    
    array.fill(a);
    assertEquals(a, array.get(20000));
    
    array.add(20000, b);
    assertEquals(b, array.get(20000));
    assertEquals(a, array.get(20001));
    
    assertEquals(20000, array.findValue(b, 0, 21000));
    
    array.fill(c);
    assertEquals(c, array.get(20000));
    assertEquals(c, array.get(40000));
    assertEquals(c, array.get(0));
}
  
  public static BigSegmentedArray initialize(BigSegmentedArray array)
  {
    for (int i = 0; i < array.size(); i++)
    {
      array.add(i, i % array.maxValue());
    }
    return array;
  }
  
}
