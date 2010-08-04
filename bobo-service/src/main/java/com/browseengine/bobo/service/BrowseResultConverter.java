package com.browseengine.bobo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.MappedFacetAccessible;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class BrowseResultConverter implements Converter {

	public void marshal(Object obj, HierarchicalStreamWriter writer,
			MarshallingContext ctx) {
		BrowseResult result=(BrowseResult)obj;
		writer.addAttribute("numhits", String.valueOf(result.getNumHits()));
		writer.addAttribute("totaldocs", String.valueOf(result.getTotalDocs()));
		writer.addAttribute("time", String.valueOf(result.getTime()));
		
		writer.startNode("facets");
		Set<Entry<String,FacetAccessible>> facetAccessors=result.getFacetMap().entrySet();
		
		writer.addAttribute("count", String.valueOf(facetAccessors.size()));
		
		for (Entry<String,FacetAccessible> entry : facetAccessors){
			String choiceName=entry.getKey();
			FacetAccessible facetAccessor = entry.getValue();
			
			List<BrowseFacet> facetList = facetAccessor.getFacets();
			
			writer.startNode("facet");
			writer.addAttribute("name", choiceName);
			
			writer.addAttribute("facetcount", String.valueOf(facetList.size()));
			
			for (BrowseFacet facet : facetList){
				writer.startNode("facet-value");
				writer.addAttribute("value", String.valueOf(facet.getValue()));
				writer.addAttribute("count", String.valueOf(facet.getHitCount()));
				writer.endNode();
			}
			writer.endNode();
		}
		writer.endNode();
		writer.startNode("hits");
		BrowseHit[] hits=result.getHits();

		writer.addAttribute("length", String.valueOf(hits==null ? 0 : hits.length));
		
		for (BrowseHit hit : hits){
			ctx.convertAnother(hit);
		}
		
		writer.endNode();
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext ctx) {		
		BrowseResult res=new BrowseResult();
		
		String numHitsString=reader.getAttribute("numhits");
		if (numHitsString!=null){
			res.setNumHits(Integer.parseInt(numHitsString));
		}
		
		String totalDocsString=reader.getAttribute("totaldocs");
		if (totalDocsString!=null){
			res.setTotalDocs(Integer.parseInt(totalDocsString));
		}
		
		String timeString=reader.getAttribute("time");
		if (timeString!=null){
			res.setTime(Long.parseLong(timeString));
		}
		
		while (reader.hasMoreChildren()){
			reader.moveDown();
			if ("facets".equals(reader.getNodeName())){
				Map<String,FacetAccessible> facetMap = new HashMap<String,FacetAccessible>();
				String facetCountString = reader.getAttribute("count");
				if (facetCountString!=null){
					int count = Integer.parseInt(facetCountString);
					if (count > 0){
						for (int i=0;i<count;++i){
							reader.moveDown();
							String name = reader.getAttribute("name");
							String countStr = reader.getAttribute("facetcount");
							int fcount = 0;
							if (countStr!=null){
								fcount = Integer.parseInt(countStr);
							}
							BrowseFacet[] facets = new BrowseFacet[fcount];
							for (int k=0;k<fcount;++k){
								facets[k]=new BrowseFacet();
								reader.moveDown();
								String valueString=reader.getAttribute("value");
								facets[k].setValue(valueString);
								
								String countString=reader.getAttribute("count");
								if (countString!=null){
									facets[k].setHitCount(Integer.parseInt(countString));
								}
								reader.moveUp();
							}
							facetMap.put(name,new MappedFacetAccessible(facets));
							reader.moveUp();
						}
						res.addAll(facetMap);
					}
				}
			}
			else if ("hits".equals(reader.getNodeName())){
				String countStr = reader.getAttribute("length");
				int hitLen = Integer.parseInt(countStr);
				BrowseHit[] hits = new BrowseHit[hitLen];
				for (int i = 0; i< hitLen; ++i){
					hits[i]=(BrowseHit)ctx.convertAnother(res, BrowseHit.class);
				}
				res.setHits(hits);
			}
			reader.moveUp();
		}
		return res;
	}

	public boolean canConvert(Class clazz) {
		return BrowseResult.class.equals(clazz);
	}

}
