package com.browseengine.bobo.facets.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.FacetCountCollectorSource;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.facets.FacetHandler.FacetDataNone;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessOrFilter;
import com.browseengine.bobo.sort.DocComparatorSource;

public class ComboFacetHandler extends FacetHandler<FacetDataNone> {

	private static final String DFEAULT_SEPARATOR = ":";
	private final String _separator;
	
	
	public ComboFacetHandler(String name,Set<String> dependsOn){
		this(name,DFEAULT_SEPARATOR,dependsOn);
	}
	
	public ComboFacetHandler(String name,String seperator,Set<String> dependsOn){
		super(name,dependsOn);
		_separator = seperator;
	}
	
	public String getSeparator(){
		return _separator;
	}
	
	private static class ComboSelection{
		final String name;
		final String val;
		
		private ComboSelection(String name,String val){
			this.name = name;
			this.val = val;
		}
		
		static ComboSelection parse(String value,String sep){
			StringTokenizer strtok = new StringTokenizer(value, sep);
			if (strtok.hasMoreTokens()){
				String name = strtok.nextToken();
				if (strtok.hasMoreTokens()){
					String val = strtok.nextToken();
					return new ComboSelection(name,val);
				}
			}
			return null;
		}
	}
	
	@Override
	public RandomAccessFilter buildRandomAccessFilter(String value,
			Properties selectionProperty) throws IOException {
		RandomAccessFilter retFilter = EmptyFilter.getInstance();
		ComboSelection comboSel = ComboSelection.parse(value, _separator);
		if (comboSel!=null){
			FacetHandler<?> handler =getDependedFacetHandler(comboSel.name);
			if (handler!=null){
			   retFilter = handler.buildRandomAccessFilter(comboSel.val, selectionProperty);
			}
		}
		return retFilter;
	}
	
	private static Map<String,List<String>> convertMap(String[] vals,String sep){
		Map<String,List<String>> retmap = new HashMap<String,List<String>>();
		for (String val : vals){
			ComboSelection sel = ComboSelection.parse(val, sep);
			if (sel!=null){
			   List<String> valList = retmap.get(sel.name);
			   if (valList==null){
				   valList = new LinkedList<String>();
				   retmap.put(sel.name, valList);
			   }
			   valList.add(sel.val);
			}
		}
		return retmap;
	}

	@Override
	public RandomAccessFilter buildRandomAccessAndFilter(String[] vals,
			Properties prop) throws IOException {
		Map<String,List<String>> valMap = convertMap(vals,_separator);
		Set<Entry<String,List<String>>> entries = valMap.entrySet();
		
		List<RandomAccessFilter> filterList = new LinkedList<RandomAccessFilter>();
		for (Entry<String,List<String>> entry : entries){
			String name = entry.getKey();
			FacetHandler<?> facetHandler = getDependedFacetHandler(name);
			if (facetHandler == null){
				return EmptyFilter.getInstance();
			}
			List<String> selVals = entry.getValue();
			if (selVals==null || selVals.size()==0) return EmptyFilter.getInstance();
			RandomAccessFilter f = facetHandler.buildRandomAccessAndFilter(selVals.toArray(new String[0]), prop);
			if (f == EmptyFilter.getInstance()) return f;
			filterList.add(f);
		}
		
		if (filterList.size() == 0){
			return EmptyFilter.getInstance();
		}
		if (filterList.size() == 1){
			return filterList.get(0);
		}
		return new RandomAccessAndFilter(filterList);
	}

	@Override
	public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,
			Properties prop, boolean isNot) throws IOException {
		Map<String,List<String>> valMap = convertMap(vals,_separator);
		
        Set<Entry<String,List<String>>> entries = valMap.entrySet();
		
		List<RandomAccessFilter> filterList = new LinkedList<RandomAccessFilter>();
		for (Entry<String,List<String>> entry : entries){
			String name = entry.getKey();
			FacetHandler<?> facetHandler = getDependedFacetHandler(name);
			if (facetHandler == null){
				continue;
			}
			List<String> selVals = entry.getValue();
			if (selVals==null || selVals.size()==0){
				continue;
			}
			RandomAccessFilter f = facetHandler.buildRandomAccessOrFilter(selVals.toArray(new String[0]), prop,isNot);
			if (f == EmptyFilter.getInstance()) continue;
			filterList.add(f);
		}
		
		if (filterList.size() == 0){
			return EmptyFilter.getInstance();
		}
		if (filterList.size() == 1){
			return filterList.get(0);
		}
		
		if (isNot){
		  return new RandomAccessAndFilter(filterList);
		}
		else{
		  return new RandomAccessOrFilter(filterList);
		}
	}

	@Override
	public DocComparatorSource getDocComparatorSource() {
		throw new UnsupportedOperationException("sorting not supported for "+ComboFacetHandler.class);
	}

	@Override
	public FacetCountCollectorSource getFacetCountCollectorSource(
			BrowseSelection sel, FacetSpec fspec) {
		throw new UnsupportedOperationException("facet counting not supported for "+ComboFacetHandler.class);
	}

	@Override
	public String[] getFieldValues(BoboIndexReader reader, int id) {		
		Set<String> dependsOn = this.getDependsOn();
		List<String> valueList = new LinkedList<String>();
		for (String depends : dependsOn){
			FacetHandler<?> facetHandler = getDependedFacetHandler(depends);
			String[] fieldValues = facetHandler.getFieldValues(reader, id);
			for (String fieldVal : fieldValues){
			  StringBuilder buf = new StringBuilder();
			  buf.append(depends).append(_separator).append(fieldVal);
			  valueList.add(buf.toString());
			}
		}
		return valueList.toArray(new String[0]);
	}
	
	

	@Override
	public int getNumItems(BoboIndexReader reader, int id) {
		Set<String> dependsOn = this.getDependsOn();
		List<String> valueList = new LinkedList<String>();
		int count = 0;
		for (String depends : dependsOn){
			FacetHandler<?> facetHandler = getDependedFacetHandler(depends);
			String[] fieldValues = facetHandler.getFieldValues(reader, id);
			if (fieldValues!=null){
				count++;
			}
		}
		return count;
	}

	@Override
	public FacetDataNone load(BoboIndexReader reader) throws IOException {
		return FacetDataNone.instance;
	}
}
