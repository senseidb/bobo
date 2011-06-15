package com.browseengine.bobo.sort;

import org.apache.log4j.Logger;
import org.apache.lucene.search.ScoreDoc;

public class DocIDPriorityQueue {
  private static Logger logger = Logger.getLogger(DocIDPriorityQueue.class.getName());
  public int size;
  final protected ScoreDoc[] heap;
  public final int base;

  private final DocComparator comparator;

  public DocIDPriorityQueue(DocComparator comparator, int maxSize, int base) {
    this.comparator = comparator;
    size = 0;
    this.base = base;
    int heapSize;
    if (0 == maxSize)
      // We allocate 1 extra to avoid if statement in top()
      heapSize = 2;
    else
      heapSize = maxSize + 1;
    heap = new ScoreDoc[heapSize];
  }

  /**
   * Adds an Object to a PriorityQueue in log(size) time. If one tries to add
   * more objects than maxSize from initialize an
   * {@link ArrayIndexOutOfBoundsException} is thrown.
   * 
   * @return the new 'bottom' element in the queue.
   */
  public final ScoreDoc add(ScoreDoc element) {
    size++;
    heap[size] = element;
    upHeap(size);
    return heap[1];
  }

  public Comparable<?> sortValue(ScoreDoc doc) {
    return comparator.value(doc);
  }

  private final int compare(ScoreDoc doc1, ScoreDoc doc2) {
    final int cmp = comparator.compare(doc1, doc2);
    if (cmp != 0) {
      return -cmp;
    } else {
      return doc2.doc - doc1.doc;
    }
  }

  public ScoreDoc replace(ScoreDoc element) {
    heap[1] = element;
    downHeap(1);
    return heap[1];
  }

  /**
   * Takes O(size) time.
   *
   * @return the 'bottom' element in the queue.
   **/
  public ScoreDoc replace(ScoreDoc newEle, ScoreDoc oldEle) {
    for (int i=1; i<=size; ++i) {
      if (heap[i] == oldEle) {
        heap[i] = newEle;
        upHeap(i);
        downHeap(i);
        break;
      }
    }
    return heap[1];
  }

  /** Returns the least element of the PriorityQueue in constant time. */
  public final ScoreDoc top() {
    // We don't need to check size here: if maxSize is 0,
    // then heap is length 2 array with both entries null.
    // If size is 0 then heap[1] is already null.
    return heap[1];
  }

  /** Removes and returns the least element of the PriorityQueue in log(size)
    time. */
  public final ScoreDoc pop() {
    if (size > 0) {
      ScoreDoc result = heap[1];			  // save first value
      heap[1] = heap[size];			  // move last to first
      heap[size] = null;			  // permit GC of objects
      size--;
      downHeap(1);				  // adjust heap
      return result;
    } else
      return null;
  }

  /**
   * Should be called when the Object at top changes values. Still log(n) worst
   * case, but it's at least twice as fast to
   * 
   * <pre>
   * pq.top().change();
   * pq.updateTop();
   * </pre>
   * 
   * instead of
   * 
   * <pre>
   * o = pq.pop();
   * o.change();
   * pq.push(o);
   * </pre>
   * 
   * @return the new 'top' element.
   */
  public final ScoreDoc updateTop() {
    downHeap(1);
    return heap[1];
  }

  /** Returns the number of elements currently stored in the PriorityQueue. */
  public final int size() {
    return size;
  }

  /** Removes all entries from the PriorityQueue. */
  public final void clear() {
    for (int i = 0; i <= size; i++) {
      heap[i] = null;
    }
    size = 0;
  }

  private final void upHeap(int i) {
    ScoreDoc node = heap[i];			  // save bottom node
    int j = i >>> 1;
    while (j > 0 && compare(node, heap[j]) < 0) {
      heap[i] = heap[j];			  // shift parents down
      i = j;
      j = j >>> 1;
    }
    heap[i] = node;				  // install saved node
  }

  private final void downHeap(int i) {
    ScoreDoc node = heap[i];			  // save top node
    int j = i << 1;				  // find smaller child
    int k = j + 1;
    if (k <= size && compare(heap[k], heap[j]) < 0) {
      j = k;
    }
    while (j <= size && compare(heap[j], node) < 0) {
      heap[i] = heap[j];			  // shift up child
      i = j;
      j = i << 1;
      k = j + 1;
      if (k <= size && compare(heap[k], heap[j]) < 0) {
        j = k;
      }
    }
    heap[i] = node;				  // install saved node
  }
}
