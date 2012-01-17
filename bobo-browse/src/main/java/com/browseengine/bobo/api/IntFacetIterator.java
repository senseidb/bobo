package com.browseengine.bobo.api;

public abstract class IntFacetIterator extends FacetIterator
{
  public int facet;
  public abstract int nextInt();
  public abstract int nextInt(int minHits);
  public abstract String format(int val);
}
