package com.browseengine.bobo.facets.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.util.OpenBitSet;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BoboIndexReader.WorkArea;
import com.browseengine.bobo.facets.range.MultiDataCacheBuilder;
import com.browseengine.bobo.sort.DocComparator;
import com.browseengine.bobo.sort.DocComparatorSource;
import com.browseengine.bobo.util.BigIntBuffer;
import com.browseengine.bobo.util.BigNestedIntArray;
import com.browseengine.bobo.util.BigNestedIntArray.BufferedLoader;
import com.browseengine.bobo.util.BigNestedIntArray.Loader;
import com.browseengine.bobo.util.StringArrayComparator;

public class MultiValueWithWeightFacetDataCache<T> extends MultiValueFacetDataCache<T>
{
  private static final long serialVersionUID = 1L;

  public final BigNestedIntArray _weightArray;

  public MultiValueWithWeightFacetDataCache()
  {
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
  public void load(String fieldName,
                   IndexReader reader,
                   TermListFactory<T> listFactory,
                   WorkArea workArea) throws IOException
  {
    long t0 = System.currentTimeMillis();
    int maxdoc = reader.maxDoc();
    BufferedLoader loader = getBufferedLoader(maxdoc, workArea);
    BufferedLoader weightLoader = getBufferedLoader(maxdoc, null);

    TermEnum tenum = null;
    TermDocs tdoc = null;
    TermValueList<T> list = (listFactory == null ? (TermValueList<T>)new TermStringList() : listFactory.createTermList());
    IntArrayList minIDList = new IntArrayList();
    IntArrayList maxIDList = new IntArrayList();
    IntArrayList freqList = new IntArrayList();
    OpenBitSet bitset = new OpenBitSet(maxdoc + 1);
    int negativeValueCount = getNegativeValueCount(reader, fieldName.intern()); 
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
    int valId = 0;

    try
    {
      tdoc = reader.termDocs();
      tenum = reader.terms(new Term(fieldName, ""));
      if (tenum != null)
      {
        do
        {
          Term term = tenum.term();
          if (term == null || !fieldName.equals(term.field()))
            break;

          String val = term.text();

          if (val != null)
          {
            int weight = 0;
            String[] split = val.split(":");
            if (split.length > 1)
            {
              val = split[0];
              weight = Integer.parseInt(split[1]);
            }
            if (pre == null || !val.equals(pre))
            {
              if (pre != null)
              {
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

            tdoc.seek(tenum);
            if(tdoc.next())
            {
              df++;
              int docid = tdoc.doc();

              if(!loader.add(docid, valId)) logOverflow(fieldName);
              else weightLoader.add(docid, weight);

              if (docid < minID) minID = docid;
              bitset.fastSet(docid);
              while(tdoc.next())
              {
                df++;
                docid = tdoc.doc();
               
                if(!loader.add(docid, valId)) logOverflow(fieldName);
                else weightLoader.add(docid, weight);

                bitset.fastSet(docid);
              }
              if (docid > maxID) maxID = docid;
            }
            pre = val;
          }

        }
        while (tenum.next());
        if (pre != null)
        {
          freqList.add(df);
          minIDList.add(minID);
          maxIDList.add(maxID);
        }
      }
    }
    finally
    {
      try
      {
        if (tdoc != null)
        {
          tdoc.close();
        }
      }
      finally
      {
        if (tenum != null)
        {
          tenum.close();
        }
      }
    }

    list.seal();

    try
    {
      _nestedArray.load(maxdoc + 1, loader);
      _weightArray.load(maxdoc + 1, weightLoader);
    }
    catch (IOException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw new RuntimeException("failed to load due to " + e.toString(), e);
    }
    
    this.valArray = list;
    this.freqs = freqList.toIntArray();
    this.minIDs = minIDList.toIntArray();
    this.maxIDs = maxIDList.toIntArray();

    int doc = 0;
    while (doc <= maxdoc && !_nestedArray.contains(doc, 0, true))
    {
      ++doc;
    }
    if (doc <= maxdoc)
    {
      this.minIDs[0] = doc;
      doc = maxdoc;
      while (doc > 0 && !_nestedArray.contains(doc, 0, true))
      {
        --doc;
      }
      if (doc > 0)
      {
        this.maxIDs[0] = doc;
      }
    }
    this.freqs[0] = maxdoc + 1 - (int) bitset.cardinality();   
  }
}
