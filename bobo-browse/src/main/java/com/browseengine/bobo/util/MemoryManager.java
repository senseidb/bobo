/**
 * 
 */
package com.browseengine.bobo.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

/**
 * @author "Xiaoyang Gu<xgu@linkedin.com>"
 *
 */
public class MemoryManager<T> implements MemoryManagerAdminMBean
{
  private static final Logger log = Logger.getLogger(MemoryManager.class.getName());
  private static final int[] sizetable;
  private AtomicLong _hits = new AtomicLong(0);
  private AtomicLong _miss = new AtomicLong(0);
  static
  {
    int initsize = 1024;
    double ratio = 1.3;
    int l = (int) (Math.log(Integer.MAX_VALUE/initsize)/ Math.log(ratio)) + 1;
    sizetable = new int[l];
    sizetable[0] = initsize;
    for(int i=1; i< sizetable.length; i++)
    {
      sizetable[i] = (int) (sizetable[i-1]*ratio);
    }
  }
  private final ConcurrentHashMap<Integer, ConcurrentLinkedQueue<WeakReference<T>>> _sizeMap = new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<WeakReference<T>>>();
  private volatile ConcurrentLinkedQueue<T> _releaseQueue = new ConcurrentLinkedQueue<T>();
  private volatile ConcurrentLinkedQueue<T> _releaseQueueb = new ConcurrentLinkedQueue<T>();
  private final AtomicInteger _releaseQueueSize = new AtomicInteger(0);
  private final Initializer<T> _initializer;
  private final Thread _cleanThread;
  public MemoryManager(Initializer<T> initializer)
  {
    this._initializer = initializer;
    _cleanThread = new Thread(new Runnable(){

      public void run()
      {
        T buf = null;
        while(true)
        {
          synchronized(MemoryManager.this)
          {
            try
            {
              MemoryManager.this.wait(10);
            } catch (InterruptedException e)
            {
              log.error(e);
            }
          }
          ConcurrentLinkedQueue<T> t = _releaseQueue;
          _releaseQueue = _releaseQueueb;
          _releaseQueueb =t;
          while((buf = _releaseQueueb.poll()) != null)
          {
            ConcurrentLinkedQueue<WeakReference<T>> queue = _sizeMap.get(_initializer.size(buf));
            // buf is wrapped in WeakReference. this allows GC to reclaim the buffer memory
            _initializer.init(buf);// pre-initializing the buffer in parallel so we save time when it is requested later.
            queue.offer(new WeakReference<T>(buf));
            int x =_releaseQueueSize.decrementAndGet();
          }
          buf = null;
        }
      }});
    _cleanThread.setDaemon(true);
    _cleanThread.start();
  }
  
  public MemoryManagerAdminMBean getAdminMBean(){
      return this;
  }
  
  @Override
	public long getNumCacheMisses() {
		return _miss.get();
	}
	
	@Override
	public long getNumCacheHits() {
		return _hits.get();
	}
	
	@Override
	public double getHitRate() {
		long miss = _miss.get();
	    long hit = _hits.get();
	    return (double)hit/(double)(hit + miss);
	}

  /**
   * @return an initialized instance of type T. The size of the instance may not be the same as the requested size.
   */
  public T get(int reqsize)
  {
    return _initializer.newInstance(reqsize);
    //long t0 = System.currentTimeMillis();
    //int size = reqsize;
    //for(int i = 0; i<sizetable.length; i++)
    //{
      //if (sizetable[i] >= reqsize)
      //{
        //size = sizetable[i];
        //break;
      //}
    //}
    //ConcurrentLinkedQueue<WeakReference<T>> queue = _sizeMap.get(size);
    //if (queue==null)
    //{
      //queue =  new ConcurrentLinkedQueue<WeakReference<T>>();
      //_sizeMap.putIfAbsent(size, queue);
      //queue = _sizeMap.get(size);
    //}
    //while(true)
    //{
      //WeakReference<T> ref = (WeakReference<T>) queue.poll();
      //if(ref != null)
      //{
        //T buf = ref.get();
        //if(buf != null)
        //{
          //_hits.incrementAndGet();
          //return buf;
        //}
      //}
      //else
      //{
        //T ret = _initializer.newInstance(size);
        //_miss.incrementAndGet();
        //long hit = _hits.get();
        //if (hit > Long.MAX_VALUE/2)
        //{
          //_hits.set(0);
          //_miss.set(0);
        //}
        //return ret;
      //}
    //}
  }

  /**
   * return the instance to the manager after use
   * @param buf
   */
  public void release(T buf)
  {
    //if (_releaseQueueSize.get()>8000)
    //{
      //log.info("release queue full");
      //synchronized(MemoryManager.this)
      //{
        //MemoryManager.this.notifyAll();
      //}
      //return;
    //}
    //if(buf != null)
    //{
      //_releaseQueue.offer(buf);
      //_releaseQueueSize.incrementAndGet();
      //synchronized(MemoryManager.this)
      //{
        //MemoryManager.this.notifyAll();
      //}
    //}
  }

  public static interface Initializer<E>
  {
    public E newInstance(int size);
    public int size(E buf);
    public void init(E buf);
  }
}
