package com.browseengine.bobo.facets.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FacetIterator;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FieldValueAccessor;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.FacetCountCollector;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermDoubleList;
import com.browseengine.bobo.facets.data.TermFloatList;
import com.browseengine.bobo.facets.data.TermIntList;
import com.browseengine.bobo.facets.data.TermLongList;
import com.browseengine.bobo.facets.data.TermShortList;
import com.browseengine.bobo.util.BigSegmentedArray;
import com.browseengine.bobo.util.IntBoundedPriorityQueue;
import com.browseengine.bobo.util.MemoryManager;
import com.browseengine.bobo.util.IntBoundedPriorityQueue.IntComparator;

public abstract class DefaultFacetCountCollector implements FacetCountCollector
{
  private static final Logger log = Logger.getLogger(DefaultFacetCountCollector.class.getName());
  protected final FacetSpec _ospec;
  public int[] _count;
  public int _countlength;
  protected FacetDataCache _dataCache;
  private final String _name;
  protected final BrowseSelection _sel;
  protected final BigSegmentedArray _array;
  private int _docBase;
  protected LinkedList<int[]> intarraylist = new LinkedList<int[]>();
  private Iterator _iterator;
  private int[] _scores = null;
  private boolean _normalizeFreq;
  
  public static final String FACET_SCORE_NORMALIZE_MAP = "facetScoreNormalize";

  protected static MemoryManager<int[]> intarraymgr = new MemoryManager<int[]>(new MemoryManager.Initializer<int[]>()
      {

    public void init(int[] buf)
    {
      Arrays.fill(buf, 0);
    }

    public int[] newInstance(int size)
    {
      return new int[size];
    }

    public int size(int[] buf)
    {
      assert buf!=null;
      return buf.length;
    }

      });

  public DefaultFacetCountCollector(String name,FacetDataCache dataCache,int docBase,
      BrowseSelection sel,FacetSpec ospec)
  {
    _sel = sel;
    _ospec = ospec;
    _name = name;
    _dataCache=dataCache;
    if (_dataCache.freqs.length <= 3096)
    {
      _countlength = _dataCache.freqs.length;
      _count = new int[_countlength];
    } else
    {
      _countlength = _dataCache.freqs.length;
      _count = intarraymgr.get(_countlength);//new int[_dataCache.freqs.length];
      intarraylist.add(_count);
    }
    _array = _dataCache.orderArray;
    _docBase = docBase;
    
  }
  
  public void setFacetScoringParams(Map<String,String> facetScoringParams){
	try{
    	_normalizeFreq = Boolean.parseBoolean(facetScoringParams.get(FACET_SCORE_NORMALIZE_MAP));
    }
    catch(Exception e){
    	_normalizeFreq = false;
    }
  }
  
  public void setNormalizeFreq(boolean normalizeFreq){
	  _normalizeFreq = normalizeFreq;
  }

  public String getName()
  {
    return _name;
  }

  abstract public void collect(int docid);

  abstract public void collectAll();
  
  public int[] buildScore(){
	  if (!_normalizeFreq){
	    return _count;
	  }
	  else{
		int[] scores = new int[_count.length];
		int[] freqs = _dataCache.freqs;
		for (int i=0;i<_count.length;++i){
		  int score = 0;
		  if (_count[i]>0){
		    double freq = freqs[i];
		    if (freq>0.0){
		      double cnt = _count[i];
			  score = (int)(cnt/freq*100.0);  	
		    } 
		  }
		  scores[i] = score;
		}
		return scores;
	  }
  }
  
  public final int[] getScores(){
	  if (_scores==null){
		  _scores = buildScore();
	  }
	  return _scores;
  }

  public BrowseFacet getFacet(String value)
  {
    BrowseFacet facet = null;
    int index=_dataCache.valArray.indexOf(value);
    
    int score = 0;
    if (index >=0 ){
      facet = new BrowseFacet(_dataCache.valArray.get(index),_count[index]);
      int[] scores = getScores();
      score = scores[index];
    }
    else{
      facet = new BrowseFacet(_dataCache.valArray.format(value),0);  
    }
    facet.setFacetValueScore(score);
    return facet; 
  }

  public int[] getCountDistribution()
  {
    return _count;
  }

  public List<BrowseFacet> getFacets() {
    if (_ospec!=null)
    {
      int minCount=_ospec.getMinHitCount();
      int max=_ospec.getMaxCount();
      if (max <= 0) max=_countlength;
      
      int[] scores = getScores();

      List<BrowseFacet> facetColl;
      List<String> valList=_dataCache.valArray;
      FacetSortSpec sortspec = _ospec.getOrderBy();
      if (sortspec == FacetSortSpec.OrderValueAsc)
      {
        facetColl=new ArrayList<BrowseFacet>(max);
        for (int i = 1; i < _countlength;++i) // exclude zero
        {
          int hits=_count[i];
          if (hits>=minCount)
          {
            BrowseFacet facet=new BrowseFacet(valList.get(i),hits);
            facet.setFacetValueScore(scores[i]);
            System.out.println("DefaultFacetCountCollector: Value --> " + valList.get(i));
            facetColl.add(facet);
          }
          if (facetColl.size()>=max) break;
        }
      }
      else //if (sortspec == FacetSortSpec.OrderHitsDesc)
      {
        ComparatorFactory comparatorFactory;
        if (sortspec == FacetSortSpec.OrderHitsDesc){
          comparatorFactory = new FacetHitcountComparatorFactory();
        }
        else if (sortspec == FacetSortSpec.OrderScoreDesc){
          comparatorFactory = new FacetScoreComparatorFactory();
        }
        else{
          comparatorFactory = _ospec.getCustomComparatorFactory();
        }

        if (comparatorFactory == null){
          throw new IllegalArgumentException("facet comparator factory not specified");
        }

        final IntComparator comparator = comparatorFactory.newComparator(new FieldValueAccessor(){

          public String getFormatedValue(int index) {
            return _dataCache.valArray.get(index);
          }

          public Object getRawValue(int index) {
            return _dataCache.valArray.getRawValue(index);
          }

        }, _count,scores);
        facetColl=new LinkedList<BrowseFacet>();
        final int forbidden = -1;
        IntBoundedPriorityQueue pq=new IntBoundedPriorityQueue(comparator,max, forbidden);

        for (int i=1;i<_countlength;++i)
        {
          int hits=_count[i];
          if (hits>=minCount)
          {
            pq.offer(i);
          }
        }

        int val;
        while((val = pq.pollInt()) != forbidden)
        {
          BrowseFacet facet=new BrowseFacet(valList.get(val),_count[val]);
          facet.setFacetValueScore(scores[val]);
          ((LinkedList<BrowseFacet>)facetColl).addFirst(facet);
        }
      }
      return facetColl;
    }
    else
    {
      return FacetCountCollector.EMPTY_FACET_LIST;
    }
  }

  public void close()
  {
    for(int[] buf: intarraylist)
    {
      intarraymgr.release(buf);
    }
  }

  /**
   * This function returns an Iterator to visit the facets in value order
   * @return	The Iterator to iterate over the facets in value order
   */
  public FacetIterator iterator() {
	int[] scores = getScores();
    if (_dataCache.valArray.getType().equals(Integer.class))
    {
      return new DefaultIntFacetIterator((TermIntList) _dataCache.valArray, _count, scores,_countlength, false);
    } else if (_dataCache.valArray.getType().equals(Long.class))
    {
      return new DefaultLongFacetIterator((TermLongList) _dataCache.valArray, _count, scores,_countlength, false);
    } else if (_dataCache.valArray.getType().equals(Short.class))
    {
      return new DefaultShortFacetIterator((TermShortList) _dataCache.valArray, _count,scores, _countlength, false);
    } else if (_dataCache.valArray.getType().equals(Float.class))
    {
      return new DefaultFloatFacetIterator((TermFloatList) _dataCache.valArray, _count, scores,_countlength, false);
    } else if (_dataCache.valArray.getType().equals(Double.class))
    {
      return new DefaultDoubleFacetIterator((TermDoubleList) _dataCache.valArray, _count, scores,_countlength, false);
    } else
    return new DefaultFacetIterator(_dataCache.valArray, _count, scores,_countlength, false);
  }
}
