package com.browseengine.bobo.perf;

import com.browseengine.bobo.api.BoboBrowser;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.perf.RequestFactory.ReqIterator;

public class BrowseThread extends Thread {
	public static class Stats {
		private long _time;
		private long _createTime;
		private Exception _exception;

		public Stats(long time, Exception exception) {
			_time = time;
			_exception = exception;
			_createTime = System.nanoTime();
		}

		public long getCreateTime() {
			return _createTime;
		}

		public long getTime() {
			return _time;
		}

		public Exception getException() {
			return _exception;
		}
	}

	public interface StatsCollector {
		void collect(Stats stats);
	}

	private BoboIndexReader _reader;
	private ReqIterator _iter;
	private long _throttleWait;
	private StatsCollector _collector;

	public BrowseThread(BoboIndexReader reader, ReqIterator reqIter,
			long throttleWait, StatsCollector statsCollector) {
		super("bobo perf thread");
		_reader = reader;
		_iter = reqIter;
		_throttleWait = throttleWait;
		_collector = statsCollector;
	}

	public void run() {
		while (true) {
			BrowseRequest req = _iter.next();
			if (req != null) {
				BoboBrowser svc = null;
				long time = 0L;
				Exception ex = null;
				try {
					svc = new BoboBrowser(_reader);
					BrowseResult res = svc.browse(req);
					// System.out.println("num hits: "+res.getNumHits());
					time = res.getTime();
				} catch (Exception e) {
					e.printStackTrace();
					ex = e;
				} finally {
					if (_collector != null) {
						_collector.collect(new Stats(time, ex));
					}
					try {
						svc.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				try {
					Thread.sleep(_throttleWait);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				break;
			}
		}
	}

}
