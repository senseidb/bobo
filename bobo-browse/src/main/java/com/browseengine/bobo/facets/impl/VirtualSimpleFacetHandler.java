package com.browseengine.bobo.facets.impl;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.index.TermDocs;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.facets.data.FacetDataCache;
import com.browseengine.bobo.facets.data.TermListFactory;
import com.browseengine.bobo.facets.data.TermFixedLengthLongArrayListFactory;
import com.browseengine.bobo.facets.data.TermStringList;
import com.browseengine.bobo.facets.data.TermValueList;
import com.browseengine.bobo.facets.data.FacetDataFetcher;
import com.browseengine.bobo.util.BigIntArray;
import com.browseengine.bobo.util.BigSegmentedArray;

public class VirtualSimpleFacetHandler extends SimpleFacetHandler
{
  protected FacetDataFetcher _facetDataFetcher;

  public VirtualSimpleFacetHandler(String name,
                                   String indexFieldName,
                                   TermListFactory termListFactory,
                                   FacetDataFetcher facetDataFetcher,
                                   Set<String> dependsOn)
  {
    super(name, null, termListFactory, dependsOn);
    _facetDataFetcher = facetDataFetcher;
  }

  public VirtualSimpleFacetHandler(String name,
                                   TermListFactory termListFactory,
                                   FacetDataFetcher facetDataFetcher,
                                   Set<String> dependsOn)
  {
    this(name, null, termListFactory, facetDataFetcher, dependsOn);
  }

  @Override
  public FacetDataCache load(BoboIndexReader reader) throws IOException
  {
    int doc = -1;
    TreeMap<Object, LinkedList<Integer>> dataMap = null;
    LinkedList<Integer> docList = null;

    int nullMinId = -1;
    int nullMaxId = -1;
    int nullFreq = 0;

    TermDocs termDocs = reader.termDocs(null);
    try
    {
      while(termDocs.next())
      {
        doc = termDocs.doc();
        Object val = _facetDataFetcher.fetch(reader, doc);
        if (val == null)
        {
          if (nullMinId < 0)
            nullMinId = doc;
          nullMaxId = doc;
          ++ nullFreq;
          continue;
        }
        if (dataMap == null)
        {
          // Initialize.
          if (val instanceof long[])
          {
            if(_termListFactory == null)
              _termListFactory = new TermFixedLengthLongArrayListFactory(
                ((long[])val).length);

            dataMap = new TreeMap<Object, LinkedList<Integer>>(new Comparator<Object>()
            {
              public int compare(Object big, Object small)
              {
                if (((long[])big).length != ((long[])small).length)
                {
                  throw new RuntimeException(""+Arrays.asList(((long[])big))+" and "+
                    Arrays.asList(((long[])small))+" have different length.");
                }

                long r = 0;
                for (int i=0; i<((long[])big).length; ++i)
                {
                  r = ((long[])big)[i] - ((long[])small)[i];
                  if (r != 0)
                    break;
                }

                if (r > 0)
                  return 1;
                else if (r < 0)
                  return -1;

                return 0;
              }
            });
          }
          else if (val instanceof Comparable)
          {
            dataMap = new TreeMap<Object, LinkedList<Integer>>();
          }
          else
          {
            dataMap = new TreeMap<Object, LinkedList<Integer>>(new Comparator<Object>()
            {
              public int compare(Object big, Object small)
              {
                return String.valueOf(big).compareTo(String.valueOf(small));
              }
            });
          }
        }

        docList = dataMap.get(val);
        if (docList == null)
        {
          docList = new LinkedList<Integer>();
          dataMap.put(val, docList);
        }
        docList.add(doc);
      }
    }
    finally
    {
      termDocs.close();
    }
    _facetDataFetcher.cleanup(reader);

    int maxDoc = reader.maxDoc();
    int size = dataMap == null ? 1:(dataMap.size() + 1);

    BigSegmentedArray order = new BigIntArray(maxDoc);
    TermValueList list = _termListFactory == null ?
      new TermStringList(size) :
      _termListFactory.createTermList(size);

    int[] freqs = new int[size];
    int[] minIDs = new int[size];
    int[] maxIDs = new int[size];

    list.add(null);
    freqs[0] = nullFreq;
    minIDs[0] = nullMinId;
    maxIDs[0] = nullMaxId;

    if (dataMap != null)
    {
      int i = 1;
      Integer docId;
      for (Map.Entry<Object, LinkedList<Integer>> entry : dataMap.entrySet())
      {
        list.add(list.format(entry.getKey()));
        docList = entry.getValue();
        freqs[i] = docList.size();
        minIDs[i] = docList.get(0);
        while((docId = docList.poll()) != null)
        {
          doc = docId;
          order.add(doc, i);
        }
        maxIDs[i] = doc;
        ++i;
      }
    }
    list.seal();

    FacetDataCache dataCache = new FacetDataCache(order, list, freqs, minIDs,
      maxIDs, TermCountSize.large);
    return dataCache;
  }
}
