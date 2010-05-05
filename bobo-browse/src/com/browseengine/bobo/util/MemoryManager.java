/**
 * 
 */
package com.browseengine.bobo.util;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

/**
 * @author "Xiaoyang Gu<xgu@linkedin.com>"
 *
 */
public class MemoryManager<T>
{
  private static final Logger log = Logger.getLogger(MemoryManager.class.getName());

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

  /**
   * @return an initialized instance of type T.
   */
  public T get(int size)
  {
    ConcurrentLinkedQueue<WeakReference<T>> queue = _sizeMap.get(size);
    if (queue==null)
    {
      queue =  new ConcurrentLinkedQueue<WeakReference<T>>();
      _sizeMap.putIfAbsent(size, queue);
      queue = _sizeMap.get(size);
    }
    while(true)
    {
      WeakReference<T> ref = (WeakReference<T>) queue.poll();
      if(ref != null)
      {
        T buf = ref.get();
        if(buf != null)
        {
          return buf;
        }
      }
      else
      {
        return _initializer.newInstance(size);
      }
    }
  }

  /**
   * return the instance to the manager after use
   * @param buf
   */
  public void release(T buf)
  {
    if (_releaseQueueSize.get()>8000)
    {
      log.info("release queue full");
      synchronized(MemoryManager.this)
      {
        MemoryManager.this.notifyAll();
      }
      return;
    }
    if(buf != null)
    {
      _releaseQueue.offer(buf);
      _releaseQueueSize.incrementAndGet();
      synchronized(MemoryManager.this)
      {
        MemoryManager.this.notifyAll();
      }
    }
  }

  public static interface Initializer<E>
  {
    public E newInstance(int size);
    public int size(E buf);
    public void init(E buf);
  }
}
