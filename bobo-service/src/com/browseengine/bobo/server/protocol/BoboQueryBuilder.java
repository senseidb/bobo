package com.browseengine.bobo.server.protocol;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

public abstract class BoboQueryBuilder {
	public abstract Query parseQuery(String query,String defaultField);
	public abstract Sort parseSort(String sortString);
}
