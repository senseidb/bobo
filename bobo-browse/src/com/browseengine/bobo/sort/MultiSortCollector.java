package com.browseengine.bobo.sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.Browsable;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.MultiBoboBrowser;
import com.browseengine.bobo.util.ListMerger;

public class MultiSortCollector extends SortCollector {

	private static Logger logger = Logger.getLogger(MultiSortCollector.class);
    private int _totalCount;
    private final MultiBoboBrowser _multiBrowser;
    private final SortCollector[] _subCollectors;
    private final int[] _starts;
    private final int _offset;
    private final int _count;
    private Scorer _scorer;
    private int _docBase;
    
    private SortCollector _currentSortCollector;
    private int _currentIndex;
    
	public MultiSortCollector(MultiBoboBrowser multiBrowser,Query q,SortField[] sort,int offset,int count,boolean doScoring,boolean fetchStoredFields){
		super(sort,fetchStoredFields);
	    _offset=offset;
	    _count=count;
	    _multiBrowser = multiBrowser;
	    Browsable[] subBrowsers = _multiBrowser.getSubBrowsers();
	    _subCollectors = new SortCollector[subBrowsers.length];
	    for (int i=0;i<subBrowsers.length;++i)
	    {
	      _subCollectors[i] = subBrowsers[i].getSortCollector(sort, q, 0, _offset+_count,fetchStoredFields,doScoring);
	    }
	    _starts = _multiBrowser.getStarts();
	    _totalCount = 0; 
	    _docBase = 0;
	    _currentIndex = 0;
	    _currentSortCollector = null;
	}

	@Override
	public int getTotalHits() {
		return _totalCount;
	}

	@Override
	public BrowseHit[] topDocs() throws IOException{
		ArrayList<Iterator<BrowseHit>> iteratorList = new ArrayList<Iterator<BrowseHit>>(_subCollectors.length);
	    
	    for (int i=0;i<_subCollectors.length;++i)
	    {
	      int base = _starts[i];
	      try
	      {
	    	BrowseHit[] subHits = _subCollectors[i].topDocs();
	    	if(subHits.length > 0)
	    	{
	    	  iteratorList.add(Arrays.asList(subHits).iterator());	    	  
	    	}
	      }
	      catch(IOException ioe)
	      {
	        logger.error(ioe.getMessage(),ioe);
	      }
	    }
	    
	    Comparator<BrowseHit> comparator = new Comparator<BrowseHit>(){

			public int compare(BrowseHit o1, BrowseHit o2) {
				Comparable c1=o1.getComparable();
				Comparable c2=o2.getComparable();
				if (c1==null || c2==null){
					return o2.getDocid() - o1.getDocid();
				}
				return c1.compareTo(c2);
			}
	    	
	    };
	    
	    ArrayList<BrowseHit> mergedList = ListMerger.mergeLists(_offset, _count, iteratorList.toArray(new Iterator[iteratorList.size()]), comparator);
	    return mergedList.toArray(new BrowseHit[mergedList.size()]);
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return _collector == null ? true : _collector.acceptsDocsOutOfOrder();
	}

	@Override
	public void collect(int doc) throws IOException {
	    _currentSortCollector.collect(doc);
	    _totalCount++;
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		_currentSortCollector = _subCollectors[_currentIndex++];
		_currentSortCollector.setNextReader(reader, docBase);
		_docBase = docBase;
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		_currentSortCollector.setScorer(scorer);
		_scorer = scorer;
	}
	
	
}
