package com.browseengine.bobo.api;

import java.io.Serializable;
import java.util.Comparator;

/**
 * specifies how facets are to be returned for a browse
 *
 */
public class FacetSpec implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Sort options for facets
	 */
	public static enum FacetSortSpec
	{
	  /**
	   * Order by the facet values in lexographical ascending order
	   */
		OrderValueAsc,
		
		/**
		 * Order by the facet hit counts in descending order
		 */
		OrderHitsDesc,
		
		/**
		 * custom order, must have a comparator
		 */
		OrderByCustom
	}
			
	private FacetSortSpec orderBy;
	private int max;
	private boolean expandSelection;
	private int minCount;
	private ComparatorFactory _comparatorFactory;

	/**
	 * Constructor.
	 *
	 */
	public FacetSpec(){
		orderBy=FacetSortSpec.OrderValueAsc;
		minCount=1;
		expandSelection = false;
		_comparatorFactory = null;
	}				
	
	public void setCustomComparatorFactory(ComparatorFactory comparatorFactory){
		_comparatorFactory = comparatorFactory;
	}
	
	public ComparatorFactory getCustomComparatorFactory(){
		return _comparatorFactory;
	}
	
    /**
	 * Sets the minimum number of hits a choice would need to have to be returned.
	 * @param minCount minimum count
	 * @see #getMinHitCount()
	 */
	public void setMinHitCount(int minCount){
		this.minCount=minCount;
	}
	
	/**
	 * Gets the minimum number of hits a choice would need to have to be returned.
	 * @return minimum count
	 * @see #setMinHitCount(int)
	 */
	public int getMinHitCount(){
		return minCount;
	}
	
	/**
	 * Get the current choice sort order
	 * @return choice sort order
	 * @see #setOrderBy(FacetSortSpec)
	 */
	public FacetSortSpec getOrderBy() {
		return orderBy;
	}

	/**
	 * Sets the choice sort order
	 * @param order sort order
	 * @see #getOrderBy()
	 */
	public void setOrderBy(FacetSortSpec order) {
		orderBy = order;
	}				

	/**
	 * Gets the maximum number of choices to return
	 * @return max number of choices to return
	 * @see #setMaxCount(int)
	 */
	public int getMaxCount() {
		return max;
	}

	/**
	 * Sets the maximum number of choices to return.
	 * @param maxCount max number of choices to return, default = 0 which means all
	 * @see #getMaxCount()
	 */
	public void setMaxCount(int maxCount) {
		max = maxCount;
	}

	@Override
	public String toString(){
		StringBuffer buffer=new StringBuffer();			
		buffer.append("orderBy: ").append(orderBy).append("\n");
		buffer.append("max count: ").append(max).append("\n");
		buffer.append("min hit count: ").append(minCount).append("\n");
		buffer.append("expandSelection: ").append(expandSelection);
		return buffer.toString();
	}

	/**
	 * Gets whether we are expanding sibling choices
	 * @return A boolean indicating whether to expand sibling choices.
	 * @see #setExpandSelection(boolean)
	 */
	public boolean isExpandSelection() {
		return expandSelection;
	}

	/**
	 * Sets whether we are expanding sibling choices
	 * @param expandSelection indicating whether to expand sibling choices.
	 * @see #isExpandSelection()
	 */
	public void setExpandSelection(boolean expandSelection) {
		this.expandSelection = expandSelection;
	}
}
