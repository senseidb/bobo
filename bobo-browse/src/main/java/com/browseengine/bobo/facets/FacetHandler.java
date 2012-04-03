package com.browseengine.bobo.facets;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.browseengine.bobo.api.BoboIndexReader;
import com.browseengine.bobo.api.BrowseSelection;
import com.browseengine.bobo.api.BrowseSelection.ValueOperation;
import com.browseengine.bobo.api.FacetAccessible;
import com.browseengine.bobo.api.FacetSpec;
import com.browseengine.bobo.facets.filter.EmptyFilter;
import com.browseengine.bobo.facets.filter.RandomAccessAndFilter;
import com.browseengine.bobo.facets.filter.RandomAccessFilter;
import com.browseengine.bobo.facets.filter.RandomAccessNotFilter;
import com.browseengine.bobo.facets.filter.RandomAccessOrFilter;
import com.browseengine.bobo.sort.DocComparatorSource;

/**
 * FacetHandler definition
 *
 */
public abstract class FacetHandler<D>
{
	public static class FacetDataNone implements Serializable{
		private static final long serialVersionUID = 1L;
		public static FacetDataNone instance = new FacetDataNone();
		private FacetDataNone(){}
	}
	
	protected final String _name;
	private final Set<String> _dependsOn;
	private final Map<String,FacetHandler<?>> _dependedFacetHandlers;
	private TermCountSize _termCountSize;
	
	public static enum TermCountSize{
		small,
		medium,
		large
	}
	
	/**
	 * Constructor
	 * @param name name
	 * @param dependsOn Set of names of facet handlers this facet handler depend on for loading
	 */
	public FacetHandler(String name,Set<String> dependsOn)
	{
		_name=name;
		_dependsOn = new HashSet<String>();
		if (dependsOn != null)
		{
			_dependsOn.addAll(dependsOn);
		}
		_dependedFacetHandlers = new HashMap<String,FacetHandler<?>>();
		_termCountSize = TermCountSize.large;
	}
	
	public FacetHandler<D> setTermCountSize(String termCountSize){
		setTermCountSize(TermCountSize.valueOf(termCountSize.toLowerCase()));
    return this;
	}
	
	public FacetHandler<D> setTermCountSize(TermCountSize termCountSize){
		_termCountSize = termCountSize;
    return this;
	}
	
	public TermCountSize getTermCountSize(){
		return _termCountSize;
	}
	
	/**
	 * Constructor
	 * @param name name
	 */
	public FacetHandler(String name)
	{
		this(name,null);
	}
	
	/**
	 * Gets the name
	 * @return name
	 */
	public final String getName()
	{
		return _name;
	}
	
	/**
	 * Gets names of the facet handler this depends on
	 * @return set of facet handler names
	 */
	public final Set<String> getDependsOn()
	{
		return _dependsOn;
	}
	
	/**
	 * Adds a list of depended facet handlers
	 * @param facetHandler depended facet handler
	 */
	public final void putDependedFacetHandler(FacetHandler<?> facetHandler)
	{
		_dependedFacetHandlers.put(facetHandler._name, facetHandler);
	}
	
	/**
	 * Gets a depended facet handler
	 * @param name facet handler name
	 * @return facet handler instance 
	 */
	public final FacetHandler<?> getDependedFacetHandler(String name)
	{
		return _dependedFacetHandlers.get(name);
	}
	
	/**
	 * Load information from an index reader, initialized by {@link BoboIndexReader}
	 * @param reader reader
	 * @throws IOException
	 */
	abstract public D load(BoboIndexReader reader) throws IOException;

	public FacetAccessible merge(FacetSpec fspec, List<FacetAccessible> facetList)
	{
		return new CombinedFacetAccessible(fspec,facetList);
	}
	
	@SuppressWarnings("unchecked")
	public D getFacetData(BoboIndexReader reader){
		return (D)reader.getFacetData(_name);
	}
	
	public D load(BoboIndexReader reader, BoboIndexReader.WorkArea workArea) throws IOException
	{
	  return load(reader);
	}
	
	public void loadFacetData(BoboIndexReader reader, BoboIndexReader.WorkArea workArea) throws IOException
	{
	  reader.putFacetData(_name, load(reader, workArea));
	}
	
	public void loadFacetData(BoboIndexReader reader) throws IOException
	{
	  reader.putFacetData(_name, load(reader));
	}

	/**
	 * Gets a filter from a given selection
	 * @param sel selection
	 * @return a filter
	 * @throws IOException 
	 * @throws IOException
	 */
	public RandomAccessFilter buildFilter(BrowseSelection sel) throws IOException
	{
      String[] selections = sel.getValues();
      String[] notSelections = sel.getNotValues();
      Properties prop=sel.getSelectionProperties();
      
      RandomAccessFilter filter = null;
      if (selections!=null && selections.length > 0)
      {
        if (sel.getSelectionOperation() == ValueOperation.ValueOperationAnd)
        {
          filter = buildRandomAccessAndFilter(selections,prop);
          if (filter == null)
          {
            filter = EmptyFilter.getInstance();
          }
        }
        else
        {
          filter = buildRandomAccessOrFilter(selections, prop,false);
          if (filter == null)
          {
            return EmptyFilter.getInstance();
          }
        }
      }
      
      if (notSelections!=null && notSelections.length>0)
      {
        RandomAccessFilter notFilter = buildRandomAccessOrFilter(notSelections, prop, true);
        if (filter==null)
        {
          filter = notFilter;
        }
        else
        {
          RandomAccessFilter andFilter = new RandomAccessAndFilter(Arrays.asList(new RandomAccessFilter[]{filter,notFilter}));
          filter = andFilter;
        }
      }
      
      return filter;
	}
	
	abstract public RandomAccessFilter buildRandomAccessFilter(String value,Properties selectionProperty) throws IOException;
	
  public RandomAccessFilter buildRandomAccessAndFilter(String[] vals, Properties prop) throws IOException {
    ArrayList<RandomAccessFilter> filterList = new ArrayList<RandomAccessFilter>(vals.length);

    for (String val : vals) {
      RandomAccessFilter f = buildRandomAccessFilter(val, prop);
      if (f != null) {
        filterList.add(f);
      } else {
        return EmptyFilter.getInstance();
      }
    }

    if (filterList.size() == 1)
      return filterList.get(0);
    return new RandomAccessAndFilter(filterList);
  }
	
	public RandomAccessFilter buildRandomAccessOrFilter(String[] vals,Properties prop,boolean isNot) throws IOException
    {
      ArrayList<RandomAccessFilter> filterList = new ArrayList<RandomAccessFilter>(vals.length);
      
      for (String val : vals)
      {
        RandomAccessFilter f = buildRandomAccessFilter(val, prop);
        if(f != null && !(f instanceof EmptyFilter)) 
        {
          filterList.add(f);
        }
      }
      
      RandomAccessFilter finalFilter;
      if (filterList.size() == 0)
      {
        finalFilter = EmptyFilter.getInstance();
      }
      else
      {
        finalFilter = new RandomAccessOrFilter(filterList);
      }
      
      if (isNot)
      {
        finalFilter = new RandomAccessNotFilter(finalFilter);
      }
      return finalFilter;
    }
	
	/**
	 * Gets a FacetCountCollector
	 * @param sel selection
	 * @param fspec facetSpec
	 * @return a FacetCountCollector
	 */
	abstract public FacetCountCollectorSource getFacetCountCollectorSource(BrowseSelection sel, FacetSpec fspec);

  /**
   * Override this method if your facet handler have a better group mode like the SimpleFacetHandler.
   */
	public FacetCountCollectorSource getFacetCountCollectorSource(BrowseSelection sel,
                                                                FacetSpec ospec,
                                                                boolean groupMode) {
    return getFacetCountCollectorSource(sel, ospec);
  }
	
	/**
	 * Gets the field value
	 * @param id doc
	 * @param reader index reader
	 * @return array of field values
	 * @see #getFieldValue(BoboIndexReader,int)
	 */
	abstract public String[] getFieldValues(BoboIndexReader reader,int id);
	
	public int getNumItems(BoboIndexReader reader,int id){
	  throw new UnsupportedOperationException("getNumItems is not supported for this facet handler: "+getClass().getName());
	}
	
	public Object[] getRawFieldValues(BoboIndexReader reader,int id){
		return getFieldValues(reader, id);
	}
	
	/**
	 * Gets a single field value
	 * @param id doc
	 * @param reader index reader
	 * @return first field value
	 * @see #getFieldValues(BoboIndexReader,int)
	 */
	public String getFieldValue(BoboIndexReader reader,int id)
	{
		return getFieldValues(reader,id)[0];
	}
	
	/**
	 * builds a comparator to determine how sorting is done
	 * @return a sort comparator
	 */
	abstract public DocComparatorSource getDocComparatorSource();
	
	@Override
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}
}
