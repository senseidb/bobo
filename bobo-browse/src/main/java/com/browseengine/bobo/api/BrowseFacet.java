package com.browseengine.bobo.api;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * This class represents a facet
 */
public class BrowseFacet implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String _value;
	private int _hitcount;
	
	public BrowseFacet()
	{
	}
	
	public BrowseFacet(String value,int hitcount)
	{
		_value=value;
		_hitcount=hitcount;
	}
	
	/**
	 * Gets the facet value
	 * @return value
	 * @see #setValue(String)
	 */
	public String getValue(){
		return _value;
	}
	
	/**
	 * Sets the facet value
	 *
   * @param value Facet value
   * @see #getValue()
	 */
	public BrowseFacet setValue(String value){
		_value=value;
    return this;
	}
	
	/**
	 * Gets the hit count
	 * @return hit count
	 * @deprecated use {@link #getFacetValueHitCount()}
	 */
	public int getHitCount(){
		return _hitcount;
	}
	
	/**
	 * Sets the hit count
	 *
   * @param hitcount Hit count
   * @deprecated use {@link #setFacetValueHitCount(int)}
	 */
	public BrowseFacet setHitCount(int hitcount){
		_hitcount=hitcount;
    return this;
	}
	
	/**
	 * Gets the hit count
	 * @return hit count
	 * @see #setHitCount(int)
	 */
	public int getFacetValueHitCount(){
		return _hitcount;
	}
	
	/**
	 * Sets the hit count
	 *
   * @param hitcount Hit count
   * @see #getHitCount()
	 */
	public BrowseFacet setFacetValueHitCount(int hitcount){
		_hitcount=hitcount;
    return this;
	}
	
	@Override
	public String toString(){
		StringBuilder buf=new StringBuilder();	
		buf.append(_value).append("(").append(_hitcount).append(")");
		return buf.toString();
	}


	@Override
	public boolean equals(Object obj) {
		boolean equals=false;
		
		if (obj instanceof BrowseFacet){
			BrowseFacet c2=(BrowseFacet)obj;
			if (_hitcount==c2._hitcount && _value.equals(c2._value)){
				equals=true;
			}
		}
		return equals;
	}

	public List<BrowseFacet> merge(List<BrowseFacet> v,Comparator<BrowseFacet> comparator) {
		int i=0;
		for (BrowseFacet facet : v)
		{
			int val = comparator.compare(this, facet);
			if (val == 0)
			{
				facet._hitcount +=this._hitcount;
				return v;
			}
			if (val>0)
			{
				
			}
			i++;
		}
		v.add(this);
		return v;
	}	
}
