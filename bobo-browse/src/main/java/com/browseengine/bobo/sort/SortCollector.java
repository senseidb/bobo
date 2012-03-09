package com.browseengine.bobo.sort;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.apache.log4j.Logger;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BoboCustomSortField;
import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BoboSubBrowser;
import com.browseengine.bobo.api.Browsable;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.jmx.JMXUtil;
import com.browseengine.bobo.sort.DocComparatorSource.DocIdDocComparatorSource;
import com.browseengine.bobo.sort.DocComparatorSource.RelevanceDocComparatorSource;
import com.browseengine.bobo.util.MemoryManager;
import com.browseengine.bobo.util.MemoryManagerAdminMBean;

public abstract class SortCollector extends Collector {
	private static final Logger logger = Logger.getLogger(SortCollector.class);
	
  protected static MemoryManager<int[]> intarraymgr = new MemoryManager<int[]>(new MemoryManager.Initializer<int[]>()
  {
    public void init(int[] buf)
    {
      Arrays.fill(buf, 0);
    }

    public int[] newInstance(int size)
    {
      return new int[size];
    }

    public int size(int[] buf)
    {
      assert buf!=null;
      return buf.length;
    }

  });

  protected static MemoryManager<float[]> floatarraymgr = new MemoryManager<float[]>(new MemoryManager.Initializer<float[]>()
  {
    public void init(float[] buf)
    {
      Arrays.fill(buf, 0);
    }

    public float[] newInstance(int size)
    {
      return new float[size];
    }

    public int size(float[] buf)
    {
      assert buf!=null;
      return buf.length;
    }

  });

  static{
	  try{
      // register memory manager mbean
      MBeanServer mbeanServer = java.lang.management.ManagementFactory.getPlatformMBeanServer();
	    ObjectName mbeanName = new ObjectName(JMXUtil.JMX_DOMAIN,"name","SortCollectorImpl-MemoryManager-Int");
	    StandardMBean mbean = new StandardMBean(intarraymgr.getAdminMBean(), MemoryManagerAdminMBean.class);
	    mbeanServer.registerMBean(mbean, mbeanName);

	    mbeanName = new ObjectName(JMXUtil.JMX_DOMAIN,"name","SortCollectorImpl-MemoryManager-Float");
	    mbean = new StandardMBean(floatarraymgr.getAdminMBean(), MemoryManagerAdminMBean.class);
	    mbeanServer.registerMBean(mbean, mbeanName);
	  }
	  catch(Exception e){
	    logger.error(e.getMessage(),e);
	  }
  }

  public static class CollectorContext {
    public BoboIndexReader reader;
    public int base;
    public DocComparator comparator;
    public int length;

    public CollectorContext(BoboIndexReader reader, int base, DocComparator comparator) {
      this.reader = reader;
      this.base = base;
      this.comparator = comparator;
    }
  }

  public FacetHandler<?> groupBy = null; // Point to the first element of groupByMulti to avoid array lookups.
  public FacetHandler<?>[] groupByMulti = null;

  public LinkedList<CollectorContext> contextList;
  public LinkedList<int[]> docidarraylist;
  public LinkedList<float[]> scorearraylist;

  public static int BLOCK_SIZE = 4096;

	protected Collector _collector = null;
	protected final SortField[] _sortFields;
	protected final boolean _fetchStoredFields;
  protected boolean _closed = false;
	
	protected SortCollector(SortField[] sortFields,boolean fetchStoredFields){
		_sortFields = sortFields;
		_fetchStoredFields = fetchStoredFields;
	}
	
	abstract public BrowseHit[] topDocs() throws IOException;

	abstract public int getTotalHits();
	abstract public int getTotalGroups();
	abstract public FacetAccessible[] getGroupAccessibles();
	
	private static DocComparatorSource getNonFacetComparatorSource(SortField sf){
		String fieldname = sf.getField();
		Locale locale = sf.getLocale();
		if (locale != null) {
	      return new DocComparatorSource.StringLocaleComparatorSource(fieldname, locale);
		}
		
		int type = sf.getType();

	    switch (type) {
	    case SortField.INT:
	      return new DocComparatorSource.IntDocComparatorSource(fieldname);
	
	    case SortField.FLOAT:
	      return new DocComparatorSource.FloatDocComparatorSource(fieldname);
	
	    case SortField.LONG:
	      return new DocComparatorSource.LongDocComparatorSource(fieldname);
	
	    case SortField.DOUBLE:
	      return new DocComparatorSource.LongDocComparatorSource(fieldname);
	
	    case SortField.BYTE:
	      return new DocComparatorSource.ByteDocComparatorSource(fieldname);
	
	    case SortField.SHORT:
	      return new DocComparatorSource.ShortDocComparatorSource(fieldname);
	
	    case SortField.CUSTOM:
		  throw new IllegalArgumentException("lucene custom sort no longer supported: "+fieldname); 
	
	    case SortField.STRING:
	      return new DocComparatorSource.StringOrdComparatorSource(fieldname);
	
	    case SortField.STRING_VAL:
	      return new DocComparatorSource.StringValComparatorSource(fieldname);
	        
	    default:
	      throw new IllegalStateException("Illegal sort type: " + type+", for field: "+fieldname);
	    }
	}
	
	private static DocComparatorSource getComparatorSource(Browsable browser,SortField sf){
		DocComparatorSource compSource = null;
		if (SortField.FIELD_DOC.equals(sf)){
			compSource = new DocIdDocComparatorSource();
		}
		else if (SortField.FIELD_SCORE.equals(sf) || sf.getType() == SortField.SCORE){
			// we want to do reverse sorting regardless for relevance
			compSource = new ReverseDocComparatorSource(new RelevanceDocComparatorSource());
		}
		else if (sf instanceof BoboCustomSortField){
			BoboCustomSortField custField = (BoboCustomSortField)sf;
			DocComparatorSource src = custField.getCustomComparatorSource();
			assert src!=null;
			compSource = src;
		}
		else{
			Set<String> facetNames = browser.getFacetNames();
			String sortName = sf.getField();
			if (facetNames.contains(sortName)){
				FacetHandler<?> handler = browser.getFacetHandler(sortName);
				assert handler!=null;
				compSource = handler.getDocComparatorSource();
			}
			else{		// default lucene field
				logger.info("doing default lucene sort for: "+sf);
				compSource = getNonFacetComparatorSource(sf);
			}
		}
		boolean reverse = sf.getReverse();
		if (reverse){
			compSource = new ReverseDocComparatorSource(compSource);
		}
		compSource.setReverse(reverse);
		return compSource;
	}
	
	private static SortField convert(Browsable browser,SortField sort){
		String field =sort.getField();
		FacetHandler<?> facetHandler = browser.getFacetHandler(field);
		if (facetHandler!=null){
			browser.getFacetHandler(field);
			BoboCustomSortField sortField = new BoboCustomSortField(field, sort.getReverse(), facetHandler.getDocComparatorSource());
			return sortField;
		}
		else{
			return sort;
		}
	}
	public static SortCollector buildSortCollector(Browsable browser,Query q,SortField[] sort,int offset,int count,boolean forceScoring,boolean fetchStoredFields, Set<String> termVectorsToFetch,String[] groupBy, int maxPerGroup, boolean collectDocIdCache){
		boolean doScoring=forceScoring;
		if (sort == null || sort.length==0){	
			if (q!=null && !(q instanceof MatchAllDocsQuery)){
			  sort = new SortField[]{SortField.FIELD_SCORE};
			}
		}

		if (sort==null || sort.length==0){
			sort = new SortField[]{SortField.FIELD_DOC};
		}
		
		Set<String> facetNames = browser.getFacetNames();
		for (SortField sf : sort){
			if (sf.getType() == SortField.SCORE) {
				doScoring= true;
				break;
			}	
		}

		DocComparatorSource compSource;
		if (sort.length==1){
			SortField sf = convert(browser,sort[0]);
			compSource = getComparatorSource(browser,sf);
		}
		else{
			DocComparatorSource[] compSources = new DocComparatorSource[sort.length];
			for (int i = 0; i<sort.length;++i){
				compSources[i]=getComparatorSource(browser,convert(browser,sort[i]));
			}
			compSource = new MultiDocIdComparatorSource(compSources);
		}
		return new SortCollectorImpl(compSource, sort, browser, offset, count, doScoring, fetchStoredFields, termVectorsToFetch,groupBy, maxPerGroup, collectDocIdCache);
	}
	
	public SortCollector setCollector(Collector collector){
		_collector = collector;
    return this;
	}
	
	public Collector getCollector(){
		return _collector; 
	}

  public void close() {
    if (!_closed)
    {
      _closed = true;
      if (docidarraylist != null) {
        while(!docidarraylist.isEmpty())
        {
          intarraymgr.release(docidarraylist.poll());
        }
      }
      if (scorearraylist != null) {
        while(!scorearraylist.isEmpty())
        {
          floatarraymgr.release(scorearraylist.poll());
        }
      }
    }
  }
}
