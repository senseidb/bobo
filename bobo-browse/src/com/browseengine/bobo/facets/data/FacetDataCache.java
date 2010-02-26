package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;
import java.io.Serializable;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.ScoreDoc;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandler.TermCountSize;
import com.browseengine.bobo.sort.DocComparator;
import com.browseengine.bobo.sort.DocComparatorSource;
import com.browseengine.bobo.util.BigByteArray;
import com.browseengine.bobo.util.BigIntArray;
import com.browseengine.bobo.util.BigSegmentedArray;
import com.browseengine.bobo.util.BigShortArray;

public class FacetDataCache implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public BigSegmentedArray orderArray;
	public TermValueList valArray;
	public int[] freqs;
	public int[] minIDs;
	public int[] maxIDs;
	private final TermCountSize _termCountSize;
	
	public FacetDataCache(BigSegmentedArray orderArray,TermValueList valArray,int[] freqs,int[] minIDs,int[] maxIDs,TermCountSize termCountSize)
	{
		this.orderArray=orderArray;
		this.valArray=valArray;
		this.freqs=freqs;
		this.minIDs=minIDs;
		this.maxIDs=maxIDs;
		_termCountSize = termCountSize;
	}
	
	public FacetDataCache()
	{
	  this.orderArray = null;
	  this.valArray = null;
	  this.maxIDs = null;
	  this.minIDs = null;
	  this.freqs = null;
	  _termCountSize = TermCountSize.large;
	}
	
	private final static BigSegmentedArray newInstance(TermCountSize termCountSize,int maxDoc){
		if (termCountSize == TermCountSize.small){
			return new BigByteArray(maxDoc);
		}
		else if (termCountSize == TermCountSize.medium){
			return new BigShortArray(maxDoc);
		}
		else return new BigIntArray(maxDoc);
	}
	
	public void load(String fieldName,IndexReader reader,TermListFactory listFactory) throws IOException
	{
	  String field = fieldName.intern();
      int maxDoc=reader.maxDoc();
      

      BigSegmentedArray order=this.orderArray;
      if (order == null)                        // we want to reuse the memory
      {
        order = newInstance(_termCountSize,maxDoc);
      }
      else
      {
        order.ensureCapacity(maxDoc);  // no need to fill to 0, we are reseting the data anyway
      }
      this.orderArray = order;
        
      IntArrayList minIDList=new IntArrayList();
      IntArrayList maxIDList=new IntArrayList();
      IntArrayList freqList = new IntArrayList();
        
      int length=maxDoc+1;
      TermValueList list = listFactory == null ? new TermStringList() : listFactory.createTermList();
        TermDocs termDocs = reader.termDocs();
        TermEnum termEnum = reader.terms (new Term (field, ""));
        int t = 0;  // current term number

        list.add(null);
        minIDList.add(-1);
        maxIDList.add(-1);
        freqList.add(0);
        //int df = 0;
        t++;
        try {
          do {
            Term term = termEnum.term();
            if (term==null || term.field() != field) break;

            if (t > order.maxValue()) {
				throw new IOException("maximum number of value cannot exceed: "
						+ order.maxValue());
			}
            // store term text
            // we expect that there is at most one term per document
            if (t >= length) throw new RuntimeException ("there are more terms than " +
                    "documents in field \"" + field + "\", but it's impossible to sort on " +
                    "tokenized fields");
            list.add(term.text());
            termDocs.seek (termEnum);
            // freqList.add(termEnum.docFreq()); // doesn't take into account deldocs
            int minID=-1;
            int maxID=-1;
            int df = 0;
            if(termDocs.next())
            {
              df++;
              int docid = termDocs.doc();
              order.add(docid, t);
              minID = docid;
              while (termDocs.next())
              {
                df++;
                docid = termDocs.doc();
                order.add(docid, t);
              }
              maxID = docid;
            }
            freqList.add(df);
            minIDList.add(minID);
            maxIDList.add(maxID);
            
            t++;
          } while (termEnum.next());
        } finally {
          termDocs.close();
          termEnum.close();
        }
        list.seal();

        this.valArray = list;
        this.freqs = freqList.toIntArray();
        this.minIDs = minIDList.toIntArray();
        this.maxIDs = maxIDList.toIntArray();
	}
	
	public static int[] convert(FacetDataCache dataCache,String[] vals)
	{
	    IntList list = new IntArrayList(vals.length);
	    for (int i=0;i<vals.length;++i)
	    {
	      int index = dataCache.valArray.indexOf(vals[i]);
	      if (index>=0)
	      {
	        list.add(index);
	      }
	    }
	    return list.toIntArray();
	}
	
	public static class FacetDocComparatorSource extends DocComparatorSource{
		private FacetHandler<FacetDataCache> _facetHandler;
		public FacetDocComparatorSource(FacetHandler<FacetDataCache> facetHandler){
			_facetHandler = facetHandler;
		}
		
		@Override
		public DocComparator getComparator(IndexReader reader, int docbase)
				throws IOException {
			if (!(reader instanceof BoboIndexReader)) throw new IllegalStateException("reader not instance of "+BoboIndexReader.class);
			BoboIndexReader boboReader = (BoboIndexReader)reader;
			final FacetDataCache dataCache = _facetHandler.getFacetData(boboReader);
			final BigSegmentedArray orderArray = dataCache.orderArray;
			return new DocComparator() {
				
				@Override
				public Comparable value(ScoreDoc doc) {
					int index = orderArray.get(doc.doc);
			        return dataCache.valArray.get(index);
				}
				
				@Override
				public int compare(ScoreDoc doc1, ScoreDoc doc2) {
					return orderArray.get(doc1.doc) - orderArray.get(doc2.doc);	
				}
			};
		}
	}
}

