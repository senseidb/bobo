package com.browseengine.bobo.perf;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.lucene.index.IndexReader;

import com.browseengine.bobo.perf.BrowseThread.Stats;
import com.browseengine.bobo.perf.BrowseThread.StatsCollector;

public class DocumentFetchPerf extends AbstractPerfTest {

	public static final String NUM_DOCS_TO_FETCH = "numdocs.to.fetch";
	public static final String NUM_ITER = "num.iter";

	private int _numDocsToFetch;
	private int _numIter;
	private final Random _rand;

	public DocumentFetchPerf(PropertiesConfiguration propConf)
			throws IOException {
		super(propConf);
		_rand = new Random();
		try {
			_numDocsToFetch = Integer.parseInt(_propConf
					.getString(NUM_DOCS_TO_FETCH));
		} catch (Exception e) {
			_numDocsToFetch = 50;
		}

		try {
			_numIter = Integer.parseInt(_propConf.getString(NUM_ITER));
		} catch (Exception e) {
			_numIter = 50;
		}
	}

	@Override
	public Thread buildWorkThread() {
		return new FetchDocThread(_rand, _numDocsToFetch, luceneReader,
				throttleWait, _numIter, this);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File propFile = new File(args[0]);
		DocumentFetchPerf perf = new DocumentFetchPerf(
				new PropertiesConfiguration(propFile));
		perf.start();
	}

	private static class FetchDocThread extends Thread {
		private final Random _rand;
		private final int[] docsToFetch;
		private final IndexReader _idxReader;
		private final StatsCollector _collector;
		private final long _throttleWait;
		private final int _numIter;

		FetchDocThread(Random rand, int numToFetch, IndexReader reader,
				long throttleWait, int numIter, StatsCollector statsCollector) {
			_idxReader = reader;
			_rand = rand;
			_throttleWait = throttleWait;
			_numIter = numIter;
			_collector = statsCollector;

			int maxDoc = reader.maxDoc();

			IntSet idSet = new IntOpenHashSet();
			while (idSet.size() < numToFetch) {
				int docid = _rand.nextInt(maxDoc);
				if (!idSet.contains(docid) && !reader.isDeleted(docid)) {
					idSet.add(docid);
				}
			}

			docsToFetch = idSet.toIntArray();
		}

		@Override
		public void run() {
			for (int i = 0; i < _numIter; ++i) {
				try {
					for (int docid : docsToFetch) {

						long start = System.nanoTime();
						_idxReader.document(docid);
						long end = System.nanoTime();
						_collector.collect(new Stats((end - start), null));
					}
				} catch (Exception e) {
					e.printStackTrace();
					_collector.collect(new Stats(0L, e));
				}
				try {
					Thread.sleep(_throttleWait);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
