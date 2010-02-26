package com.browseengine.bobo.svc.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.ComparatorFactory;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.browseengine.bobo.facets.impl.FacetHitcountComparatorFactory;
import com.browseengine.bobo.facets.impl.FacetValueComparatorFactory;
import com.browseengine.bobo.gwt.svc.BoboFacetSpec;
import com.browseengine.bobo.gwt.svc.BoboHit;
import com.browseengine.bobo.gwt.svc.BoboRequest;
import com.browseengine.bobo.gwt.svc.BoboResult;
import com.browseengine.bobo.gwt.svc.BoboSearchService;
import com.browseengine.bobo.gwt.svc.BoboSelection;
import com.browseengine.bobo.gwt.svc.BoboSortSpec;
import com.browseengine.bobo.gwt.widgets.FacetValue;
import com.browseengine.bobo.service.BrowseService;

public class BoboSearchServiceImpl implements BoboSearchService {
	private static final Logger log = Logger.getLogger(BoboSearchServiceImpl.class);
	private BrowseService _svc;
	private QueryParser _qparser;
	
	public BoboSearchServiceImpl(BrowseService svc,QueryParser qparser){
		_svc = svc;
		_qparser = qparser;
	}
	
	private static FacetSpec convert(BoboFacetSpec spec){
		FacetSpec fspec = new FacetSpec();
		
		fspec.setMaxCount(spec.getMax());
		fspec.setMinHitCount(spec.getMinCount());
		fspec.setExpandSelection(spec.isExpandSelection());
		fspec.setOrderBy(spec.isOrderByHits() ? FacetSortSpec.OrderHitsDesc : FacetSortSpec.OrderValueAsc);
		return fspec;
	}
	
	private static Comparator<FacetValue> getComparator(FacetSpec fspec){
		ComparatorFactory compFactory = null;
		
		if (fspec == null || FacetSortSpec.OrderHitsDesc == fspec.getOrderBy()){
			compFactory = new FacetHitcountComparatorFactory();
		}
		else if (FacetSortSpec.OrderValueAsc == fspec.getOrderBy()){
			compFactory = new FacetValueComparatorFactory();
		}
		else{
			compFactory = fspec.getCustomComparatorFactory();
		}

		final Comparator<BrowseFacet> subComparator = compFactory.newComparator();
		return new Comparator<FacetValue>(){
			public int compare(FacetValue f1, FacetValue f2) {
				BrowseFacet bf1 = new BrowseFacet(f1.getValue(),f1.getCount());
				BrowseFacet bf2 = new BrowseFacet(f2.getValue(),f2.getCount());
				return subComparator.compare(bf1, bf2);
			}		
		};
	}
	
	private BrowseRequest convert(BoboRequest req) throws Exception{
		BrowseRequest breq = new BrowseRequest();
		String q = req.getQuery();
		if (q!=null){
			breq.setQuery(_qparser.parse(q));
		}
		breq.setOffset(req.getOffset());
		breq.setCount(req.getCount());
		breq.setFetchStoredFields(false);
		List<BoboSortSpec> sortList = req.getSortSpecs();
		int size;
		if (sortList!=null && (size = sortList.size())>0){
		  SortField[] sorts = new SortField[size];
		  int i=0;
		  for (BoboSortSpec sortSpec : sortList){
			  sorts[i++] = new SortField(sortSpec.getField(),sortSpec.isReverse());
		  }
		  breq.setSort(sorts);
		}
		
		Map<String,BoboSelection> selections = req.getSelections();
		if (selections!=null){
			Set<Entry<String,BoboSelection>> entrySet = selections.entrySet();
			for (Entry<String,BoboSelection> entry : entrySet){
				String fldName = entry.getKey();
				BoboSelection sel = entry.getValue();

				BrowseSelection bsel = new BrowseSelection(fldName);
				
				List<String> selVals = sel.getValues();

				if (selVals!=null){
				  for (String val : selVals){
					bsel.addValue(val);  
				  }
				}
				
				List<String> notVals = sel.getNotValues();
				if (notVals!=null){
				  for (String val : notVals){
					bsel.addNotValue(val);  
				  }
				}
				
				Map<String,String> props = sel.getSelectionProperties();
				if (props!=null && props.size()>0){
					bsel.setSelectionProperties(props);
				}
				breq.addSelection(bsel);
			}
		}
		
		Map<String,BoboFacetSpec> facetMap = req.getFacetSpecMap();
		if (facetMap!=null && facetMap.size()>0){
			Set<Entry<String,BoboFacetSpec>> entrySet = facetMap.entrySet();
			for (Entry<String,BoboFacetSpec> entry : entrySet){
				breq.setFacetSpec(entry.getKey(), convert(entry.getValue()));
			}
		}
		return breq;
	}
	
	private static BoboHit convert(BrowseHit hit){
		BoboHit bhit = new BoboHit();
		bhit.setDocid(hit.getDocid());
		bhit.setScore(hit.getScore());
		bhit.setFields(hit.getFieldValues());
		return bhit;
	}
	
	private BoboResult convert(BrowseRequest req,BrowseResult res) throws Exception{
		BoboResult bres = new BoboResult();
		bres.setNumHits(res.getNumHits());
		bres.setTotalDocs(res.getTotalDocs());
		bres.setTime(res.getTime());
		BrowseHit[] bhits = res.getHits();
		List<BoboHit> hits = new ArrayList<BoboHit>(bhits.length);
		for (BrowseHit bhit : bhits){
			hits.add(convert(bhit));
		}
		Map<String,FacetAccessible> facetMap = res.getFacetMap();
		if (facetMap!=null && facetMap.size()>0){
			Map<String,List<FacetValue>> fmap = new HashMap<String,List<FacetValue>>();
			Set<Entry<String,FacetAccessible>> entrySet = facetMap.entrySet();
			for (Entry<String,FacetAccessible> entry : entrySet){
				String fldName = entry.getKey();
				FacetAccessible accessor = entry.getValue();
				List<BrowseFacet> facetList = accessor.getFacets();
				FacetSpec fspec = req.getFacetSpec(fldName);
				TreeSet<FacetValue> facetValList = new TreeSet<FacetValue>(getComparator(fspec));
				Set<String> selectedVals = new HashSet<String>();
				BrowseSelection sel = req.getSelection(fldName);
				if (sel!=null){
					String[] vals = sel.getValues();
					if (vals!=null && vals.length > 0){
						selectedVals.addAll(Arrays.asList(vals));
					}
				}
				for (BrowseFacet bfacet : facetList){
					FacetValue fval = new FacetValue();
					String val = bfacet.getValue();
					fval.setSelected(selectedVals.remove(val));
					fval.setValue(val);
					fval.setCount(bfacet.getHitCount());
					facetValList.add(fval);
				}
				for (String leftOver : selectedVals){
					BrowseFacet bfacet = accessor.getFacet(leftOver);
					FacetValue fval = new FacetValue();
					String val = bfacet.getValue();
					fval.setSelected(true);
					fval.setValue(val);
					fval.setCount(bfacet.getHitCount());
					facetValList.add(fval);
				}
				List<FacetValue> mergedList = new ArrayList<FacetValue>(facetValList);
				fmap.put(fldName, mergedList);
			}
			bres.setFacetResults(fmap);
		}
		bres.setHits(hits);
		return bres;
	}
	
	public BoboResult search(BoboRequest req) {
		try{
		  BrowseRequest breq = convert(req);
		  BrowseResult bres = _svc.browse(breq);
		  return convert(breq,bres);
		}
		catch(Exception e){
		  log.error(e.getMessage(),e);
		  return null;
		}
	}

	public void close() {
	  try {
		_svc.close();
	  } 
	  catch (BrowseException e) {
		log.error(e.getMessage(),e);
	  }
	}
}
