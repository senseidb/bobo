/**
 *
 */
package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;

import com.browseengine.bobo.api.BoboSegmentReader;
import com.browseengine.bobo.api.BoboSegmentReader.WorkArea;
import com.browseengine.bobo.facets.range.MultiDataCacheBuilder;
import com.browseengine.bobo.sort.DocComparator;
import com.browseengine.bobo.sort.DocComparatorSource;
import com.browseengine.bobo.util.BigIntBuffer;
import com.browseengine.bobo.util.BigNestedIntArray;
import com.browseengine.bobo.util.BigNestedIntArray.BufferedLoader;
import com.browseengine.bobo.util.BigNestedIntArray.Loader;
import com.browseengine.bobo.util.StringArrayComparator;

public class MultiValueFacetDataCache<T> extends FacetDataCache<T> {
  private static final long serialVersionUID = 1L;
  private static Logger logger = Logger.getLogger(MultiValueFacetDataCache.class);

  public final BigNestedIntArray _nestedArray;
  protected int _maxItems = BigNestedIntArray.MAX_ITEMS;
  protected boolean _overflow = false;

  public MultiValueFacetDataCache() {
    super();
    _nestedArray = new BigNestedIntArray();
  }

  public MultiValueFacetDataCache<T> setMaxItems(int maxItems) {
    _maxItems = Math.min(maxItems, BigNestedIntArray.MAX_ITEMS);
    _nestedArray.setMaxItems(_maxItems);
    return this;
  }

  @Override
  public int getNumItems(int docid) {
    return _nestedArray.getNumItems(docid);
  }

  @Override
  public void load(String fieldName, AtomicReader reader, TermListFactory<T> listFactory)
      throws IOException {
    this.load(fieldName, reader, listFactory, new WorkArea());
  }

  /**
   * loads multi-value facet data. This method uses a workarea to prepare loading.
   * @param fieldName
   * @param reader
   * @param listFactory
   * @param workArea
   * @throws IOException
   */
  public void load(String fieldName, AtomicReader reader, TermListFactory<T> listFactory,
      WorkArea workArea) throws IOException {
    String field = fieldName.intern();
    int maxdoc = reader.maxDoc();
    BufferedLoader loader = getBufferedLoader(maxdoc, workArea);

    @SuppressWarnings("unchecked")
    TermValueList<T> list = (listFactory == null ? (TermValueList<T>) new TermStringList()
        : listFactory.createTermList());
    IntArrayList minIDList = new IntArrayList();
    IntArrayList maxIDList = new IntArrayList();
    IntArrayList freqList = new IntArrayList();
    OpenBitSet bitset = new OpenBitSet(maxdoc + 1);
    int negativeValueCount = getNegativeValueCount(reader, field);
    int t = 0; // current term number
    list.add(null);
    minIDList.add(-1);
    maxIDList.add(-1);
    freqList.add(0);
    t++;

    _overflow = false;

    Terms terms = reader.terms(field);
    if (terms != null) {
      TermsEnum termsEnum = terms.iterator(null);
      BytesRef text;
      while ((text = termsEnum.next()) != null) {
        String strText = text.utf8ToString();
        list.add(strText);

        Term term = new Term(field, strText);
        DocsEnum docsEnum = reader.termDocsEnum(term);
        // freqList.add(tenum.docFreq()); // removed because the df doesn't take into account
        // the num of deletedDocs
        int df = 0;
        int minID = -1;
        int maxID = -1;
        int docID = -1;
        int valId = (t - 1 < negativeValueCount) ? (negativeValueCount - t + 1) : t;
        while ((docID = docsEnum.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
          df++;
          if (!loader.add(docID, valId)) logOverflow(fieldName);
          minID = docID;
          bitset.fastSet(docID);
          while (docsEnum.nextDoc() != DocsEnum.NO_MORE_DOCS) {
            docID = docsEnum.docID();
            df++;
            if (!loader.add(docID, valId)) logOverflow(fieldName);
            bitset.fastSet(docID);
          }
          maxID = docID;
        }
        freqList.add(df);
        minIDList.add(minID);
        maxIDList.add(maxID);
        t++;
      }
    }

    list.seal();

    try {
      _nestedArray.load(maxdoc + 1, loader);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("failed to load due to " + e.toString(), e);
    }

    this.valArray = list;
    this.freqs = freqList.toIntArray();
    this.minIDs = minIDList.toIntArray();
    this.maxIDs = maxIDList.toIntArray();

    int doc = 0;
    while (doc <= maxdoc && !_nestedArray.contains(doc, 0, true)) {
      ++doc;
    }
    if (doc <= maxdoc) {
      this.minIDs[0] = doc;
      doc = maxdoc;
      while (doc > 0 && !_nestedArray.contains(doc, 0, true)) {
        --doc;
      }
      if (doc > 0) {
        this.maxIDs[0] = doc;
      }
    }
    this.freqs[0] = maxdoc + 1 - (int) bitset.cardinality();
  }

  /**
   * loads multi-value facet data. This method uses the count payload to allocate storage before loading data.
   * @param fieldName
   * @param sizeTerm
   * @param reader
   * @param listFactory
   * @throws IOException
   */
  public void load(String fieldName, AtomicReader reader, TermListFactory<T> listFactory,
      Term sizeTerm) throws IOException {
    String field = fieldName.intern();
    int maxdoc = reader.maxDoc();
    Loader loader = new AllocOnlyLoader(_maxItems, sizeTerm, reader);
    int negativeValueCount = getNegativeValueCount(reader, field);
    try {
      _nestedArray.load(maxdoc + 1, loader);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("failed to load due to " + e.toString(), e);
    }

    @SuppressWarnings("unchecked")
    TermValueList<T> list = (listFactory == null ? (TermValueList<T>) new TermStringList()
        : listFactory.createTermList());
    IntArrayList minIDList = new IntArrayList();
    IntArrayList maxIDList = new IntArrayList();
    IntArrayList freqList = new IntArrayList();
    OpenBitSet bitset = new OpenBitSet(maxdoc + 1);

    int t = 0; // current term number
    list.add(null);
    minIDList.add(-1);
    maxIDList.add(-1);
    freqList.add(0);
    t++;

    _overflow = false;

    Terms terms = reader.terms(field);
    if (terms != null) {
      TermsEnum termsEnum = terms.iterator(null);
      BytesRef text;
      while ((text = termsEnum.next()) != null) {
        String strText = text.utf8ToString();
        list.add(strText);

        Term term = new Term(field, strText);
        DocsEnum docsEnum = reader.termDocsEnum(term);

        int df = 0;
        int minID = -1;
        int maxID = -1;
        int docID = -1;
        while ((docID = docsEnum.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
          df++;
          if (!_nestedArray.addData(docID, t)) logOverflow(fieldName);
          minID = docID;
          bitset.fastSet(docID);
          int valId = (t - 1 < negativeValueCount) ? (negativeValueCount - t + 1) : t;
          while (docsEnum.nextDoc() != DocsEnum.NO_MORE_DOCS) {
            docID = docsEnum.docID();
            df++;
            if (!_nestedArray.addData(docID, valId)) logOverflow(fieldName);
            bitset.fastSet(docID);
          }
          maxID = docID;
        }
        freqList.add(df);
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
    while (doc <= maxdoc && !_nestedArray.contains(doc, 0, true)) {
      ++doc;
    }
    if (doc <= maxdoc) {
      this.minIDs[0] = doc;
      doc = maxdoc;
      while (doc > 0 && !_nestedArray.contains(doc, 0, true)) {
        --doc;
      }
      if (doc > 0) {
        this.maxIDs[0] = doc;
      }
    }
    this.freqs[0] = maxdoc + 1 - (int) bitset.cardinality();
  }

  protected void logOverflow(String fieldName) {
    if (!_overflow) {
      logger
          .error("Maximum value per document: " + _maxItems + " exceeded, fieldName=" + fieldName);
      _overflow = true;
    }
  }

  protected BufferedLoader getBufferedLoader(int maxdoc, WorkArea workArea) {
    if (workArea == null) {
      return new BufferedLoader(maxdoc, _maxItems, new BigIntBuffer());
    } else {
      BigIntBuffer buffer = workArea.get(BigIntBuffer.class);
      if (buffer == null) {
        buffer = new BigIntBuffer();
        workArea.put(buffer);
      } else {
        buffer.reset();
      }

      BufferedLoader loader = workArea.get(BufferedLoader.class);
      if (loader == null || loader.capacity() < maxdoc) {
        loader = new BufferedLoader(maxdoc, _maxItems, buffer);
        workArea.put(loader);
      } else {
        loader.reset(maxdoc, _maxItems, buffer);
      }
      return loader;
    }
  }

  /**
   * A loader that allocate data storage without loading data to BigNestedIntArray.
   * Note that this loader supports only non-negative integer data.
   */
  public final static class AllocOnlyLoader extends Loader {
    private final AtomicReader _reader;
    private final Term _sizeTerm;
    private final int _maxItems;

    public AllocOnlyLoader(int maxItems, Term sizeTerm, AtomicReader reader) throws IOException {
      _maxItems = Math.min(maxItems, BigNestedIntArray.MAX_ITEMS);
      _sizeTerm = sizeTerm;
      _reader = reader;
    }

    @Override
    public void load() throws Exception {
      DocsAndPositionsEnum docPosEnum = _reader.termPositionsEnum(_sizeTerm);
      if (docPosEnum == null) {
        return;
      }
      int docID = -1;
      while ((docID = docPosEnum.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
        if (docPosEnum.freq() > 0) {
          docPosEnum.nextPosition();
          int len = bytesToInt(docPosEnum.getPayload().bytes);
          allocate(docID, Math.min(len, _maxItems), true);
        }
      }
    }

    private static int bytesToInt(byte[] bytes) {
      return ((bytes[3] & 0xFF) << 24) | ((bytes[2] & 0xFF) << 16) | ((bytes[1] & 0xFF) << 8)
          | (bytes[0] & 0xFF);
    }
  }

  public final static class MultiFacetDocComparatorSource extends DocComparatorSource {
    private final MultiDataCacheBuilder cacheBuilder;

    public MultiFacetDocComparatorSource(MultiDataCacheBuilder multiDataCacheBuilder) {
      cacheBuilder = multiDataCacheBuilder;
    }

    @Override
    public DocComparator getComparator(final AtomicReader reader, int docbase) throws IOException {
      if (!(reader instanceof BoboSegmentReader)) throw new IllegalStateException(
          "reader must be instance of " + BoboSegmentReader.class);
      BoboSegmentReader boboReader = (BoboSegmentReader) reader;
      final MultiValueFacetDataCache<?> dataCache = cacheBuilder.build(boboReader);
      return new DocComparator() {

        @Override
        public int compare(ScoreDoc doc1, ScoreDoc doc2) {
          return dataCache._nestedArray.compare(doc1.doc, doc2.doc);
        }

        @Override
        public Comparable<?> value(ScoreDoc doc) {
          String[] vals = dataCache._nestedArray.getTranslatedData(doc.doc, dataCache.valArray);
          return new StringArrayComparator(vals);
        }

      };
    }
  }
}
