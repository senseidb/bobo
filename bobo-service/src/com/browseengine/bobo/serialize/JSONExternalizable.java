package com.browseengine.bobo.serialize;

import org.json.JSONException;
import org.json.JSONObject;

public interface JSONExternalizable extends JSONSerializable {
	JSONObject toJSON() throws JSONSerializationException,JSONException;
	void fromJSON(JSONObject obj) throws JSONSerializationException,JSONException;
}
