package com.browseengine.bobo.test;

import java.util.Random;

import com.browseengine.bobo.util.BigIntArray;

public class MemoryUtil {

  static int max = 5000000;

  static int[] getIndex(int count) {
    Random rand = new Random();
    int[] array = new int[count];

    for (int i = 0; i < count; ++i) {
      array[i] = rand.nextInt(max);
    }
    // Arrays.sort(array);
    return array;
  }

  private static class RunnerThread extends Thread {
    private final int[] array;
    private final BigIntArray bigarray;

    RunnerThread(int[] a, BigIntArray b) {
      array = a;
      bigarray = b;
    }

    @Override
    public void run() {
      long start = System.currentTimeMillis();
      for (int val : array) {
        bigarray.get(val);
      }
      long end = System.currentTimeMillis();
      System.out.println("time: " + (end - start));
    }
  }

  static void time1(final int[][] array) throws InterruptedException {
    int iter = array.length;
    final BigIntArray bigArray = new BigIntArray(max);
    Thread[] threads = new Thread[iter];

    for (int i = 0; i < iter; ++i) {
      threads[i] = new RunnerThread(array[i], bigArray);
    }

    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }
  }

  /**
   * @param args
   * @throws InterruptedException
   */
  public static void main(String[] args) throws InterruptedException {
    int threadCount = 10;
    int numIter = 1000000;
    int[][] indexesPerThread = new int[threadCount][];
    for (int i = 0; i < threadCount; ++i) {
      indexesPerThread[i] = getIndex(numIter);
    }
    time1(indexesPerThread);
    System.out.println("Test completed.");
  }

}
