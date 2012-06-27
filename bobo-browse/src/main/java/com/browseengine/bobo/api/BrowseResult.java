/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  John Wang
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
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * send mail to owner@browseengine.com.
 */

package com.browseengine.bobo.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.browseengine.bobo.mapred.MapReduceResult;
import com.browseengine.bobo.mapred.BoboMapFunctionWrapper;
import com.browseengine.bobo.sort.SortCollector;


/**
 * A Browse result
 */
public class BrowseResult implements Serializable{
	private static final long serialVersionUID = -8620935391852879446L;
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

	private int numHits;
	private int numGroups;
	private int totalDocs;
	private FacetAccessible[] _groupAccessibles;
    transient private SortCollector _sortCollector;
  //private int totalGroups;
	private Map<String,FacetAccessible> _facetMap;
	private BrowseHit[] hits;
	private long time;
	private MapReduceResult mapReduceResult;
  private List<String> errors;
	private static BrowseHit[] NO_HITS=new BrowseHit[0];
		
	/**
	 * Constructor
	 */
	public BrowseResult() {
		super();
		_facetMap=new HashMap<String,FacetAccessible>();
    _groupAccessibles = null;
    _sortCollector = null;
		numHits=0;
		numGroups=0;
		totalDocs=0;
    //totalGroups=0;
		hits=null;
		time=0L;
	}

  /**
   * Get the group accessible.
   * @return the group accessible.
   */
  public FacetAccessible[] getGroupAccessibles() {
    return _groupAccessibles;
  }

  /**
   * Set the group accessible.
   * @param groupAccessible the group accessible.
   */
  public BrowseResult setGroupAccessibles(FacetAccessible[] groupAccessibles) {
    _groupAccessibles = groupAccessibles;
    return this;
  }

  /**
   * Get the sort collector.
   * @return the sort collector.
   */
  public SortCollector getSortCollector() {
    return _sortCollector;
  }

  /**
   * Set the sort collector.
   * @param sortCollector the sort collector
   */
  public BrowseResult setSortCollector(SortCollector sortCollector) {
    _sortCollector = sortCollector;
    return this;
  }
	
	/**
	 * Get the facets by name
	 * @param name
	 * @return FacetAccessible instance corresponding to the name
	 */
	public FacetAccessible getFacetAccessor(String name) {
      return _facetMap.get(name);
	}
	
	/**
	 * Get the hit count
	 * @return hit count
	 * @see #setNumHits(int)
	 */
	public int getNumHits() {
		return numHits;
	}

	/**
	 * Sets the hit count
	 *
   * @param hits hit count
   * @see #getNumHits()
	 */
	public BrowseResult setNumHits(int hits) {
		numHits = hits;
    return this;
	}

	/**
	 * Get the group count
	 * @return group count
	 * @see #setNumGroups(int)
	 */
	public int getNumGroups() {
		return numGroups;
	}

	/**
	 * Sets the group count
	 *
   * @param groups group count
   * @see #getNumGroups()
	 */
	public BrowseResult setNumGroups(int groups) {
		numGroups = groups;
    return this;
	}

	/**
	 * Gets the total number of docs in the index
	 * @return total number of docs in the index.
	 * @see #setTotalDocs(int)
	 */
	public int getTotalDocs() {
		return totalDocs;
	}

	/**
	 * Sets the total number of docs in the index
	 *
   * @param docs total number of docs in the index
   * @see #getTotalDocs()
	 */
	public BrowseResult setTotalDocs(int docs) {
		totalDocs = docs;
    return this;
	}
	
	/**
	 * Gets the total number of groups in the index
	 * @return total number of groups in the index.
	 * @see #setTotalGroups(int)
	 */
	//public int getTotalGroups() {
		//return totalGroups;
	//}

	/**
	 * Sets the total number of groups in the index
	 * @param groups total number of groups in the index
	 * @see #getTotalGroups()
	 */
	//public void setTotalGroups(int groups) {
		//totalGroups = groups;
	//}
	
	/**
	 * Add a container full of choices
   * @param facets container full of facets
   */
	public BrowseResult addFacets(String name, FacetAccessible facets){
		_facetMap.put(name,facets);
    return this;
	}	
	
	/**
	 * Add all of the given FacetAccessible to this BrowseResult
   * @param facets map of facets to add to the result set
   */
	public BrowseResult addAll(Map<String, FacetAccessible> facets){
		_facetMap.putAll(facets);
    return this;
	}
	
	/**
	 * Sets the hits
	 *
   * @param hits hits
   * @see #getHits()
	 */
	public BrowseResult setHits(BrowseHit[] hits){
		this.hits=hits;
    return this;
	}
	
	/**
	 * Gets the hits
	 * @return hits
	 * @see #setHits(BrowseHit[])
	 */
	public BrowseHit[] getHits(){
		return hits==null ? NO_HITS : hits;
	}
	
	/**
	 * Sets the search time in milliseconds
	 *
   * @param time search time
   * @see #getTime()
	 */
	public void setTime(long time){
		this.time=time;
	}
	
	/**
	 * Gets the search time in milliseconds
	 * @return search time
	 * @see #setTime(long)
	 */
	public long getTime(){
		return time;
	}
	
	/**
	 * Gets all the facet collections
	 * @return list of facet collections
	 */
	public Map<String,FacetAccessible> getFacetMap(){
		return _facetMap;
	}
    
	public MapReduceResult getMapReduceResult() {
		return mapReduceResult;
	}

	public void setMapReduceResult(MapReduceResult mapReduceWrapper) {
		this.mapReduceResult = mapReduceWrapper;
	}

	public static String toString(Map<String,FacetAccessible> map) {
		StringBuilder buffer=new StringBuilder();
		Set<Entry<String,FacetAccessible>> entries = map.entrySet();
		
		buffer.append("{");
		for (Entry<String,FacetAccessible> entry : entries)
		{
			String name = entry.getKey();
			FacetAccessible facetAccessor = entry.getValue();
			buffer.append("name=").append(name).append(",");
			buffer.append("facets=").append(facetAccessor.getFacets()).append(";");
		}
		buffer.append("}").append('\n');
		return buffer.toString();
	}
	
	@Override
	public String toString(){
		StringBuilder buf=new StringBuilder();
		buf.append("hit count: ").append(numHits).append('\n');
		buf.append("total docs: ").append(totalDocs).append('\n');
		buf.append("facets: ").append(toString(_facetMap));
		buf.append("hits: ").append(Arrays.toString(hits));
		return buf.toString();
	}
	
	public void close()
	{
    if (_groupAccessibles != null)
    {
      for(FacetAccessible accessible : _groupAccessibles)
      {
        if (accessible != null)
          accessible.close();
      }
    }
    if (_sortCollector != null)
      _sortCollector.close();
	  if (_facetMap == null) return;
	  Collection<FacetAccessible> accessibles = _facetMap.values();
	  for(FacetAccessible fa : accessibles)
	  {
	    fa.close();
	  }
	}

  public void addError(String message) {
    if (errors == null)
      errors = new ArrayList<String>(1);

    errors.add(message);
  }

  public List<String> getBoboErrors() {
    if (errors == null)
      errors = new ArrayList<String>(1);

    return errors;
  }
}
