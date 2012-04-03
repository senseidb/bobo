/**
 * 
 */
package com.browseengine.bobo.facets;

import java.io.IOException;
import java.util.Set;

import com.browseengine.bobo.api.BoboIndexReader;

/**
 * Abstract class for RuntimeFacetHandlers. A concrete RuntimeFacetHandler should implement
 * the FacetHandlerFactory and RuntimeInitializable so that bobo knows how to create new
 * instance of the handler at run time and how to initialize it at run time respectively.
 * @author ymatsuda
 * @param <D> type parameter for FacetData
 */
public abstract class RuntimeFacetHandler<D> extends FacetHandler<D>
{
  /**
   * Constructor that specifying the dependent facet handlers using names.
   * @param name the name of this FacetHandler, which is used in FacetSpec and Selection to specify
   * the facet. If we regard a facet as a field, the name is like a field name.
   * @param dependsOn Set of names of facet handlers this facet handler depend on for loading.
   */
  public RuntimeFacetHandler(String name, Set<String> dependsOn)
  {
    super(name, dependsOn);
  }
  
  /**
   * Constructor
   * @param name the name of this FacetHandler, which is used in FacetSpec and Selection to specify
   * the facet. If we regard a facet as a field, the name is like a field name.
   */
  public RuntimeFacetHandler(String name)
  {
      super(name);
  }
  
  
  @Override
  @SuppressWarnings("unchecked")
  public D getFacetData(BoboIndexReader reader){
      return (D)reader.getRuntimeFacetData(_name);
  }

  @Override
  public void loadFacetData(BoboIndexReader reader, BoboIndexReader.WorkArea workArea) throws IOException
  {
    reader.putRuntimeFacetData(_name, load(reader, workArea));
    reader.putRuntimeFacetHandler(_name, this);
  }
  
  @Override
  public void loadFacetData(BoboIndexReader reader) throws IOException
  {
    reader.putRuntimeFacetData(_name, load(reader));
    reader.putRuntimeFacetHandler(_name, this);
  }

  public void close()
  {
  }
}
