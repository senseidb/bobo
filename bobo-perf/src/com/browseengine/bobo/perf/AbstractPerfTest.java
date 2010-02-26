package com.browseengine.bobo.perf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.perf.BrowseThread.Stats;
import com.browseengine.bobo.perf.BrowseThread.StatsCollector;

public abstract class AbstractPerfTest implements StatsCollector {

	protected PropertiesConfiguration _propConf;

	public static final String INDEX_DIR = "index.dir";
	public static final String NUM_REQ = "num.req";
	public static final String NUM_THREADS = "num.threads";
	public static final String THROTTLE_WAIT = "throttle.wait";

	protected Directory idxDir;
	protected int numReq;
	protected int numThreads;
	protected long throttleWait;
	
	protected BoboIndexReader boboReader = null;
	protected IndexReader luceneReader = null;

	public AbstractPerfTest(PropertiesConfiguration propConf)
			throws IOException {
		_propConf = propConf;
		init();
	}

	protected void init() throws IOException {
		String idxDirName = _propConf.getString(INDEX_DIR);
		File idxFile = new File(idxDirName);
		if (!idxFile.isAbsolute()) {
			idxFile = new File(new File("conf"), idxDirName);
		}
		numReq = _propConf.getInt(NUM_REQ);
		numThreads = _propConf.getInt(NUM_THREADS, 10);
		throttleWait = _propConf.getLong(THROTTLE_WAIT, 500L);

		System.out.println("index dir: " + idxFile.getAbsolutePath());
		System.out.println("number of reqs: " + numReq);
		System.out.println("number of threads: " + numThreads);
		System.out.println("throttle wait: " + throttleWait);

		idxDir = FSDirectory.open(idxFile);
		
		System.out.println("loading index...");
	
		luceneReader = IndexReader.open(idxDir, true);
		try {
			boboReader = BoboIndexReader.getInstance(luceneReader);
		} catch (IOException ioe) {
			luceneReader.close();
			luceneReader = null;
			throw ioe;
		}

	}

	abstract public Thread buildWorkThread();

	LinkedList<Stats> statsList = new LinkedList<Stats>();

	public void collect(Stats stats) {
		synchronized (statsList) {
			statsList.add(stats);
		}
	}

	public void start() throws IOException {
		System.out.println("initializing threads...");

		Thread[] threadPool = new Thread[numThreads];
		for (int i = 0; i < threadPool.length; ++i) {
			threadPool[i] = buildWorkThread();
		}

		System.out.println("press key to start load test... ");
		{
			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			int ch = br.read();
			char c = (char) ch;
		}

		long start = System.currentTimeMillis();
		for (int i = 0; i < threadPool.length; ++i) {
			threadPool[i].start();
		}
		try {
			for (int i = 0; i < threadPool.length; ++i) {
				threadPool[i].join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long end = System.currentTimeMillis();
		System.out.println("finished ... ");

		printSummary(statsList, (end - start));
	}

	void printSummary(List<Stats> stats, long totalTime) {
		System.out.println("======= Performance Report=========");
		System.out.println("total time: " + totalTime);
		System.out.println("total reqs processed: " + stats.size());
		System.out.println("QPS: " + stats.size() * 1000 / (totalTime)
				+ "  (max: " + numThreads * (1000 / throttleWait) + ")");
		Stats[] statsArray = stats.toArray(new Stats[stats.size()]);
		long sum = 0L;
		int errCount = 0;
		for (Stats stat : statsArray) {
			sum += stat.getTime();
			if (stat.getException() != null) {
				errCount++;
			}
		}

		Arrays.sort(statsArray, new Comparator<Stats>() {
			public int compare(Stats s1, Stats s2) {
				long val = s1.getTime() - s2.getTime();
				if (val == 0L) {
					val = s1.getCreateTime() - s2.getCreateTime();
				}
				if (val > 0L)
					return 1;
				if (val == 0L)
					return 0;
				return -1;
			}
		});

		if (statsArray.length>0){
		  System.out.println("median time: "
				+ statsArray[statsArray.length / 2].getTime());
		  System.out.println("average time: " + (sum / statsArray.length));
		  System.out.println("error count: " + errCount);
		}
		else{
		  System.out.println("No stats available.");
		}
	}
	
	public void shutdown(){
		try{
			boboReader.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
}
