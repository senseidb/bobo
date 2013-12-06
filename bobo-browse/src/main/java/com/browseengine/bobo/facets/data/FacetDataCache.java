package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.IOException;
import java.io.Serializable;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.BytesRef;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandler.TermCountSize;
import com.browseengine.bobo.sort.DocComparator;
import com.browseengine.bobo.sort.DocComparatorSource;
import com.browseengine.bobo.util.BigByteArray;
import com.browseengine.bobo.util.BigIntArray;
import com.browseengine.bobo.util.BigSegmentedArray;
import com.browseengine.bobo.util.BigShortArray;

public class FacetDataCache<T> implements Serializable {
  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public BigSegmentedArray orderArray;
  public TermValueList<T> valArray;
  public int[] freqs;
  public int[] minIDs;
  public int[] maxIDs;

  public FacetDataCache(BigSegmentedArray orderArray, TermValueList<T> valArray, int[] freqs,
      int[] minIDs, int[] maxIDs, TermCountSize termCountSize) {
    this.orderArray = orderArray;
    this.valArray = valArray;
    this.freqs = freqs;
    this.minIDs = minIDs;
    this.maxIDs = maxIDs;
  }

  public FacetDataCache() {
    this.orderArray = null;
    this.valArray = null;
    this.maxIDs = null;
    this.minIDs = null;
    this.freqs = null;
  }

  public int getNumItems(int docid) {
    int valIdx = orderArray.get(docid);
    return valIdx <= 0 ? 0 : 1;
  }

  private final static BigSegmentedArray newInstance(int termCount, int maxDoc) {
    // we use < instead of <= to take into consideration "missing" value (zero element in the
    // dictionary)
    if (termCount < Byte.MAX_VALUE) {
      return new BigByteArray(maxDoc);
    } else if (termCount < Short.MAX_VALUE) {
      return new BigShortArray(maxDoc);
    } else return new BigIntArray(maxDoc);
  }

  protected int getDictValueCount(AtomicReader reader, String field) throws IOException {
    int ret = 0;
    Terms terms = reader.terms(field);
    if (terms == null) {
      return ret;
    }
    return (int) terms.size();
  }

  protected int getNegativeValueCount(AtomicReader reader, String field) throws IOException {
    int ret = 0;
    Terms terms = reader.terms(field);
    if (terms == null) {
      return ret;
    }
    TermsEnum termsEnum = terms.iterator(null);
    BytesRef text;
    while ((text = termsEnum.next()) != null) {
      if (!text.utf8ToString().startsWith("-")) {
        break;
      }
      ret++;
    }
    return ret;
  }

  public void load(String fieldName, AtomicReader reader, TermListFactory<T> listFactory)
      throws IOException {
    String field = fieldName.intern();
    int maxDoc = reader.maxDoc();

    int dictValueCount = getDictValueCount(reader, fieldName);
    BigSegmentedArray order = newInstance(dictValueCount, maxDoc);
    this.orderArray = order;

    IntArrayList minIDList = new IntArrayList();
    IntArrayList maxIDList = new IntArrayList();
    IntArrayList freqList = new IntArrayList();

    int length = maxDoc + 1;
    @SuppressWarnings("unchecked")
    TermValueList<T> list = listFactory == null ? (TermValueList<T>) new TermStringList()
        : listFactory.createTermList();
    int negativeValueCount = getNegativeValueCount(reader, field);

    int t = 0; // current term number
    list.add(null);
    minIDList.add(-1);
    maxIDList.add(-1);
    freqList.add(0);
    int totalFreq = 0;
    t++;
    Terms terms = reader.terms(field);
    if (terms != null) {
      TermsEnum termsEnum = terms.iterator(null);
      BytesRef text;
      while ((text = termsEnum.next()) != null) {
        // store term text
        // we expect that there is at most one term per document
        if (t >= length) throw new RuntimeException("there are more terms than "
            + "documents in field \"" + field + "\", but it's impossible to sort on "
            + "tokenized fields");
        String strText = text.utf8ToString();
        list.add(strText);
        Term term = new Term(field, strText);
        DocsEnum docsEnum = reader.termDocsEnum(term);
        // freqList.add(termEnum.docFreq()); // doesn't take into account
        // deldocs
        int minID = -1;
        int maxID = -1;
        int docID = -1;
        int df = 0;
        int valId = (t - 1 < negativeValueCount) ? (negativeValueCount - t + 1) : t;
        while ((docID = docsEnum.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
          df++;
          order.add(docID, valId);
          minID = docID;
          while (docsEnum.nextDoc() != DocsEnum.NO_MORE_DOCS) {
            docID = docsEnum.docID();
            df++;
            order.add(docID, valId);
          }
          maxID = docID;
        }
        freqList.add(df);
        totalFreq += df;
        minIDList.add(minID);
        maxIDList.add(maxID);
        t++;
      }
    }

    list.seal();
    this.valArray = list;
    this.freqs = freqList.toIntArray();
    this.minIDs = minIDList.toIntArray();
    this.maxIDs = maxIDList.toIntArray();

    int doc = 0;
    while (doc <= maxDoc && order.get(doc) != 0) {
      ++doc;
    }
    if (doc <= maxDoc) {
      this.minIDs[0] = doc;
      // Try to get the max
      doc = maxDoc;
      while (doc > 0 && order.get(doc) != 0) {
        --doc;
      }
      if (doc > 0) {
        this.maxIDs[0] = doc;
      }
    }
    this.freqs[0] = maxDoc + 1 - totalFreq;
  }

  private static int[] convertString(FacetDataCache<?> dataCache, String[] vals) {
    IntList list = new IntArrayList(vals.length);
    for (int i = 0; i < vals.length; ++i) {
      int index = dataCache.valArray.indexOf(vals[i]);
      if (index >= 0) {
        list.add(index);
      }
    }
    return list.toIntArray();
  }

  /**
   * Same as convert(FacetDataCache dataCache,String[] vals) except that the
   * values are supplied in raw form so that we can take advantage of the type
   * information to find index faster.
   *
   * @param <T>
   * @param dataCache
   * @param vals
   * @return the array of order indices of the values.
   */
  public static <T> int[] convert(FacetDataCache<T> dataCache, T[] vals) {
    if (vals != null && (vals instanceof String[])) return convertString(dataCache, (String[]) vals);
    IntList list = new IntArrayList(vals.length);
    for (int i = 0; i < vals.length; ++i) {
      int index = dataCache.valArray.indexOfWithType(vals[i]);
      if (index >= 0) {
        list.add(index);
      }
    }
    return list.toIntArray();
  }

  public static class FacetDocComparatorSource extends DocComparatorSource {
    private final FacetHandler<FacetDataCache<?>> _facetHandler;

    public FacetDocComparatorSource(FacetHandler<FacetDataCache<?>> facetHandler) {
      _facetHandler = facetHandler;
    }

    @Override
    public DocComparator getComparator(AtomicReader reader, int docbase) throws IOException {
      if (!(reader instanceof BoboSegmentReader)) throw new IllegalStateException(
          "reader not instance of " + BoboSegmentReader.class);
      BoboSegmentReader boboReader = (BoboSegmentReader) reader;
      final FacetDataCache<?> dataCache = _facetHandler.getFacetData(boboReader);
      final BigSegmentedArray orderArray = dataCache.orderArray;
      return new DocComparator() {
        @Override
        public Comparable<?> value(ScoreDoc doc) {
          int index = orderArray.get(doc.doc);
          return dataCache.valArray.getComparableValue(index);
        }

        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          return orderArray.get(doc1.doc) - orderArray.get(doc2.doc);
        }
      };
    }
  }
}
