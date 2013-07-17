package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.IOException;

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;

import com.browseengine.bobo.api.BoboSegmentReader.WorkArea;
import com.browseengine.bobo.util.BigNestedIntArray;
import com.browseengine.bobo.util.BigNestedIntArray.BufferedLoader;

public class MultiValueWithWeightFacetDataCache<T> extends MultiValueFacetDataCache<T> {
  private static final long serialVersionUID = 1L;

  public final BigNestedIntArray _weightArray;

  public MultiValueWithWeightFacetDataCache() {
    super();
    _weightArray = new BigNestedIntArray();
  }

  /**
   * loads multi-value facet data. This method uses a workarea to prepare loading.
   * @param fieldName
   * @param reader
   * @param listFactory
   * @param workArea
   * @throws IOException
   */
  @Override
  public void load(String fieldName, AtomicReader reader, TermListFactory<T> listFactory,
      WorkArea workArea) throws IOException {
    String field = fieldName.intern();
    int maxdoc = reader.maxDoc();
    BufferedLoader loader = getBufferedLoader(maxdoc, workArea);
    BufferedLoader weightLoader = getBufferedLoader(maxdoc, null);

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

    String pre = null;

    int df = 0;
    int minID = -1;
    int maxID = -1;
    int docID = -1;
    int valId = 0;

    Terms terms = reader.terms(field);
    if (terms != null) {
      TermsEnum termsEnum = terms.iterator(null);
      BytesRef text;
      while ((text = termsEnum.next()) != null) {
        String strText = text.utf8ToString();
        String val = null;
        int weight = 0;
        String[] split = strText.split("\u0000");
        if (split.length > 1) {
          val = split[0];
          weight = Integer.parseInt(split[split.length - 1]);
        } else {
          continue;
        }

        if (pre == null || !val.equals(pre)) {
          if (pre != null) {
            freqList.add(df);
            minIDList.add(minID);
            maxIDList.add(maxID);
          }
          list.add(val);
          df = 0;
          minID = -1;
          maxID = -1;
          valId = (t - 1 < negativeValueCount) ? (negativeValueCount - t + 1) : t;
          t++;
        }

        Term term = new Term(field, strText);
        DocsEnum docsEnum = reader.termDocsEnum(term);
        if (docsEnum != null) {
          while ((docID = docsEnum.nextDoc()) != DocsEnum.NO_MORE_DOCS) {
            df++;

            if (!loader.add(docID, valId)) {
              logOverflow(fieldName);
            } else {
              weightLoader.add(docID, weight);
            }

            if (docID < minID) minID = docID;
            bitset.fastSet(docID);
            while (docsEnum.nextDoc() != DocsEnum.NO_MORE_DOCS) {
              docID = docsEnum.docID();
              df++;
              if (!loader.add(docID, valId)) {
                logOverflow(fieldName);
              } else {
                weightLoader.add(docID, weight);
              }
              bitset.fastSet(docID);
            }
            if (docID > maxID) maxID = docID;
          }
        }
        pre = val;
      }
      if (pre != null) {
        freqList.add(df);
        minIDList.add(minID);
        maxIDList.add(maxID);
      }
    }

    list.seal();

    try {
      _nestedArray.load(maxdoc + 1, loader);
      _weightArray.load(maxdoc + 1, weightLoader);
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("failed to load due to " + e.toString(), e);
    }

    this.valArray = list;
    this.freqs = freqList.toIntArray();
    this.minIDs = minIDList.toIntArray();
    this.maxIDs = maxIDList.toIntArray();
  }
}
