package com.browseengine.bobo.sort;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;

public class LuceneCustomDocComparatorSource extends DocComparatorSource {
	private final FieldComparator<Comparable> _luceneComparator;
	private final String _fieldname;
	public LuceneCustomDocComparatorSource(String fieldname,FieldComparator<Comparable> luceneComparator){
		_fieldname = fieldname;
		_luceneComparator = luceneComparator;
	}
	
	@Override
	public DocComparator getComparator(IndexReader reader, int docbase)
			throws IOException {
		_luceneComparator.setNextReader(reader, docbase);
		return new DocComparator() {
			
			@Override
			public Comparable value(ScoreDoc doc) {
				return _luceneComparator.value(doc.doc);
			}
			
			@Override
			public int compare(ScoreDoc doc1, ScoreDoc doc2) {
				return _luceneComparator.compare(doc1.doc, doc2.doc);
			}

			@Override
			public DocComparator setScorer(Scorer scorer) {
				_luceneComparator.setScorer(scorer);
        return this;
			}
		};
	}

}
