/**
 * 
 */
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

/**
 * @author ymatsuda
 *
 */
public class MultiValueFacetDataCache<T> extends FacetDataCache<T>
{
  private static final long serialVersionUID = 1L;
  private static Logger logger = Logger.getLogger(MultiValueFacetDataCache.class);
 
  public final BigNestedIntArray _nestedArray;
  protected int _maxItems = BigNestedIntArray.MAX_ITEMS;
  protected boolean _overflow = false;
  
  public MultiValueFacetDataCache()
  {
    super();
    _nestedArray = new BigNestedIntArray();
  }
  
  public MultiValueFacetDataCache<T> setMaxItems(int maxItems)
  {
    _maxItems = Math.min(maxItems, BigNestedIntArray.MAX_ITEMS);
    _nestedArray.setMaxItems(_maxItems);
    return this;
  }
  
  @Override
  public int getNumItems(int docid){
	  return _nestedArray.getNumItems(docid);
  }
  
  @Override
  public void load(String fieldName, IndexReader reader, TermListFactory<T> listFactory) throws IOException
  {
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
  public void load(String fieldName, IndexReader reader, TermListFactory<T> listFactory, WorkArea workArea) throws IOException
  {
    long t0 = System.currentTimeMillis();
    int maxdoc = reader.maxDoc();
    BufferedLoader loader = getBufferedLoader(maxdoc, workArea);

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
            list.add(val);

            tdoc.seek(tenum);
            //freqList.add(tenum.docFreq()); // removed because the df doesn't take into account the num of deletedDocs
            int df = 0;
            int minID = -1;
            int maxID = -1;
            int valId = (t - 1 < negativeValueCount) ? (negativeValueCount - t + 1) : t;
            if(tdoc.next())
            {
              df++;
              int docid = tdoc.doc();
              if(!loader.add(docid, valId)) logOverflow(fieldName);
              minID = docid;
              bitset.fastSet(docid);
              while(tdoc.next())
              {
                df++;
                docid = tdoc.doc();
               
                if(!loader.add(docid, valId)) logOverflow(fieldName);
                bitset.fastSet(docid);
              }
              maxID = docid;
            }
            freqList.add(df);
            minIDList.add(minID);
            maxIDList.add(maxID);
          }

          t++;
        }
        while (tenum.next());
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

  /**
   * loads multi-value facet data. This method uses the count payload to allocate storage before loading data.
   * @param fieldName
   * @param sizeTerm
   * @param reader
   * @param listFactory
   * @throws IOException
   */
  public void load(String fieldName, IndexReader reader, TermListFactory<T> listFactory, Term sizeTerm) throws IOException
  {
    int maxdoc = reader.maxDoc();
    Loader loader = new AllocOnlyLoader(_maxItems, sizeTerm, reader);
    int negativeValueCount = getNegativeValueCount(reader, fieldName.intern()); 
    try
    {
      _nestedArray.load(maxdoc + 1, loader);
    }
    catch (IOException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw new RuntimeException("failed to load due to " + e.toString(), e);
    }
    
    TermEnum tenum = null;
    TermDocs tdoc = null;
    TermValueList<T> list = (listFactory == null ? (TermValueList<T>)new TermStringList() : listFactory.createTermList());
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
    try
    {
      tdoc = reader.termDocs();
      tenum = reader.terms(new Term(fieldName, ""));
      if (tenum != null)
      {
        do
        {
          Term term = tenum.term();
          if(term == null || !fieldName.equals(term.field()))
            break;
          
          String val = term.text();
          
          if (val != null)
          {
            list.add(val);
            
            tdoc.seek(tenum);
            //freqList.add(tenum.docFreq()); // removed because the df doesn't take into account the num of deletedDocs
            int df = 0;
            int minID = -1;
            int maxID = -1;
            if(tdoc.next())
            {
              df++;
              int docid = tdoc.doc();
              if (!_nestedArray.addData(docid, t)) logOverflow(fieldName);
              minID = docid;
              bitset.fastSet(docid);
              int valId = (t - 1 < negativeValueCount) ? (negativeValueCount - t + 1) : t;
              while(tdoc.next())
              {
                df++;
                docid = tdoc.doc();
                if(!_nestedArray.addData(docid, valId)) logOverflow(fieldName);
                bitset.fastSet(docid);
              }
              maxID = docid;
            }
            freqList.add(df);
            minIDList.add(minID);
            maxIDList.add(maxID);
          }
          
          t++;
        }
        while (tenum.next());
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
  
  protected void logOverflow(String fieldName)
  {
    if (!_overflow)
    {
      logger.error("Maximum value per document: " + _maxItems + " exceeded, fieldName=" + fieldName);
      _overflow = true;
    }
  }

  protected BufferedLoader getBufferedLoader(int maxdoc, WorkArea workArea)
  {
    if(workArea == null)
    {
      return new BufferedLoader(maxdoc, _maxItems, new BigIntBuffer());
    }
    else
    {
      BigIntBuffer buffer = workArea.get(BigIntBuffer.class);
      if(buffer == null)
      {
        buffer = new BigIntBuffer();
        workArea.put(buffer);
      }
      else
      {
        buffer.reset();
      }
      
      BufferedLoader loader = workArea.get(BufferedLoader.class);      
      if(loader == null || loader.capacity() < maxdoc)
      {
        loader = new BufferedLoader(maxdoc, _maxItems, buffer);
        workArea.put(loader);
      }
      else
      {
        loader.reset(maxdoc, _maxItems, buffer);
      }
      return loader;
    }
  }
  
  /**
   * A loader that allocate data storage without loading data to BigNestedIntArray.
   * Note that this loader supports only non-negative integer data.
   */
  public final static class AllocOnlyLoader extends Loader
  {
    private IndexReader _reader;
    private Term _sizeTerm;
    private int _maxItems;
    
    public AllocOnlyLoader(int maxItems, Term sizeTerm, IndexReader reader) throws IOException
    {
      _maxItems = Math.min(maxItems, BigNestedIntArray.MAX_ITEMS);
      _sizeTerm = sizeTerm;
      _reader = reader;
    }
    
    @Override
    public void load() throws Exception
    {
      TermPositions tp = null;
      byte[] payloadBuffer = new byte[4];        // four bytes for an int
      try
      {
        tp = _reader.termPositions(_sizeTerm);

        if(tp == null) return;
        
        while(tp.next())
        {
          if(tp.freq() > 0)
          {
            tp.nextPosition();
            tp.getPayload(payloadBuffer, 0);
            int len = bytesToInt(payloadBuffer);
            allocate(tp.doc(), Math.min(len, _maxItems), true);
          }
        }
      }
      finally
      {
        if(tp != null) tp.close();
      }
    }
    
    private static int bytesToInt(byte[] bytes) 
    {
      return ((bytes[3] & 0xFF) << 24) | ((bytes[2] & 0xFF) << 16) | 
              ((bytes[1] & 0xFF) <<  8) |  (bytes[0] & 0xFF);
    }
  }
    
	public final static class MultiFacetDocComparatorSource extends DocComparatorSource{
		private MultiDataCacheBuilder cacheBuilder;
		public MultiFacetDocComparatorSource(MultiDataCacheBuilder multiDataCacheBuilder){
		  cacheBuilder = multiDataCacheBuilder;
		}
		
		@Override
		public DocComparator getComparator(final IndexReader reader, int docbase)
				throws IOException {
			if (!(reader instanceof BoboIndexReader)) throw new IllegalStateException("reader must be instance of "+BoboIndexReader.class);
			BoboIndexReader boboReader = (BoboIndexReader)reader;
			final MultiValueFacetDataCache dataCache = cacheBuilder.build(boboReader);
			return new DocComparator(){
				
				@Override
				public int compare(ScoreDoc doc1, ScoreDoc doc2) {
					return dataCache._nestedArray.compare(doc1.doc, doc2.doc);
				}

				@Override
				public Comparable value(ScoreDoc doc) {
					String[] vals = dataCache._nestedArray.getTranslatedData(doc.doc, dataCache.valArray);
			          return new StringArrayComparator(vals);
				}
				
			};
		}
	}
}
