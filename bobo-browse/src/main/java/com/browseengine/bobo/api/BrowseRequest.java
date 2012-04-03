package com.browseengine.bobo.api;

/**
 * 
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  jwang
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/.
 * <or other contact info for bobo-browse; snail mail/email/both>
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import com.browseengine.bobo.facets.FacetHandlerInitializerParam;
import com.browseengine.bobo.mapred.BoboMapFunctionWrapper;

/**
 * Browse Request.
 * @author jwang
 * @version 1.0
 * @since 1.0
 */
public class BrowseRequest implements Serializable{

	private static final long serialVersionUID = 3172092238778154933L;
  /**
   * The transaction ID
   */
	private long tid = -1;
	/**
	 * Get the transaction ID.
	 * @return the transaction ID.
	 */
	public final long getTid()
  {
    return tid;
  }

  /**
   * Set the transaction ID;
   * @param tid
   */
  public final void setTid(long tid)
  {
    this.tid = tid;
  }

    private HashMap<String,BrowseSelection> _selections;
	private ArrayList<SortField> _sortSpecs;
	private Map<String,FacetSpec> _facetSpecMap;
	private Map<String,FacetHandlerInitializerParam> _facetHandlerDataMap;
    private Query _query;
	private int _offset;
	private int _count;
	private boolean _fetchStoredFields;
	private Filter _filter;
	private boolean _showExplanation;
	private String _groupBy;  // TODO: Leave here for backward compatible reason, will remove it later.
	private String[] _groupByMulti;
	private int _maxPerGroup;
	private boolean _collectDocIdCache;
	private Set<String> _termVectorsToFetch;
	private BoboMapFunctionWrapper mapReduceWrapper;
	
	public Set<String> getTermVectorsToFetch(){
	  return _termVectorsToFetch;
	}
	
	public void setTermVectorsToFetch(Set<String> termVectorsToFetch){
	  _termVectorsToFetch = termVectorsToFetch;
	}
	
	public boolean isShowExplanation() {
		return _showExplanation;
	}

	public BrowseRequest setShowExplanation(boolean showExplanation) {
		_showExplanation = showExplanation;
    return this;
	}

	public Set<String> getSelectionNames(){
		return _selections.keySet();
	}
	
	public void removeSelection(String name){
		_selections.remove(name);
	}
	
	public BrowseRequest setFacetSpecs(Map<String, FacetSpec> facetSpecMap)
	{
		_facetSpecMap = facetSpecMap;
    return this;
	}
	
	public Map<String,FacetSpec> getFacetSpecs()
	{
		return _facetSpecMap;
	}
	
  /**
   * @return The map between <b>RuntimeFacetHandler</b> names and their corresponding initialization data.
   */
  public Map<String, FacetHandlerInitializerParam> getFacetHandlerDataMap()
  {
    return _facetHandlerDataMap;
  }

  /**
   * Sets the map between RuntimeFacetHandler names and their corresponding initialization data.
   * @param facetHandlerDataMap
   */
  public BrowseRequest setFacetHandlerDataMap(
          Map<String, FacetHandlerInitializerParam> facetHandlerDataMap)
  {
    _facetHandlerDataMap = facetHandlerDataMap;
    return this;
  }
	
	public int getSelectionCount()
	{
		return _selections.size();
	}
	
	/**
	 * Set a default filter
   * @param filter
   */
	public BrowseRequest setFilter(Filter filter){
		_filter=filter;
    return this;
	}
	
	/**
	 * Gets the default filter
	 */
	public Filter getFilter(){
		return _filter;
	}
	
	public BrowseRequest clearSelections(){
		_selections.clear();
    return this;
	}
	
	/**
	 * Gets the number of facet specs
	 * @return number of facet pecs
	 * @see #setFacetSpec(String, FacetSpec)
	 * @see #getFacetSpec(String)
	 */
	public int getFacetSpecCount(){
		return _facetSpecMap.size();
	}

	/**
	 * Constructor.
	 */
	public BrowseRequest() {
		super();
		_selections=new HashMap<String,BrowseSelection>();
		_sortSpecs=new ArrayList<SortField>();
		_facetSpecMap=new HashMap<String,FacetSpec>();
		_facetHandlerDataMap = new HashMap<String, FacetHandlerInitializerParam>();
		_filter = null;
		_fetchStoredFields = false;
		_groupBy = null;
    _groupByMulti = null;
    _maxPerGroup = 0;
    _collectDocIdCache = false;
	}
	
	public BrowseRequest clearSort(){
		_sortSpecs.clear();
    return this;
	}
	
	public boolean isFetchStoredFields(){
		return _fetchStoredFields;
	}
	
	public BrowseRequest setFetchStoredFields(boolean fetchStoredFields){
		_fetchStoredFields = fetchStoredFields;
    return this;
	}
	
	public String[] getGroupBy(){
    if (_groupByMulti == null && _groupBy != null)
      _groupByMulti = new String[]{_groupBy};

		return _groupByMulti;
	}
	
	public BrowseRequest setGroupBy(String[] groupBy){
		_groupByMulti = groupBy;
    if (_groupByMulti != null && _groupByMulti.length != 0)
      _groupBy = _groupByMulti[0];

    return this;
	}

	public int getMaxPerGroup(){
		return _maxPerGroup;
	}
	
	public BrowseRequest setMaxPerGroup(int maxPerGroup){
		_maxPerGroup = maxPerGroup;
    return this;
	}

	public boolean getCollectDocIdCache(){
		return _collectDocIdCache;
	}
	
	public BrowseRequest setCollectDocIdCache(boolean collectDocIdCache){
		_collectDocIdCache = collectDocIdCache;
    return this;
	}
	
	/**
	 * Sets a facet spec
	 *
   * @param name field name
   * @param facetSpec Facet spec
   * @see #getFacetSpec(String)
	 */
	public BrowseRequest setFacetSpec(String name, FacetSpec facetSpec){
		_facetSpecMap.put(name,facetSpec);
    return this;
	}
	
	/**
	 * Gets a facet spec
	 * @param name field name
	 * @return facet spec
	 * @see #setFacetSpec(String, FacetSpec)
	 */
	public FacetSpec getFacetSpec(String name){
		return _facetSpecMap.get(name);
	}

	/**
   * @param name is the name of the <b>RuntimeFacetHandler</b>.
   * @param data is the data Bobo is to use to initialize the corresponding RuntimeFacetHandler.
   */
	public BrowseRequest setFacetHandlerData(String name, FacetHandlerInitializerParam data)
	{
	  _facetHandlerDataMap.put(name, data);
    return this;
	}

	/**
	 * @param name is the name of the <b>RuntimeFacetHandler</b>.
	 * @return the data Bobo is to use to initialize the corresponding RuntimeFacetHandler.
	 */
	public FacetHandlerInitializerParam getFacethandlerData(String name)
	{
	  return _facetHandlerDataMap.get(name);
	}

	/**
	 * Gets the number of hits to return. Part of the paging parameters.
	 * @return number of hits to return.
	 * @see #setCount(int)
	 */
	public int getCount() {
		return _count;
	}

	/**
	 * Sets the number of hits to return. Part of the paging parameters.
	 *
   * @param count number of hits to return.
   * @see #getCount()
	 */
	public BrowseRequest setCount(int count) {
		_count = count;
    return this;
	}

	/**
	 * Gets the offset. Part of the paging parameters.
	 * @return offset
	 * @see #setOffset(int)
	 */
	public int getOffset() {
		return _offset;
	}

	/**
	 * Sets of the offset. Part of the paging parameters.
	 *
   * @param offset offset
   * @see #getOffset()
	 */
	public BrowseRequest setOffset(int offset) {
		_offset = offset;
    return this;
	}

	/**
	 * Set the search query
	 *
   * @param query lucene search query
   * @see #getQuery()
	 */
	public BrowseRequest setQuery(Query query){
		_query=query;
    return this;
	}
	
	/**
	 * Gets the search query
	 * @return lucene search query
	 * @see #setQuery(Query)
	 */
	public Query getQuery(){
		return _query;
	}
	
	/**
	 * Adds a browse selection
	 *
   * @param sel selection
   * @see #getSelections()
	 */
	public BrowseRequest addSelection(BrowseSelection sel){
		String[] vals = sel.getValues();
		if (vals==null || vals.length == 0)
		{
			String[] notVals = sel.getNotValues();
			if (notVals==null || notVals.length == 0) return null;
		}
		_selections.put(sel.getFieldName(),sel);
    return this;
	}
	
	/**
	 * Gets all added browse selections
	 * @return added selections
	 * @see #addSelection(BrowseSelection)
	 */
	public BrowseSelection[] getSelections(){
		return _selections.values().toArray(new BrowseSelection[_selections.size()]);
	}
	
	/**
	 * Gets selection by field name
	 * @param fieldname
	 * @return selection on the field
	 */
	public BrowseSelection getSelection(String fieldname){
	  return _selections.get(fieldname);
	}
	
	public Map<String,BrowseSelection> getAllSelections(){
		return _selections;
	}
	
	public BrowseRequest putAllSelections(Map<String, BrowseSelection> map){
		_selections.putAll(map);
    return this;
	}
		
	public BoboMapFunctionWrapper getMapReduceWrapper() {
		return mapReduceWrapper;
	}

	public BrowseRequest setMapReduceWrapper(BoboMapFunctionWrapper mapReduceWrapper) {
		this.mapReduceWrapper = mapReduceWrapper;
		return this;
	}

	/**
	 * Add a sort spec
	 *
   * @param sortSpec sort spec
   * @see #getSort()
	 * @see #setSort(SortField[])
	 */
	public BrowseRequest addSortField(SortField sortSpec){
		_sortSpecs.add(sortSpec);
    return this;
	}

	/**
	 * Gets the sort criteria
	 * @return sort criteria
	 * @see #setSort(SortField[])
	 * @see #addSortField(SortField)
	 */
	public SortField[] getSort(){
		return _sortSpecs.toArray(new SortField[_sortSpecs.size()]);
	}
	
	/**
	 * Sets the sort criteria
	 *
   * @param sorts sort criteria
   * @see #addSortField(SortField)
	 * @see #getSort()
	 */
	public BrowseRequest setSort(SortField[] sorts){
		_sortSpecs.clear();
		for (int i=0;i<sorts.length;++i){
			_sortSpecs.add(sorts[i]);
		}
    return this;
	}

	@Override
	public String toString(){
	  StringBuilder buf=new StringBuilder();
      buf.append("query: ").append(_query).append('\n');
      buf.append("page: [").append(_offset).append(',').append(_count).append("]\n");
      buf.append("sort spec: ").append(_sortSpecs).append('\n');
      buf.append("selections: ").append(_selections).append('\n');
      buf.append("facet spec: ").append(_facetSpecMap).append('\n');
      buf.append("fetch stored fields: ").append(_fetchStoredFields).append('\n');
      buf.append("group by: ").append(_groupBy);
      return buf.toString();
	}
}
