/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation
 * that handles various types of semi-structured data.  Written in Java.
 *
 * Copyright (C) 2005-2006  John Wang
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * To contact the project administrators for the bobo-browse project,
 * please go to https://sourceforge.net/projects/bobo-browse/, or
 * send mail to owner@browseengine.com.
 */

package com.browseengine.bobo.server.protocol;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.browseengine.bobo.api.BrowseFacet;
import com.browseengine.bobo.api.BrowseHit;
import com.browseengine.bobo.api.BrowseRequest;
import com.browseengine.bobo.api.BrowseResult;
import com.browseengine.bobo.api.FacetAccessible;

public class BrowseJSONSerializer {

  public static final short Selection_Type_Simple = 0;
  public static final short Selection_Type_Path = 1;
  public static final short Selection_Type_Range = 2;

  public static final short Operation_Type_Or = 0;
  public static final short Operation_Type_And = 1;

  public BrowseJSONSerializer() {
    super();
  }

  public static String serialize(BrowseRequest req) {
    return null;
  }

  /**
   * TODO: need to add support for multiple values.
   * @param doc
   * @return
   * @throws JSONException
   */
  public static JSONObject serializeValues(Map<String, String[]> values) throws JSONException {
    JSONObject obj = new JSONObject();
    Iterator<String> iter = values.keySet().iterator();
    while (iter.hasNext()) {
      String name = iter.next();
      String[] vals = values.get(name);
      if (vals.length > 0) {
        obj.put(name, vals[0]);
      }
    }
    return obj;
  }

  public static JSONObject serializeHits(BrowseHit hit) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("doc", serializeValues(hit.getFieldValues()));
    obj.put("docid", hit.getDocid());
    obj.put("score", hit.getScore());
    return obj;
  }

  public static String serialize(BrowseResult result) throws JSONException {
    JSONObject obj = new JSONObject();
    if (result != null) {
      obj.put("time", result.getTime() / 1000.0);
      obj.put("hitCount", result.getNumHits());
      obj.put("totalDocs", result.getTotalDocs());

      // serialize choices
      JSONObject choices = new JSONObject();
      Set<Entry<String, FacetAccessible>> facetAccessors = result.getFacetMap().entrySet();
      for (Entry<String, FacetAccessible> entry : facetAccessors) {
        JSONObject choiceObject = new JSONObject();
        JSONArray choiceValArray = new JSONArray();

        choiceObject.put("choicelist", choiceValArray);
        int k = 0;

        String name = entry.getKey();
        FacetAccessible facets = entry.getValue();

        List<BrowseFacet> facetList = facets.getFacets();
        for (BrowseFacet facet : facetList) {
          JSONObject choice = new JSONObject();
          choice.put("val", facet.getValue());
          choice.put("hits", facet.getFacetValueHitCount());
          choiceValArray.put(k++, choice);
        }
        choices.put(name, choiceObject);
      }
      obj.put("choices", choices);

      JSONArray hitsArray = new JSONArray();
      BrowseHit[] hits = result.getHits();
      if (hits != null && hits.length > 0) {
        for (int i = 0; i < hits.length; ++i) {
          hitsArray.put(i, serializeHits(hits[i]));
        }
      }
      obj.put("hits", hitsArray);
      // serialize documents
    }
    return obj.toString();
  }
}
