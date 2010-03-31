package com.browseengine.bobo.sort;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BoboCustomSortField;
import com.browseengine.bobo.api.BoboSubBrowser;
import com.browseengine.bobo.api.Browsable;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.sort.DocComparatorSource.DocIdDocComparatorSource;
import com.browseengine.bobo.sort.DocComparatorSource.RelevanceDocComparatorSource;

public abstract class SortCollector extends Collector {
	protected Collector _collector = null;
	protected final SortField[] _sortFields;
	protected final boolean _fetchStoredFields;
	
	protected SortCollector(SortField[] sortFields,boolean fetchStoredFields){
		_sortFields = sortFields;
		_fetchStoredFields = fetchStoredFields;
	}
	
	abstract public BrowseHit[] topDocs() throws IOException;

	abstract public int getTotalHits();
	
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
		  throw new IllegalArgumentException("lucene custom sort no longer supported"); 
	
	    case SortField.STRING:
	      return new DocComparatorSource.StringOrdComparatorSource(fieldname);
	
	    case SortField.STRING_VAL:
	      return new DocComparatorSource.StringValComparatorSource(fieldname);
	        
	    default:
	      throw new IllegalStateException("Illegal sort type: " + type);
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
	
	private static SortField convert(BoboSubBrowser browser,SortField sort){
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
	public static SortCollector buildSortCollector(BoboSubBrowser browser,Query q,SortField[] sort,int offset,int count,boolean forceScoring,boolean fetchStoredFields){
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
		return new SortCollectorImpl(compSource, sort, browser, offset, count, doScoring, fetchStoredFields);
	}
	
	public void setCollector(Collector collector){
		_collector = collector;
	}
	
	public Collector getCollector(){
		return _collector; 
	}
}
