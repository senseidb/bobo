package com.browseengine.bobo.protobuf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.api.BrowseSelection.ValueOperation;
import com.browseengine.bobo.api.FacetSpec.FacetSortSpec;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;

public class BrowseProtobufConverter {
	
	private static Logger logger = Logger.getLogger(BrowseProtobufConverter.class);
	
	private static class FacetContainerAccessible implements FacetAccessible{
		private Map<String,BrowseFacet> _data;
		FacetContainerAccessible(BrowseResultBPO.FacetContainer facetContainer){
			_data = new HashMap<String,BrowseFacet>();
			if (facetContainer!=null){
				List<BrowseResultBPO.Facet> facetList = facetContainer.getFacetsList();
				if (facetList!=null){
					for (BrowseResultBPO.Facet facet : facetList){
						BrowseFacet bfacet = new BrowseFacet();
						String val = facet.getVal();
						bfacet.setValue(val);
						bfacet.setHitCount(facet.getCount());
						_data.put(val,bfacet);
					}
				}
			}
		}
		public BrowseFacet getFacet(String value) {
			return _data.get(value);
		}

		public List<BrowseFacet> getFacets() {
			Collection<BrowseFacet> set = _data.values();
			ArrayList<BrowseFacet> list = new ArrayList<BrowseFacet>(set.size());
			list.addAll(set);
			return list;
		}
	}
	
	public static BrowseHit convert(BrowseResultBPO.Hit hit){
		BrowseHit bhit = new BrowseHit();
		bhit.setDocid(hit.getDocid());
		bhit.setScore(hit.getScore());
		List<BrowseResultBPO.FieldVal> fieldValueList = hit.getFieldValuesList();
		Map<String,String[]> fielddata = new HashMap<String,String[]>();
		for (BrowseResultBPO.FieldVal fieldVal : fieldValueList){
			List<String> valList = fieldVal.getValsList();
			fielddata.put(fieldVal.getName(), valList.toArray(new String[valList.size()]));
		}
		bhit.setFieldValues(fielddata);
		return bhit;
	}
	
	public static BrowseResult convert(BrowseResultBPO.Result result){
		long time = result.getTime();
		int numhits = result.getNumhits();
		int totaldocs = result.getTotaldocs();
		List<BrowseResultBPO.FacetContainer> facetList = result.getFacetContainersList();
		List<BrowseResultBPO.Hit> hitList = result.getHitsList();
		BrowseResult res = new BrowseResult();
		res.setTime(time);
		res.setTotalDocs(totaldocs);
		res.setNumHits(numhits);
		for (BrowseResultBPO.FacetContainer facetContainer : facetList)
		{
			res.addFacets(facetContainer.getName(), new FacetContainerAccessible(facetContainer));
		}
		
		BrowseHit[] browseHits = new BrowseHit[hitList==null ? 0 : hitList.size()];
		int i=0;
		for (BrowseResultBPO.Hit hit : hitList)
		{
			browseHits[i++] = convert(hit);
		}
		res.setHits(browseHits);
		return res;
	}
	
	public static BrowseRequest convert(BrowseRequestBPO.Request req,QueryParser qparser) throws ParseException{
		BrowseRequest breq = new BrowseRequest();
		String query = req.getQuery();
		
		if (qparser!=null && query!=null && query.length() > 0){
			try{
			  Query q = qparser.parse(query);
			  breq.setQuery(q);
			}
			catch(Exception e){
				throw new ParseException(e.getMessage());
			}
		}
		breq.setOffset(req.getOffset());
		breq.setCount(req.getCount());
		
		int i = 0;
		
		List<BrowseRequestBPO.Sort> sortList = req.getSortList();
		SortField[] sortFields = new SortField[sortList == null ? 0 : sortList.size()];
		for (BrowseRequestBPO.Sort s : sortList){
			String fieldname = s.getField();
			if (fieldname!=null && fieldname.length() == 0){
				fieldname=null;
			}
			SortField sf = new SortField(fieldname,s.getType(),s.getReverse());
			sortFields[i++] = sf;
		}
		if (sortFields.length > 0){
		 breq.setSort(sortFields);
		}
		
		List<BrowseRequestBPO.FacetSpec> fspecList = req.getFacetSpecsList();
		for (BrowseRequestBPO.FacetSpec fspec : fspecList){
			FacetSpec facetSpec = new FacetSpec();
			facetSpec.setExpandSelection(fspec.getExpand());
			facetSpec.setMaxCount(fspec.getMax());
			facetSpec.setMinHitCount(fspec.getMinCount());
			BrowseRequestBPO.FacetSpec.SortSpec fsort = fspec.getOrderBy();
			if (fsort == BrowseRequestBPO.FacetSpec.SortSpec.HitsDesc)
			{
				facetSpec.setOrderBy(FacetSortSpec.OrderHitsDesc);
			}
			else
			{
				facetSpec.setOrderBy(FacetSortSpec.OrderValueAsc);	
			}
			breq.setFacetSpec(fspec.getName(), facetSpec);
		}
		
		List<BrowseRequestBPO.Selection> selList = req.getSelectionsList();
		for (BrowseRequestBPO.Selection sel : selList){
			BrowseSelection bsel = null;
			
			List<String> vals = sel.getValuesList();
			if (vals!=null)
			{
				if (bsel==null)
				{
					bsel = new BrowseSelection(sel.getName());
				}
				bsel.setValues(vals.toArray(new String[vals.size()]));
				
			}
			vals = sel.getNotValuesList();
			if (vals!=null)
			{
				if (bsel==null)
				{
					bsel = new BrowseSelection(sel.getName());
				}
				bsel.setNotValues(vals.toArray(new String[vals.size()]));
				
			}
			
			if (bsel!= null){
				BrowseRequestBPO.Selection.Operation operation = sel.getOp();
				if (operation == BrowseRequestBPO.Selection.Operation.OR){
					bsel.setSelectionOperation(ValueOperation.ValueOperationOr);
				}
				else{
					bsel.setSelectionOperation(ValueOperation.ValueOperationAnd);
				}
				List<BrowseRequestBPO.Property> props = sel.getPropsList();
				if (props!=null)
				{
				  for (BrowseRequestBPO.Property prop : props){
					  bsel.setSelectionProperty(prop.getKey(), prop.getVal());
				  }
				}
				breq.addSelection(bsel);
			}
			
		}
		return breq;
	}
	
	public static BrowseRequestBPO.Selection convert(BrowseSelection sel){
		String name = sel.getFieldName();
		String[] vals = sel.getValues();
		String[] notVals = sel.getNotValues();
		ValueOperation op =sel.getSelectionOperation();
		Properties props = sel.getSelectionProperties();
		
		BrowseRequestBPO.Selection.Builder selBuilder = BrowseRequestBPO.Selection.newBuilder();
		selBuilder.setName(name);
		selBuilder.addAllValues(Arrays.asList(vals));
		selBuilder.addAllNotValues(Arrays.asList(notVals));
		if (op == ValueOperation.ValueOperationAnd){
		  selBuilder.setOp(BrowseRequestBPO.Selection.Operation.AND);
		}
		else{
		  selBuilder.setOp(BrowseRequestBPO.Selection.Operation.OR);
		}
		Iterator iter = props.keySet().iterator();
		while(iter.hasNext()){
			String key = (String)iter.next();
			String val = props.getProperty(key);
			BrowseRequestBPO.Property prop = BrowseRequestBPO.Property.newBuilder().setKey(key).setVal(val).build();
			selBuilder.addProps(prop);
		}
		return selBuilder.build();
	}
	
	public static BrowseRequestBPO.Request convert(BrowseRequest req){
		Query q = req.getQuery();
		String qString = null;
		if (q!=null){
			if (q instanceof MatchAllDocsQuery)
				qString = "*:*";
			else
				qString = q.toString();
		}
		
		BrowseRequestBPO.Request.Builder reqBuilder = BrowseRequestBPO.Request.newBuilder();
		reqBuilder.setOffset(req.getOffset());
		reqBuilder.setCount(req.getCount());
		if (qString!=null){
		  reqBuilder.setQuery(qString);
		}
		// selections
		BrowseSelection[] selections = req.getSelections();
		for (BrowseSelection sel : selections){
			reqBuilder.addSelections(convert(sel));
		}
		
		// sort
		SortField[] sortfields = req.getSort();
		for (SortField sortfield : sortfields){
			String fn = sortfield.getField();
			BrowseRequestBPO.Sort.Builder sortBuilder = BrowseRequestBPO.Sort.newBuilder();
			if (fn!=null){
				sortBuilder.setField(fn);
			}
			BrowseRequestBPO.Sort sort = sortBuilder.setReverse(sortfield.getReverse()).setType(sortfield.getType()).build();
			reqBuilder.addSort(sort);
		}
		
		// facetspec
		Map<String,FacetSpec> facetSpecMap = req.getFacetSpecs();
		Iterator<Entry<String,FacetSpec>> iter = facetSpecMap.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String,FacetSpec> entry = iter.next();
			FacetSpec fspec = entry.getValue();
			if (fspec!=null)
			{
				BrowseRequestBPO.FacetSpec.Builder facetspecBuilder = BrowseRequestBPO.FacetSpec.newBuilder();
				facetspecBuilder.setName(entry.getKey());
				facetspecBuilder.setExpand(fspec.isExpandSelection());
				facetspecBuilder.setMax(fspec.getMaxCount());
				facetspecBuilder.setMinCount(fspec.getMinHitCount());
				if (fspec.getOrderBy() == FacetSortSpec.OrderHitsDesc){
				  facetspecBuilder.setOrderBy(BrowseRequestBPO.FacetSpec.SortSpec.HitsDesc);
				}
				else{
				  facetspecBuilder.setOrderBy(BrowseRequestBPO.FacetSpec.SortSpec.ValueAsc);
				}
				reqBuilder.addFacetSpecs(facetspecBuilder);
			}
			else{
				logger.warn("facet handler: "+entry.getKey()+" is null, skipped");
			}
		}
		return reqBuilder.build();
	}
	
	public static BrowseResultBPO.Hit convert(BrowseHit hit){
		BrowseResultBPO.Hit.Builder hitBuilder = BrowseResultBPO.Hit.newBuilder();
		hitBuilder.setDocid(hit.getDocid());
		hitBuilder.setScore(hit.getScore());
		Map<String,String[]> fieldMap = hit.getFieldValues();
		Iterator<Entry<String,String[]>> iter = fieldMap.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String,String[]> entry = iter.next();
			BrowseResultBPO.FieldVal fieldVal = BrowseResultBPO.FieldVal.newBuilder().setName(entry.getKey()).addAllVals(Arrays.asList(entry.getValue())).build();
			hitBuilder.addFieldValues(fieldVal);
		}
		return hitBuilder.build();
	}
	
	public static BrowseResultBPO.FacetContainer convert(String name,FacetAccessible facetAccessible){
		BrowseResultBPO.FacetContainer.Builder facetBuilder = BrowseResultBPO.FacetContainer.newBuilder();
		facetBuilder.setName(name);
		List<BrowseFacet> list = facetAccessible.getFacets();
		for (BrowseFacet facet : list){
			BrowseResultBPO.Facet f = BrowseResultBPO.Facet.newBuilder().setVal(facet.getValue()).setCount(facet.getHitCount()).build();
			facetBuilder.addFacets(f);
		}
		return facetBuilder.build();
	}
	
	public static BrowseResultBPO.Result convert(BrowseResult res){
		BrowseResultBPO.Result.Builder resBuilder = BrowseResultBPO.Result.newBuilder();
		resBuilder.setTime(res.getTime());
		resBuilder.setTotaldocs(res.getTotalDocs());
		resBuilder.setNumhits(res.getNumHits());
		
		// hits
		BrowseHit[] hits = res.getHits();
		for (BrowseHit hit : hits){
			BrowseResultBPO.Hit converted = convert(hit);
			resBuilder.addHits(converted);
		}
		
		// facet containers
		Map<String,FacetAccessible> facetMap = res.getFacetMap();
		Iterator<Entry<String,FacetAccessible>> iter = facetMap.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String,FacetAccessible> entry = iter.next();
			BrowseResultBPO.FacetContainer converted = convert(entry.getKey(),entry.getValue());
			resBuilder.addFacetContainers(converted);
		}
		return resBuilder.build();
	}
	
	public static String toProtoBufString(BrowseRequest req){
		BrowseRequestBPO.Request protoReq = convert(req);
		String outString = TextFormat.printToString(protoReq);
		outString = outString.replace('\r', ' ').replace('\n', ' ');
		return outString;
	}
	
	public static BrowseRequest fromProtoBufString(String str,QueryParser qparser) throws ParseException{
		BrowseRequestBPO.Request.Builder protoReqBuilder = BrowseRequestBPO.Request.newBuilder();
		TextFormat.merge(str, protoReqBuilder);
		BrowseRequestBPO.Request protoReq = protoReqBuilder.build();
		try{
		return convert(protoReq,qparser);
		}
		catch(Exception e){
			throw new ParseException(e.getMessage());
		}
	}
}
