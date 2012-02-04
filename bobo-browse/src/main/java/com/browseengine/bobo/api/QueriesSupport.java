package com.browseengine.bobo.api;

import java.util.HashSet;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

public class QueriesSupport {
  public static Query combineAnd(Query... queries) {
    HashSet<Query> uniques = new HashSet<Query>();
    for (int i = 0; i < queries.length; i++) {
      Query query = queries[i];
      BooleanClause[] clauses = null;
      // check if we can split the query into clauses
      boolean splittable = (query instanceof BooleanQuery);
      if(splittable){
        BooleanQuery bq = (BooleanQuery) query;
        splittable = bq.isCoordDisabled();
        clauses = bq.getClauses();
        for (int j = 0; splittable && j < clauses.length; j++) {
          splittable = (clauses[j].getOccur() == BooleanClause.Occur.MUST);
        }
      }
      if(splittable){
        for (int j = 0; j < clauses.length; j++) {
          uniques.add(clauses[j].getQuery());
        }
      } else {
        uniques.add(query);
      }
    }
    // optimization: if we have just one query, just return it
    if(uniques.size() == 1){
        return uniques.iterator().next();
    }
    BooleanQuery result = new BooleanQuery(true);
    for (final Query query : uniques)
      result.add(query, BooleanClause.Occur.MUST);
    return result;
  }
}
