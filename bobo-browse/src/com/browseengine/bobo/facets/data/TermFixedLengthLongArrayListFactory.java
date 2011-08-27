package com.browseengine.bobo.facets.data;

public class TermFixedLengthLongArrayListFactory implements TermListFactory<long[]>
{
  protected int width;

  public TermFixedLengthLongArrayListFactory(int width)
  {
    this.width = width;
  }

  public TermValueList<long[]> createTermList(int capacity)
  {
    return new TermFixedLengthLongArrayList(width, capacity);
  }

  public TermValueList<long[]> createTermList()
  {
    return createTermList(-1);
  }

  public Class<?> getType()
  {
    return long[].class;
  }
}
