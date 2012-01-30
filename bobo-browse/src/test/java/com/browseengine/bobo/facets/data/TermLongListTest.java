package com.browseengine.bobo.facets.data;

import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.Test;

public class TermLongListTest extends TestCase {

  @Test
  public void test1TwoNegativeValues() {
    TermLongList list = new TermLongList();       
    list.add(null);
    list.add("-1");
    list.add("-2");   
    list.add("0");
    list.add("1");
   
    list.seal();
    assertTrue( Arrays.equals(new long[] {0, -2, -1, 0, 1 }, list.getElements()));
  }
  @Test
  public void test2ThreeNegativeValues() {
    TermLongList list = new TermLongList();       
    list.add(null);
    list.add("-1");
    list.add("-2"); 
    list.add("-3"); 
    list.add("0");
    list.add("1");
   
    list.seal();
    assertTrue( Arrays.equals(new long[] {0, -3, -2, -1, 0, 1 }, list.getElements()));
  }
  @Test
  public void test2aThreeNegativeValuesInt() {
    TermIntList list = new TermIntList();       
    list.add(null);
    list.add("-1");
    list.add("-2"); 
    list.add("-3"); 
    list.add("0");
    list.add("1");
   
    list.seal();
    assertTrue( Arrays.equals(new int[] {0, -3, -2, -1, 0, 1 }, list.getElements()));
  }
  @Test
  public void test2bThreeNegativeValuesShort() {
    TermShortList list = new TermShortList();       
    list.add(null);
    list.add("-1");
    list.add("-2"); 
    list.add("-3"); 
    list.add("0");
    list.add("1");
   
    list.seal();
    assertTrue( Arrays.equals(new short[] {0, -3, -2, -1, 0, 1 }, list.getElements()));
  }
  @Test
  public void test3ThreeNegativeValuesWithoutDummy() {
    TermLongList list = new TermLongList();      
    
    list.add("-1");
    list.add("-2"); 
    list.add("-3"); 
    list.add("0");
    list.add("1");
   
    list.seal();
    assertTrue( Arrays.equals(new long[] {-3, -2, -1, 0, 1 }, list.getElements()));
  }
}
