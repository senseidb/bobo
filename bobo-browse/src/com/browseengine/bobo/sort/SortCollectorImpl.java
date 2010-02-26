package com.browseengine.bobo.sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BoboSubBrowser;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.util.ListMerger;

public class SortCollectorImpl extends SortCollector {
  private final LinkedList<DocIDPriorityQueue> _pqList;
  private final int _numHits;
  private int _totalHits;
  private MyScoreDoc _bottom;
  private MyScoreDoc _tmpScoreDoc;
  private boolean _queueFull;
  private DocComparator _currentComparator;
  private DocComparatorSource _compSource;
  private DocIDPriorityQueue _currentQueue;
  private BoboIndexReader _currentReader=null;
  
  private final boolean _doScoring;
  private float _maxScore;
  private Scorer _scorer;
  private final int _offset;
  private final int _count;
  
  private final Map<String,FacetHandler<?>> _facetHandlerMap;
	
  static class MyScoreDoc extends ScoreDoc {
    private static final long serialVersionUID = 1L;
    
    DocIDPriorityQueue queue;
    BoboIndexReader _srcReader;
    public MyScoreDoc(){
    	this(0,0.0f,null,null);
    }
    
    public MyScoreDoc(int docid, float score, DocIDPriorityQueue queue,BoboIndexReader reader) {
      super(docid, score);
      this.queue = queue;
      _srcReader = reader;
    }
    
    Comparable getValue(){
    	return queue.sortValue(this);
    }
  }
	
  public SortCollectorImpl(DocComparatorSource compSource,SortField[] sortFields,BoboSubBrowser boboBrowser,int offset,int count,boolean doScoring,boolean fetchStoredFields) {
	super(sortFields,fetchStoredFields);
    assert (offset>=0 && count>0);
	_facetHandlerMap = boboBrowser.getFacetHandlerMap();
    _compSource = compSource;
    _pqList = new LinkedList<DocIDPriorityQueue>();
    _numHits = offset + count;
    _offset = offset;
    _count = count;
    _totalHits = 0;
    _maxScore = 0.0f;
    _queueFull = false;
    _doScoring = doScoring;
    _tmpScoreDoc = new MyScoreDoc();
  }

  @Override
  public boolean acceptsDocsOutOfOrder() {
    return _collector == null ? true : _collector.acceptsDocsOutOfOrder();
  }

  @Override
  public void collect(int doc) throws IOException {
    _totalHits++;
    float score;
    if (_doScoring){
 	   score = _scorer.score();
 	   _maxScore+=score;
    }
    else{
 	   score = 0.0f;
    }
    
    if (_queueFull){
      _tmpScoreDoc.doc = doc;
      _tmpScoreDoc.score = score;
      
      if (_currentComparator.compare(_bottom,_tmpScoreDoc)<=0){
        return;
      }
      MyScoreDoc tmp = _bottom;
      _bottom = (MyScoreDoc)_currentQueue.replace(_tmpScoreDoc);
      _tmpScoreDoc = tmp;
    }
    else{ 
      _bottom = (MyScoreDoc)_currentQueue.add(new MyScoreDoc(doc,score,_currentQueue,_currentReader));
      _queueFull = (_currentQueue.size() >= _numHits);
    }
    
    if (_collector != null) _collector.collect(doc);
  }

  @Override
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
	assert reader instanceof BoboIndexReader;
	_currentReader = (BoboIndexReader)reader;
    _currentComparator = _compSource.getComparator(reader,docBase);
    _currentQueue = new DocIDPriorityQueue(_currentComparator,
                                           _numHits, docBase);

    _tmpScoreDoc.queue = _currentQueue;
    _tmpScoreDoc._srcReader = _currentReader;
    _pqList.add(_currentQueue);
    _queueFull = false;
  }

  @Override
  public void setScorer(Scorer scorer) throws IOException {
	  _scorer = scorer;
	  _currentComparator.setScorer(scorer);
  }

  @Override
  public int getTotalHits(){
    return _totalHits;
  }
	
  @Override
  public BrowseHit[] topDocs() throws IOException{
    ArrayList<Iterator<MyScoreDoc>> iterList = new ArrayList<Iterator<MyScoreDoc>>(_pqList.size());
    for (DocIDPriorityQueue pq : _pqList){
      int count = pq.size();
      MyScoreDoc[] resList = new MyScoreDoc[count];
      for (int i = count - 1; i >= 0; i--) { 
    	  resList[i] = (MyScoreDoc)pq.pop();
      }
      iterList.add(Arrays.asList(resList).iterator());
    }
    
    ArrayList<MyScoreDoc> resList = ListMerger.mergeLists(_offset, _count, iterList, new Comparator<MyScoreDoc>() {

        public int compare(MyScoreDoc o1, MyScoreDoc o2) {
          Comparable s1 = o1.getValue();
          Comparable s2 = o2.getValue();
          int r;
          if (s1 == null) {
            if (s2 == null) {
              r = 0;
            } else {
              r = -1;
            }
          } else if (s2 == null) {
            r = 1;
          }
            else{
            int v = s1.compareTo(s2);
            if (v==0){
              r = o1.doc + o1.queue.base - o2.doc - o2.queue.base;
            } else {
              r = v;
            }
          }
          return r;
        }
      });
    return buildHits(resList.toArray(new MyScoreDoc[resList.size()]), _sortFields, _facetHandlerMap, _fetchStoredFields);
  }
  
  protected static BrowseHit[] buildHits(MyScoreDoc[] scoreDocs,SortField[] sortFields,Map<String,FacetHandler<?>> facetHandlerMap,boolean fetchStoredFields)
    throws IOException
  {
    ArrayList<BrowseHit> hitList = new ArrayList<BrowseHit>(scoreDocs.length);
    Collection<FacetHandler<?>> facetHandlers= facetHandlerMap.values();
    for (MyScoreDoc fdoc : scoreDocs)
    {
      BoboIndexReader reader = fdoc._srcReader;
      BrowseHit hit=new BrowseHit();
      if (fetchStoredFields){
         
         hit.setStoredFields(reader.document(fdoc.doc));
      }
      Map<String,String[]> map = new HashMap<String,String[]>();
      for (FacetHandler<?> facetHandler : facetHandlers)
      {
          map.put(facetHandler.getName(),facetHandler.getFieldValues(reader,fdoc.doc));//-fdoc.queue.base));
      }
      hit.setFieldValues(map);
      hit.setDocid(fdoc.doc+fdoc.queue.base);
      hit.setScore(fdoc.score);
      hit.setComparable(fdoc.getValue());
      hitList.add(hit);
    }
    BrowseHit[] hits = hitList.toArray(new BrowseHit[hitList.size()]);
    return hits;
  }
}
