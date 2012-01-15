package com.browseengine.bobo.test;

import java.util.Random;

import com.browseengine.bobo.util.BigIntArray;

public class MemoryUtil
{
  
  static int max = 5000000;

  static int[] getIndex(int count)
  {
    Random rand = new Random();
    int[] array=new int[count];
    
    for (int i=0;i<count;++i)
    {
      array[i]=rand.nextInt(max);
    }
    //Arrays.sort(array);
    return array;
  }
  
  private static class RunnerThread2 extends Thread
  {
    private int[] array;
    private BigIntArray bigarray;
    
    RunnerThread2(int[] a,BigIntArray b)
    {
      array=a;
      bigarray=b;
    }
    
    public void run()
    {
      long start=System.currentTimeMillis();
      for (int val : array)
      {
        int x = bigarray.get(val);
      }

      long end=System.currentTimeMillis();
      System.out.println("time: "+(end-start));
    }
  }

  
  static void time1(final int[][] array)
  {
    int iter = array.length;
    final BigIntArray bigArray = new BigIntArray(max);
    Thread[] threads=new Thread[iter];
    
    for (int i=0;i<iter;++i)
    {
      threads[i]=new RunnerThread2(array[i],bigArray);
    }
    
    for (Thread t : threads)
    {
      t.start();
    }
  }
  
  private static class RunnerThread extends Thread
  {
    private int[] array;
    private int[] bigarray;
    
    RunnerThread(int[] a,int[] b)
    {
      array=a;
      bigarray=b;
    }
    
    public void run()
    {
      long start=System.currentTimeMillis();
      for (int val : array)
      {
        int x = bigarray[val];
      }

      long end=System.currentTimeMillis();
      System.out.println("time: "+(end-start));
    }
  }
  
  static void time2(final int[][] array)
  {
    int iter = array.length;
    final int[] bigArray=new int[max];
    Thread[] threads=new Thread[iter];
    for (int i=0;i<iter;++i)
    {
      threads[i]=new RunnerThread(array[i],bigArray);
    }
    
    for (Thread t : threads)
    {
      t.start();
    }
  }
  /**
   * @param args
   */
  public static void main(String[] args)
  {
    int threadCount=10;
    int numIter = 1000000;
    int[][] indexesPerThread=new int[threadCount][];
    for (int i=0;i<threadCount;++i)
    {
      indexesPerThread[i]=getIndex(numIter);
    }
    
    time1(indexesPerThread);

    //time2(indexesPerThread);
  }

}
