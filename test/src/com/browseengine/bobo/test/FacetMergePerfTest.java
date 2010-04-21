package com.browseengine.bobo.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.CombinedFacetAccessible;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermIntList;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler.SimpleFacetCountCollector;
import com.browseengine.bobo.util.BigIntArray;
import com.browseengine.bobo.util.BoboSimpleDecimalFormat;

public class FacetMergePerfTest {
	static int numVals = 100000;
	static int numDocs = 5000000;
	static int numSegs =20;
	static int numDocsPerSeg = numDocs/numSegs;
	static Random rand = new Random();
	
	static FacetDataCache makeFacetDataCache(){
		FacetDataCache cache = new FacetDataCache();
		cache.freqs = new int[numVals];
		/*for (int i=0;i<cache.freqs.length;++i){
			int v = Math.abs(rand.nextInt(numDocs-1))+1;
			cache.freqs[i]=v;
		}*/
		Arrays.fill(cache.freqs,1);
		cache.maxIDs = new int[numVals];
		cache.minIDs = new int[numVals];
		cache.valArray = new TermIntList(numVals,"0000000000");
		BoboSimpleDecimalFormat formatter = BoboSimpleDecimalFormat.getInstance(10);
		
		for (int i=0;i<numVals;++i){
			cache.valArray.add(formatter.format(i+1));
		}
		cache.orderArray = new BigIntArray(numDocsPerSeg);
		return cache;
	}
	
	static FacetAccessible buildSubAccessible(String name,int segment,FacetSpec fspec){
		
		SimpleFacetCountCollector collector = new SimpleFacetCountCollector(name,makeFacetDataCache(),numDocsPerSeg*segment,null,fspec);
		collector.collectAll();
		return collector;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		int nThreads = 1;//0;
		final int numIters = 5;
		int numSegs = 20;
		
		String fname1 = "facet1";
		String fname2 = "facet2";
		final FacetSpec fspec = new FacetSpec();
		fspec.setExpandSelection(true);
		fspec.setMaxCount(50);
		fspec.setMinHitCount(1);
		fspec.setOrderBy(FacetSortSpec.OrderHitsDesc);
		
		final List<FacetAccessible> list1 = new ArrayList<FacetAccessible>(numSegs);
		for (int i=0;i<numSegs;++i){
			list1.add(buildSubAccessible(fname1, i, fspec));
		}
		
		final List<FacetAccessible> list2 = new ArrayList<FacetAccessible>(numSegs);
		for (int i=0;i<numSegs;++i){
			list2.add(buildSubAccessible(fname2, i, fspec));
		}		
		
		final AtomicLong timeCounter = new AtomicLong();
		Thread[] threads = new Thread[nThreads];
		for (int i =0;i<threads.length;++i){
			threads[i]=new Thread(new Runnable(){
				
				public void run() {
					
					for (int i=0;i<numIters;++i){
					  long start = System.nanoTime();
					  final CombinedFacetAccessible combined1 = new CombinedFacetAccessible(fspec, list1);
					 // final CombinedFacetAccessible combined2 = new CombinedFacetAccessible(fspec, list2);
					  List<BrowseFacet> facets1 = combined1.getFacets();
					  //List<BrowseFacet> facets2 = combined2.getFacets();
					  long end= System.nanoTime();
					  timeCounter.getAndAdd(end-start);
					}
				}
				
			});
		}

		System.out.println("press key to start load test... ");
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			int ch = br.read();
			char c = (char) ch;
		}
		for (Thread t : threads){
			t.start();
		}
		
		for (Thread t : threads){
			t.join();
		}
		
		System.out.println("average time: "+timeCounter.get()/numIters/nThreads/1000000+" ms");
		
	}
	
	public static void main2(String[] args) {
		//Comparable c = "00000000001";
		//Comparable c2 ="00000000002";
	//	Comparable c = Integer.valueOf(1);
	//	Comparable c2 = Integer.valueOf(2);
		
		int v1 =1;
		int v2=2;
		long s=System.currentTimeMillis();
		for (int i =0;i<1000000;++i){
			//c.compareTo(c2);
			//c.equals(c2);
			boolean b = v1==v2;
		}
		long e=System.currentTimeMillis();
		
		System.out.println("timeL: "+(e-s));
	}

}

