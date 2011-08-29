package com.browseengine.bobo.sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.Browsable;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.impl.SimpleFacetHandler;
import com.browseengine.bobo.facets.CombinedFacetAccessible;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.util.ListMerger;

public class SortCollectorImpl extends SortCollector {
  private static final Comparator<MyScoreDoc> MERGE_COMPATATOR = new Comparator<MyScoreDoc>()
  {
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
  };

  private final LinkedList<DocIDPriorityQueue> _pqList;
  private final int _numHits;
  private int _totalHits;
  private int _totalGroups;
  private ScoreDoc _bottom;
  private ScoreDoc _tmpScoreDoc;
  private boolean _queueFull;
  private DocComparator _currentComparator;
  private DocComparatorSource _compSource;
  private DocIDPriorityQueue _currentQueue;
  private BoboIndexReader _currentReader=null;
  private FacetCountCollector _facetCountCollector;

  private final boolean _doScoring;
  private Scorer _scorer;
  private final int _offset;
  private final int _count;

  private final Browsable _boboBrowser;
  private final FacetHandler<?> _groupBy;
  private CombinedFacetAccessible _groupAccessible;
  private final List<FacetAccessible> _facetAccessibles;
  private final Map<Integer, ScoreDoc> _currentValueDocMaps;
  static class MyScoreDoc extends ScoreDoc {
    private static final long serialVersionUID = 1L;

    DocIDPriorityQueue queue;
    BoboIndexReader reader;
    Comparable sortValue;
    
    public MyScoreDoc(){
      this(0,0.0f,null,null);
    }

    public MyScoreDoc(int docid, float score, DocIDPriorityQueue queue,BoboIndexReader reader) {
      super(docid, score);
      this.queue = queue;
      this.reader = reader;
      this.sortValue = null;
    }

    Comparable getValue(){
      if(sortValue == null)
        sortValue = queue.sortValue(this);
      return sortValue;
    }
  }

  public SortCollectorImpl(DocComparatorSource compSource,
                           SortField[] sortFields,
                           Browsable boboBrowser,
                           int offset,
                           int count,
                           boolean doScoring,
                           boolean fetchStoredFields,
                           String groupBy) {
    super(sortFields,fetchStoredFields);
    assert (offset>=0 && count>=0);
    _boboBrowser = boboBrowser;
    _compSource = compSource;
    _pqList = new LinkedList<DocIDPriorityQueue>();
    _numHits = offset + count;
    _offset = offset;
    _count = count;
    _totalHits = 0;
    _totalGroups = 0;
    _queueFull = false;
    _doScoring = doScoring;
    _tmpScoreDoc = new MyScoreDoc();
    if (groupBy != null) {
      _groupBy = boboBrowser.getFacetHandler(groupBy);
      if (_groupBy != null && _count > 0) {
        _currentValueDocMaps = new HashMap<Integer, ScoreDoc>(_count);
        _facetAccessibles = new LinkedList<FacetAccessible>();
      }
      else {
        _currentValueDocMaps = null;
        _facetAccessibles = null;
      }
    }
    else {
      _groupBy = null;
      _currentValueDocMaps = null;
      _facetAccessibles = null;
    }
  }

  @Override
  public boolean acceptsDocsOutOfOrder() {
    return _collector == null ? true : _collector.acceptsDocsOutOfOrder();
  }

  @Override
  public void collect(int doc) throws IOException {
    ++_totalHits;

    if (_groupBy != null) {
      if (_facetCountCollector != null)
        _facetCountCollector.collect(doc);
      //Object val = _groupBy.getRawFieldValues(_currentReader, doc)[0];

      if (_count > 0) {
        final float score = (_doScoring ? _scorer.score() : 0.0f);
        _tmpScoreDoc.doc = doc;
        _tmpScoreDoc.score = score;
        if (!_queueFull || _currentComparator.compare(_bottom,_tmpScoreDoc) > 0) {
          final Integer order = ((FacetDataCache)_groupBy.getFacetData(_currentReader)).orderArray.get(doc);
          ScoreDoc pre = _currentValueDocMaps.get(order);
          if (pre != null) {
            if ( _currentComparator.compare(pre, _tmpScoreDoc) > 0) {
              ScoreDoc tmp = pre;
              _bottom = _currentQueue.replace(_tmpScoreDoc, pre);
              _currentValueDocMaps.put(order, _tmpScoreDoc);
              _tmpScoreDoc = tmp;
            }
          }
          else {
            if (_queueFull){
              MyScoreDoc tmp = (MyScoreDoc)_bottom;
              _currentValueDocMaps.remove(((FacetDataCache)_groupBy.getFacetData(tmp.reader)).orderArray.get(tmp.doc));
              _bottom = _currentQueue.replace(_tmpScoreDoc);
              _currentValueDocMaps.put(order, _tmpScoreDoc);
              _tmpScoreDoc = tmp;
            }
            else{ 
              ScoreDoc tmp = new MyScoreDoc(doc,score,_currentQueue,_currentReader);
              _bottom = _currentQueue.add(tmp);
              _currentValueDocMaps.put(order, tmp);
              _queueFull = (_currentQueue.size >= _numHits);
            }
          }
        }
      }
    }
    else {
      if (_count > 0) {
        final float score = (_doScoring ? _scorer.score() : 0.0f);

        if (_queueFull){
          _tmpScoreDoc.doc = doc;
          _tmpScoreDoc.score = score;
    
          if (_currentComparator.compare(_bottom,_tmpScoreDoc) > 0){
            ScoreDoc tmp = _bottom;
            _bottom = _currentQueue.replace(_tmpScoreDoc);
            _tmpScoreDoc = tmp;
          }
        }
        else{ 
          _bottom = _currentQueue.add(new MyScoreDoc(doc,score,_currentQueue,_currentReader));
          _queueFull = (_currentQueue.size >= _numHits);
        }
      }
    }

    if (_collector != null) _collector.collect(doc);
  }

  private void collectTotalGrous() {
    if (_facetCountCollector instanceof SimpleFacetHandler.SimpleGroupByFacetCountCollector) {
      _totalGroups += ((SimpleFacetHandler.SimpleGroupByFacetCountCollector)_facetCountCollector).getTotalGroups();
      return;
    }

    int[] count = _facetCountCollector.getCountDistribution();
    for (int c : count) {
      if (c > 0)
        ++_totalGroups;
    }
  }

  @Override
  public void setNextReader(IndexReader reader, int docBase) throws IOException {
    assert reader instanceof BoboIndexReader;
    _currentReader = (BoboIndexReader)reader;
    _currentComparator = _compSource.getComparator(reader,docBase);
    _currentQueue = new DocIDPriorityQueue(_currentComparator, _numHits, docBase);
    if (_groupBy != null) {
      if (_facetCountCollector != null)
        collectTotalGrous();
      if (_groupBy instanceof SimpleFacetHandler)
        _facetCountCollector = ((SimpleFacetHandler)_groupBy).getFacetCountCollectorSource(null, null, true).getFacetCountCollector(_currentReader, docBase);
      else
        _facetCountCollector = _groupBy.getFacetCountCollectorSource(null, null).getFacetCountCollector(_currentReader, docBase);
      if (_facetAccessibles != null)
        _facetAccessibles.add(_facetCountCollector);
      if (_currentValueDocMaps != null)
        _currentValueDocMaps.clear();
    }
    MyScoreDoc myScoreDoc = (MyScoreDoc)_tmpScoreDoc;
    myScoreDoc.queue = _currentQueue;
    myScoreDoc.reader = _currentReader;
    myScoreDoc.sortValue = null;
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
  public int getTotalGroups(){
    return _totalGroups;
  }

  @Override
  public CombinedFacetAccessible getGroupAccessible(){
    return _groupAccessible;
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

    List<MyScoreDoc> resList;
    if (_count > 0) {
      if (_groupBy == null) {
        resList = ListMerger.mergeLists(_offset, _count, iterList, MERGE_COMPATATOR);
      }
      else {
        if (_facetCountCollector != null)
        {
          collectTotalGrous();
          _facetCountCollector = null;
        }
        _groupAccessible = new CombinedFacetAccessible(new FacetSpec(), _facetAccessibles);
        resList = new ArrayList<MyScoreDoc>(_count);
        Iterator<MyScoreDoc> mergedIter = ListMerger.mergeLists(iterList, MERGE_COMPATATOR);
        Set<Object> groupSet = new HashSet<Object>(_offset+_count);
        int offsetLeft = _offset;
        while(mergedIter.hasNext())
        {
          MyScoreDoc scoreDoc = mergedIter.next();
          Object val = _groupBy.getRawFieldValues(scoreDoc.reader, scoreDoc.doc)[0];
          if (!groupSet.contains(val))
          {
            if (offsetLeft > 0)
              --offsetLeft;
            else
              resList.add(scoreDoc);
            groupSet.add(val);
          }
          if (resList.size() >= _count)
            break;
        }
      }
    }
    else
      resList = Collections.EMPTY_LIST;

    Map<String,FacetHandler<?>> facetHandlerMap = _boboBrowser.getFacetHandlerMap();
    return buildHits(resList.toArray(new MyScoreDoc[resList.size()]), _sortFields, facetHandlerMap, _fetchStoredFields, _groupBy, _groupAccessible);
  }

  protected static BrowseHit[] buildHits(MyScoreDoc[] scoreDocs,SortField[] sortFields,Map<String,FacetHandler<?>> facetHandlerMap,boolean fetchStoredFields, FacetHandler<?> groupBy, CombinedFacetAccessible groupAccessible)
  throws IOException
  {
    BrowseHit[] hits = new BrowseHit[scoreDocs.length];
    Collection<FacetHandler<?>> facetHandlers= facetHandlerMap.values();
    for (int i =scoreDocs.length-1; i >=0 ; i--)
    {
      MyScoreDoc fdoc = scoreDocs[i];
      BoboIndexReader reader = fdoc.reader;
      BrowseHit hit=new BrowseHit();
      if (fetchStoredFields){

        hit.setStoredFields(reader.document(fdoc.doc));
      }
      Map<String,String[]> map = new HashMap<String,String[]>();
      Map<String,Object[]> rawMap = new HashMap<String,Object[]>();
      for (FacetHandler<?> facetHandler : facetHandlers)
      {
          map.put(facetHandler.getName(),facetHandler.getFieldValues(reader,fdoc.doc));
          rawMap.put(facetHandler.getName(),facetHandler.getRawFieldValues(reader,fdoc.doc));
      }
      hit.setFieldValues(map);
      hit.setRawFieldValues(rawMap);
      hit.setDocid(fdoc.doc+fdoc.queue.base);
      hit.setScore(fdoc.score);
      hit.setComparable(fdoc.getValue());
      if (groupBy != null) {
        hit.setGroupValue(hit.getField(groupBy.getName()));
        if (hit.getGroupValue() != null && groupAccessible != null) {
          BrowseFacet facet = groupAccessible.getFacet(hit.getGroupValue());
          hit.setGroupHitsCount(facet.getFacetValueHitCount());
        }
      }
      hits[i] = hit;
    }
    return hits;
  }
}
