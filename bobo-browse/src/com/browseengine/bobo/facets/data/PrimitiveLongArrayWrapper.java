package com.browseengine.bobo.facets.data;

import java.util.Arrays;

public class PrimitiveLongArrayWrapper
{
  public long[] data;

  public PrimitiveLongArrayWrapper(long[] data)
  {
    this.data = data;
  }

  @Override
  public boolean equals(Object other)
  {
    if (other instanceof PrimitiveLongArrayWrapper)
    {
      return Arrays.equals(data, ((PrimitiveLongArrayWrapper)other).data);
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    return Arrays.hashCode(data);
  }
}

