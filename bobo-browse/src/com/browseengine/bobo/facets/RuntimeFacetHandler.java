/**
 * 
 */
package com.browseengine.bobo.facets;

import java.io.IOException;
import java.util.Set;

import com.browseengine.bobo.api.BoboIndexReader;

/**
 * @author ymatsuda
 *
 */
public abstract class RuntimeFacetHandler<D> extends FacetHandler<D>
{
  /**
   * Constructor
   * @param name name
   * @param dependsOn Set of names of facet handlers this facet handler depend on for loading
   */
  public RuntimeFacetHandler(String name, Set<String> dependsOn)
  {
    super(name, dependsOn);
  }
  
  /**
   * Constructor
   * @param name name
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
  }
  
  @Override
  public void loadFacetData(BoboIndexReader reader) throws IOException
  {
    reader.putRuntimeFacetData(_name, load(reader));
  }

}
