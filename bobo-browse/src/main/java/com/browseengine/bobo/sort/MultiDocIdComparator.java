package com.browseengine.bobo.sort;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;


public class MultiDocIdComparator extends DocComparator {
	private final DocComparator[] _comparators;
	
	public MultiDocIdComparator(DocComparator[] comparators){
		_comparators = comparators;
	}
	
	public int compare(ScoreDoc doc1, ScoreDoc doc2) {
		for (int i=0;i<_comparators.length;++i){
			int v=_comparators[i].compare(doc1, doc2);
			if (v!=0) return v;
		}
		return 0;
	}

	public MultiDocIdComparator setScorer(Scorer scorer){
	  for (DocComparator comparator : _comparators){
	    comparator.setScorer(scorer);
	  }
    return this;
	}
	
	@Override
	public Comparable value(ScoreDoc doc) {
		return new MultiDocIdComparable(doc, _comparators);
	}
	
	public static class MultiDocIdComparable implements Comparable
	{
	  private ScoreDoc _doc;
	  private DocComparator[] _comparators;

	  public MultiDocIdComparable(ScoreDoc doc, DocComparator[] comparators)
	  {
	    _doc = doc;
	    _comparators = comparators;
	  }
	  
	  public int compareTo(Object o)
      {
	    MultiDocIdComparable other = (MultiDocIdComparable)o;
        Comparable c1,c2;
        for (int i=0;i<_comparators.length;++i){
            c1 = _comparators[i].value(_doc);
            c2 = other._comparators[i].value(other._doc);
            int v = c1.compareTo(c2);
            if (v!=0) {
                return v;
            }
        }
        return 0;
      }
	}
}
